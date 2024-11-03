package com.tencent.supersonic.headless.server.facade.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import com.tencent.supersonic.headless.api.pojo.QueryParam;
import com.tencent.supersonic.headless.api.pojo.enums.SemanticType;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticTranslateResp;
import com.tencent.supersonic.headless.chat.knowledge.HanlpMapResult;
import com.tencent.supersonic.headless.chat.knowledge.KnowledgeBaseService;
import com.tencent.supersonic.headless.chat.knowledge.MapResult;
import com.tencent.supersonic.headless.chat.knowledge.SearchService;
import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;
import com.tencent.supersonic.headless.chat.knowledge.helper.NatureHelper;
import com.tencent.supersonic.headless.core.cache.QueryCache;
import com.tencent.supersonic.headless.core.executor.QueryExecutor;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.translator.SemanticTranslator;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.SemanticModel;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import com.tencent.supersonic.headless.server.annotation.S2DataPermission;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.manager.SemanticSchemaManager;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.SchemaService;
import com.tencent.supersonic.headless.server.utils.MetricDrillDownChecker;
import com.tencent.supersonic.headless.server.utils.QueryReqConverter;
import com.tencent.supersonic.headless.server.utils.QueryUtils;
import com.tencent.supersonic.headless.server.utils.StatUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class S2SemanticLayerService implements SemanticLayerService {

    private final StatUtils statUtils;
    private final QueryUtils queryUtils;
    private final QueryReqConverter queryReqConverter;
    private final SemanticSchemaManager semanticSchemaManager;
    private final DataSetService dataSetService;
    private final SchemaService schemaService;
    private final SemanticTranslator semanticTranslator;
    private final MetricDrillDownChecker metricDrillDownChecker;
    private final KnowledgeBaseService knowledgeBaseService;
    private final MetricService metricService;
    private final DimensionService dimensionService;
    private final QueryCache queryCache = ComponentFactory.getQueryCache();
    private final List<QueryExecutor> queryExecutors = ComponentFactory.getQueryExecutors();

    public S2SemanticLayerService(StatUtils statUtils, QueryUtils queryUtils,
            QueryReqConverter queryReqConverter, SemanticSchemaManager semanticSchemaManager,
            DataSetService dataSetService, SchemaService schemaService,
            SemanticTranslator semanticTranslator, MetricDrillDownChecker metricDrillDownChecker,
            KnowledgeBaseService knowledgeBaseService, MetricService metricService,
            DimensionService dimensionService) {
        this.statUtils = statUtils;
        this.queryUtils = queryUtils;
        this.queryReqConverter = queryReqConverter;
        this.semanticSchemaManager = semanticSchemaManager;
        this.dataSetService = dataSetService;
        this.schemaService = schemaService;
        this.semanticTranslator = semanticTranslator;
        this.metricDrillDownChecker = metricDrillDownChecker;
        this.knowledgeBaseService = knowledgeBaseService;
        this.metricService = metricService;
        this.dimensionService = dimensionService;
    }

    public DataSetSchema getDataSetSchema(Long id) {
        return schemaService.getDataSetSchema(id);
    }

    @S2DataPermission
    @Override
    public SemanticTranslateResp translate(SemanticQueryReq queryReq, User user) throws Exception {
        QueryStatement queryStatement = buildQueryStatement(queryReq, user);
        semanticTranslator.translate(queryStatement);
        return SemanticTranslateResp.builder().querySQL(queryStatement.getSql())
                .isOk(queryStatement.isOk()).errMsg(queryStatement.getErrMsg()).build();
    }

    @Override
    @S2DataPermission
    @SneakyThrows
    public SemanticQueryResp queryByReq(SemanticQueryReq queryReq, User user) {
        TaskStatusEnum state = TaskStatusEnum.SUCCESS;
        log.info("[queryReq:{}]", queryReq);
        try {
            // 1.initStatInfo
            statUtils.initStatInfo(queryReq, user);

            // 2.query from cache

            String cacheKey = queryCache.getCacheKey(queryReq);
            Object query = queryCache.query(queryReq, cacheKey);
            if (Objects.nonNull(query)) {
                log.info("cacheKey:{},query:{}", cacheKey,
                        StringUtils.normalizeSpace(query.toString()));
            }
            if (Objects.nonNull(query)) {
                SemanticQueryResp queryResp = (SemanticQueryResp) query;
                queryResp.setUseCache(true);
                return queryResp;
            }
            StatUtils.get().setUseResultCache(false);

            // 3 query
            QueryStatement queryStatement = buildQueryStatement(queryReq, user);
            SemanticQueryResp queryResp = null;

            // skip translation if already done.
            if (!queryStatement.isTranslated()) {
                semanticTranslator.translate(queryStatement);
            }
            queryPreCheck(queryStatement);

            for (QueryExecutor queryExecutor : queryExecutors) {
                if (queryExecutor.accept(queryStatement)) {
                    queryResp = queryExecutor.execute(queryStatement);
                    queryUtils.populateQueryColumns(queryResp,
                            queryStatement.getSemanticSchemaResp());
                }
            }

            // 4 reset cache and set stateInfo
            Boolean setCacheSuccess = queryCache.put(cacheKey, queryResp);
            if (setCacheSuccess) {
                // if result is not null, update cache data
                statUtils.updateResultCacheKey(cacheKey);
            }
            if (Objects.isNull(queryResp)) {
                state = TaskStatusEnum.ERROR;
            } else {
                queryResp.appendErrorMsg(queryStatement.getErrMsg());
            }

            return queryResp;
        } catch (Exception e) {
            log.error("exception in queryByReq:{}, e: ", queryReq, e);
            state = TaskStatusEnum.ERROR;
            throw e;
        } finally {
            statUtils.statInfo2DbAsync(state);
        }
    }

    @Override
    public SemanticQueryResp queryDimensionValue(DimensionValueReq dimensionValueReq, User user) {
        SemanticQueryResp semanticQueryResp = new SemanticQueryResp();
        DimensionResp dimensionResp = getDimension(dimensionValueReq);
        Set<Long> dataSetIds = dimensionValueReq.getDataSetIds();
        dimensionValueReq.setModelId(dimensionResp.getModelId());

        List<String> dimensionValues = getDimensionValuesFromDict(dimensionValueReq, dataSetIds);

        // If the search results are null, search dimensionValue from the database
        if (CollectionUtils.isEmpty(dimensionValues)) {
            return getDimensionValuesFromDb(dimensionValueReq, user);
        }

        List<QueryColumn> columns = createQueryColumns(dimensionValueReq);
        List<Map<String, Object>> resultList = createResultList(dimensionValueReq, dimensionValues);

        semanticQueryResp.setColumns(columns);
        semanticQueryResp.setResultList(resultList);
        return semanticQueryResp;
    }

    private List<String> getDimensionValuesFromDict(DimensionValueReq dimensionValueReq,
            Set<Long> dataSetIds) {
        if (StringUtils.isBlank(dimensionValueReq.getValue())) {
            return SearchService.getDimensionValue(dimensionValueReq);
        }

        Map<Long, List<Long>> modelIdToDataSetIds = new HashMap<>();
        modelIdToDataSetIds.put(dimensionValueReq.getModelId(), new ArrayList<>(dataSetIds));

        List<HanlpMapResult> hanlpMapResultList = knowledgeBaseService
                .prefixSearch(dimensionValueReq.getValue(), 2000, modelIdToDataSetIds, dataSetIds);

        HanlpHelper.transLetterOriginal(hanlpMapResultList);

        return hanlpMapResultList.stream()
                .filter(o -> o.getNatures().stream().map(NatureHelper::getElementID)
                        .anyMatch(elementID -> dimensionValueReq.getElementID().equals(elementID)))
                .map(MapResult::getName).collect(Collectors.toList());
    }

    private SemanticQueryResp getDimensionValuesFromDb(DimensionValueReq dimensionValueReq,
            User user) {
        QuerySqlReq querySqlReq = buildQuerySqlReq(dimensionValueReq);
        return queryByReq(querySqlReq, user);
    }

    private List<QueryColumn> createQueryColumns(DimensionValueReq dimensionValueReq) {
        QueryColumn queryColumn = new QueryColumn();
        queryColumn.setNameEn(dimensionValueReq.getBizName());
        queryColumn.setShowType(SemanticType.CATEGORY.name());
        queryColumn.setAuthorized(true);
        queryColumn.setType("CHAR");

        List<QueryColumn> columns = new ArrayList<>();
        columns.add(queryColumn);
        return columns;
    }

    private List<Map<String, Object>> createResultList(DimensionValueReq dimensionValueReq,
            List<String> dimensionValues) {
        return dimensionValues.stream().map(value -> {
            Map<String, Object> map = new HashMap<>();
            map.put(dimensionValueReq.getBizName(), value);
            return map;
        }).collect(Collectors.toList());
    }

    private DimensionResp getDimension(DimensionValueReq dimensionValueReq) {
        Long elementID = dimensionValueReq.getElementID();
        DimensionResp dimensionResp = schemaService.getDimension(elementID);
        if (dimensionResp == null) {
            String bizName = dimensionValueReq.getBizName();
            Long modelId = dimensionValueReq.getModelId();
            return schemaService.getDimension(bizName, modelId);
        }
        return dimensionResp;
    }

    @Override
    public List<ItemResp> getDomainDataSetTree() {
        return schemaService.getDomainDataSetTree();
    }

    @Override
    public List<DimensionResp> getDimensions(MetaFilter metaFilter) {
        return dimensionService.getDimensions(metaFilter);
    }

    @Override
    public List<MetricResp> getMetrics(MetaFilter metaFilter) {
        return metricService.getMetrics(metaFilter);
    }

    private QueryStatement buildSqlQueryStatement(QuerySqlReq querySqlReq, User user)
            throws Exception {
        // If dataSetId or DataSetName is empty, parse dataSetId from the SQL
        if (querySqlReq.needGetDataSetId()) {
            Long dataSetId = dataSetService.getDataSetIdFromSql(querySqlReq.getSql(), user);
            querySqlReq.setDataSetId(dataSetId);
        }
        SchemaFilterReq filter = buildSchemaFilterReq(querySqlReq);
        SemanticSchemaResp semanticSchemaResp = schemaService.fetchSemanticSchema(filter);
        QueryStatement queryStatement = queryReqConverter.convert(querySqlReq, semanticSchemaResp);
        queryStatement.setModelIds(querySqlReq.getModelIds());
        queryStatement.setEnableOptimize(queryUtils.enableOptimize());
        queryStatement.setSemanticSchemaResp(semanticSchemaResp);
        queryStatement.setSemanticModel(semanticSchemaManager.getSemanticModel(semanticSchemaResp));
        return queryStatement;
    }

    private QueryStatement buildQueryStatement(SemanticQueryReq semanticQueryReq, User user)
            throws Exception {
        QueryStatement queryStatement = null;
        if (semanticQueryReq instanceof QuerySqlReq) {
            queryStatement = buildSqlQueryStatement((QuerySqlReq) semanticQueryReq, user);
        }
        if (semanticQueryReq instanceof QueryStructReq) {
            queryStatement = buildStructQueryStatement((QueryStructReq) semanticQueryReq);
        }
        if (semanticQueryReq instanceof QueryMultiStructReq) {
            queryStatement =
                    buildMultiStructQueryStatement((QueryMultiStructReq) semanticQueryReq, user);
        }
        if (Objects.nonNull(queryStatement) && Objects.nonNull(semanticQueryReq.getSqlInfo())
                && StringUtils.isNotBlank(semanticQueryReq.getSqlInfo().getQuerySQL())) {
            queryStatement.setSql(semanticQueryReq.getSqlInfo().getQuerySQL());
            queryStatement.setDataSetId(semanticQueryReq.getDataSetId());
            queryStatement.setIsTranslated(true);
        }
        return queryStatement;
    }

    private QueryStatement buildStructQueryStatement(QueryStructReq queryStructReq) {
        SchemaFilterReq filter = buildSchemaFilterReq(queryStructReq);
        SemanticSchemaResp semanticSchemaResp = schemaService.fetchSemanticSchema(filter);
        QueryStatement queryStatement = new QueryStatement();
        QueryParam queryParam = new QueryParam();
        queryReqConverter.convert(queryStructReq, queryParam);
        queryStatement.setQueryParam(queryParam);
        queryStatement.setIsS2SQL(false);
        queryStatement.setEnableOptimize(queryUtils.enableOptimize());
        queryStatement.setDataSetId(queryStructReq.getDataSetId());
        queryStatement.setSemanticSchemaResp(semanticSchemaResp);
        queryStatement.setSemanticModel(semanticSchemaManager.getSemanticModel(semanticSchemaResp));
        return queryStatement;
    }

    private QueryStatement buildMultiStructQueryStatement(QueryMultiStructReq queryMultiStructReq,
            User user) throws Exception {
        List<QueryStatement> sqlParsers = new ArrayList<>();
        for (QueryStructReq queryStructReq : queryMultiStructReq.getQueryStructReqs()) {
            QueryStatement queryStatement = buildQueryStatement(queryStructReq, user);
            SemanticModel semanticModel = queryStatement.getSemanticModel();
            queryStatement.setModelIds(queryStructReq.getModelIds());
            queryStatement.setSemanticModel(semanticModel);
            queryStatement.setEnableOptimize(queryUtils.enableOptimize());
            semanticTranslator.translate(queryStatement);
            sqlParsers.add(queryStatement);
        }
        log.info("multi sqlParser:{}", sqlParsers);
        return queryUtils.sqlParserUnion(queryMultiStructReq, sqlParsers);
    }

    private SchemaFilterReq buildSchemaFilterReq(SemanticQueryReq semanticQueryReq) {
        SchemaFilterReq schemaFilterReq = new SchemaFilterReq();
        schemaFilterReq.setDataSetId(semanticQueryReq.getDataSetId());
        schemaFilterReq.setModelIds(semanticQueryReq.getModelIds());
        return schemaFilterReq;
    }

    private QuerySqlReq buildQuerySqlReq(DimensionValueReq queryDimValueReq) {
        QuerySqlReq querySqlReq = new QuerySqlReq();
        List<ModelResp> modelResps =
                schemaService.getModelList(Lists.newArrayList(queryDimValueReq.getModelId()));
        DimensionResp dimensionResp = schemaService.getDimension(queryDimValueReq.getBizName(),
                queryDimValueReq.getModelId());
        ModelResp modelResp = modelResps.get(0);
        String sql = String.format("select distinct %s from %s where 1=1", dimensionResp.getName(),
                modelResp.getName());
        List<Dim> timeDims = modelResp.getTimeDimension();
        if (CollectionUtils.isNotEmpty(timeDims)) {
            sql = String.format("%s and %s >= '%s' and %s <= '%s'", sql,
                    TimeDimensionEnum.DAY.getName(), queryDimValueReq.getDateInfo().getStartDate(),
                    TimeDimensionEnum.DAY.getName(), queryDimValueReq.getDateInfo().getEndDate());
        }
        if (StringUtils.isNotBlank(queryDimValueReq.getValue())) {
            sql += " AND " + queryDimValueReq.getBizName() + " LIKE '%"
                    + queryDimValueReq.getValue() + "%'";
        }
        querySqlReq.setModelIds(Sets.newHashSet(queryDimValueReq.getModelId()));
        querySqlReq.setSql(sql);
        return querySqlReq;
    }

    private void queryPreCheck(QueryStatement queryStatement) {
        // Check whether the dimensions of the metric drill-down are correct temporarily,
        // add the abstraction of a validator later.
        metricDrillDownChecker.checkQuery(queryStatement);
    }

}
