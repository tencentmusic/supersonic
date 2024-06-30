package com.tencent.supersonic.headless.server.facade.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.api.pojo.DataInfo;
import com.tencent.supersonic.headless.api.pojo.DataSetInfo;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.EntityInfo;
import com.tencent.supersonic.headless.api.pojo.QueryParam;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.TagTypeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.request.ExplainSqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryDimValueReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.ExplainResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.chat.utils.QueryReqBuilder;
import com.tencent.supersonic.headless.core.cache.QueryCache;
import com.tencent.supersonic.headless.core.executor.QueryExecutor;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.translator.SemanticTranslator;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.SemanticModel;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import com.tencent.supersonic.headless.server.annotation.S2DataPermission;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.manager.SemanticSchemaManager;
import com.tencent.supersonic.headless.server.utils.QueryReqConverter;
import com.tencent.supersonic.headless.server.utils.QueryUtils;
import com.tencent.supersonic.headless.server.utils.StatUtils;
import com.tencent.supersonic.headless.server.web.service.DataSetService;
import com.tencent.supersonic.headless.server.web.service.SchemaService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Slf4j
public class S2SemanticLayerService implements SemanticLayerService {

    private StatUtils statUtils;
    private final QueryUtils queryUtils;
    private final QueryReqConverter queryReqConverter;
    private final SemanticSchemaManager semanticSchemaManager;
    private final DataSetService dataSetService;
    private final SchemaService schemaService;
    private final SemanticTranslator semanticTranslator;

    public S2SemanticLayerService(
            StatUtils statUtils,
            QueryUtils queryUtils,
            QueryReqConverter queryReqConverter,
            SemanticSchemaManager semanticSchemaManager,
            DataSetService dataSetService,
            SchemaService schemaService,
            SemanticTranslator semanticTranslator) {
        this.statUtils = statUtils;
        this.queryUtils = queryUtils;
        this.queryReqConverter = queryReqConverter;
        this.semanticSchemaManager = semanticSchemaManager;
        this.dataSetService = dataSetService;
        this.schemaService = schemaService;
        this.semanticTranslator = semanticTranslator;
    }

    public DataSetSchema getDataSetSchema(Long id) {
        return schemaService.getDataSetSchema(id);
    }

    @Override
    @S2DataPermission
    @SneakyThrows
    public SemanticQueryResp queryByReq(SemanticQueryReq queryReq, User user) {
        TaskStatusEnum state = TaskStatusEnum.SUCCESS;
        log.info("[queryReq:{}]", queryReq);
        try {
            //1.initStatInfo
            statUtils.initStatInfo(queryReq, user);
            //2.query from cache
            QueryCache queryCache = ComponentFactory.getQueryCache();
            String cacheKey = queryCache.getCacheKey(queryReq);
            log.debug("cacheKey:{}", cacheKey);
            Object query = queryCache.query(queryReq, cacheKey);
            if (Objects.nonNull(query)) {
                SemanticQueryResp queryResp = (SemanticQueryResp) query;
                queryResp.setUseCache(true);
                return queryResp;
            }
            StatUtils.get().setUseResultCache(false);
            //3 query
            QueryStatement queryStatement = buildQueryStatement(queryReq, user);
            SemanticQueryResp result = query(queryStatement);
            //4 reset cache and set stateInfo
            Boolean setCacheSuccess = queryCache.put(cacheKey, result);
            if (setCacheSuccess) {
                // if result is not null, update cache data
                statUtils.updateResultCacheKey(cacheKey);
            }
            if (Objects.isNull(result)) {
                state = TaskStatusEnum.ERROR;
            }
            return result;
        } catch (Exception e) {
            log.error("exception in queryByStruct, e: ", e);
            state = TaskStatusEnum.ERROR;
            throw e;
        } finally {
            statUtils.statInfo2DbAsync(state);
        }
    }

    private QueryStatement buildSqlQueryStatement(QuerySqlReq querySqlReq, User user) throws Exception {
        //If dataSetId or DataSetName is empty, parse dataSetId from the SQL
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

    private QueryStatement buildQueryStatement(SemanticQueryReq semanticQueryReq, User user) throws Exception {
        QueryStatement queryStatement = null;
        if (semanticQueryReq instanceof QuerySqlReq) {
            queryStatement = buildSqlQueryStatement((QuerySqlReq) semanticQueryReq, user);
        }
        if (semanticQueryReq instanceof QueryStructReq) {
            queryStatement = buildStructQueryStatement((QueryStructReq) semanticQueryReq);
        }
        if (semanticQueryReq instanceof QueryMultiStructReq) {
            queryStatement = buildMultiStructQueryStatement((QueryMultiStructReq) semanticQueryReq, user);
        }
        if (Objects.nonNull(queryStatement) && Objects.nonNull(semanticQueryReq.getSqlInfo()) && StringUtils.isNotBlank(
                semanticQueryReq.getSqlInfo().getQuerySQL())) {
            queryStatement.setSql(semanticQueryReq.getSqlInfo().getQuerySQL());
            queryStatement.setSourceId(semanticQueryReq.getSqlInfo().getSourceId());
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

    private QueryStatement buildMultiStructQueryStatement(QueryMultiStructReq queryMultiStructReq, User user)
            throws Exception {
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

    @Override
    @SneakyThrows
    public SemanticQueryResp queryDimValue(QueryDimValueReq queryDimValueReq, User user) {
        QuerySqlReq querySqlReq = buildQuerySqlReq(queryDimValueReq);
        return queryByReq(querySqlReq, user);
    }

    @Override
    public <T> ExplainResp explain(ExplainSqlReq<T> explainSqlReq, User user) throws Exception {
        T queryReq = explainSqlReq.getQueryReq();
        QueryStatement queryStatement = buildQueryStatement((SemanticQueryReq) queryReq, user);
        semanticTranslator.translate(queryStatement);

        String sql = "";
        String sorceId = "";
        if (Objects.nonNull(queryStatement)) {
            sql = queryStatement.getSql();
            sorceId = queryStatement.getSourceId();
        }
        return ExplainResp.builder().sql(sql).sourceId(sorceId).build();
    }

    public List<ItemResp> getDomainDataSetTree() {
        return schemaService.getDomainDataSetTree();
    }

    private QuerySqlReq buildQuerySqlReq(QueryDimValueReq queryDimValueReq) {
        QuerySqlReq querySqlReq = new QuerySqlReq();
        List<ModelResp> modelResps = schemaService.getModelList(Lists.newArrayList(queryDimValueReq.getModelId()));
        DimensionResp dimensionResp = schemaService.getDimension(queryDimValueReq.getDimensionBizName(),
                queryDimValueReq.getModelId());
        ModelResp modelResp = modelResps.get(0);
        String sql = String.format("select distinct %s from %s where 1=1",
                dimensionResp.getName(), modelResp.getName());
        List<Dim> timeDims = modelResp.getTimeDimension();
        if (CollectionUtils.isNotEmpty(timeDims)) {
            sql = String.format("%s and %s >= '%s' and %s <= '%s'", sql, TimeDimensionEnum.DAY.getName(),
                    queryDimValueReq.getDateInfo().getStartDate(), TimeDimensionEnum.DAY.getName(),
                    queryDimValueReq.getDateInfo().getEndDate());
        }
        if (StringUtils.isNotBlank(queryDimValueReq.getValue())) {
            sql += " AND " + queryDimValueReq.getDimensionBizName() + " LIKE '%" + queryDimValueReq.getValue() + "%'";
        }
        querySqlReq.setModelIds(Sets.newHashSet(queryDimValueReq.getModelId()));
        querySqlReq.setSql(sql);
        return querySqlReq;
    }

    private SemanticQueryResp query(QueryStatement queryStatement) throws Exception {
        SemanticQueryResp semanticQueryResp = null;
        try {
            //1 translate
            if (!queryStatement.isTranslated()) {
                semanticTranslator.translate(queryStatement);
            }

            //2 execute
            for (QueryExecutor queryExecutor : ComponentFactory.getQueryExecutors()) {
                if (queryExecutor.accept(queryStatement)) {
                    semanticQueryResp = queryExecutor.execute(queryStatement);
                    queryUtils.fillItemNameInfo(semanticQueryResp, queryStatement.getSemanticSchemaResp());
                }
            }

            return semanticQueryResp;
        } catch (Exception e) {
            log.error("exception in query, e: ", e);
            throw e;
        }
    }

    public EntityInfo getEntityInfo(SemanticParseInfo parseInfo, DataSetSchema dataSetSchema, User user) {
        if (parseInfo != null && parseInfo.getDataSetId() != null && parseInfo.getDataSetId() > 0) {
            EntityInfo entityInfo = getEntityBasicInfo(dataSetSchema);
            if (parseInfo.getDimensionFilters().size() <= 0 || entityInfo.getDataSetInfo() == null) {
                entityInfo.setMetrics(null);
                entityInfo.setDimensions(null);
                return entityInfo;
            }
            String primaryKey = entityInfo.getDataSetInfo().getPrimaryKey();
            if (StringUtils.isNotBlank(primaryKey)) {
                String entityId = "";
                for (QueryFilter chatFilter : parseInfo.getDimensionFilters()) {
                    if (chatFilter != null && chatFilter.getBizName() != null && chatFilter.getBizName()
                            .equals(primaryKey)) {
                        if (chatFilter.getOperator().equals(FilterOperatorEnum.EQUALS)) {
                            entityId = chatFilter.getValue().toString();
                        }
                    }
                }
                entityInfo.setEntityId(entityId);
                try {
                    fillEntityInfoValue(entityInfo, dataSetSchema, user);
                    return entityInfo;
                } catch (Exception e) {
                    log.error("setMainModel error", e);
                }
            }
        }
        return null;
    }

    private EntityInfo getEntityBasicInfo(DataSetSchema dataSetSchema) {

        EntityInfo entityInfo = new EntityInfo();
        if (dataSetSchema == null) {
            return entityInfo;
        }
        Long dataSetId = dataSetSchema.getDataSet().getDataSet();
        DataSetInfo dataSetInfo = new DataSetInfo();
        dataSetInfo.setItemId(dataSetId.intValue());
        dataSetInfo.setName(dataSetSchema.getDataSet().getName());
        dataSetInfo.setWords(dataSetSchema.getDataSet().getAlias());
        dataSetInfo.setBizName(dataSetSchema.getDataSet().getBizName());
        if (Objects.nonNull(dataSetSchema.getEntity())) {
            dataSetInfo.setPrimaryKey(dataSetSchema.getEntity().getBizName());
        }
        entityInfo.setDataSetInfo(dataSetInfo);
        TagTypeDefaultConfig tagTypeDefaultConfig = dataSetSchema.getTagTypeDefaultConfig();
        if (tagTypeDefaultConfig == null || tagTypeDefaultConfig.getDefaultDisplayInfo() == null) {
            return entityInfo;
        }
        List<DataInfo> dimensions = tagTypeDefaultConfig.getDefaultDisplayInfo().getDimensionIds().stream()
                .map(id -> {
                    SchemaElement element = dataSetSchema.getElement(SchemaElementType.DIMENSION, id);
                    if (element == null) {
                        return null;
                    }
                    return new DataInfo(element.getId().intValue(), element.getName(), element.getBizName(), null);
                }).filter(Objects::nonNull).collect(Collectors.toList());
        List<DataInfo> metrics = tagTypeDefaultConfig.getDefaultDisplayInfo().getDimensionIds().stream()
                .map(id -> {
                    SchemaElement element = dataSetSchema.getElement(SchemaElementType.METRIC, id);
                    if (element == null) {
                        return null;
                    }
                    return new DataInfo(element.getId().intValue(), element.getName(), element.getBizName(), null);
                }).filter(Objects::nonNull).collect(Collectors.toList());
        entityInfo.setDimensions(dimensions);
        entityInfo.setMetrics(metrics);
        return entityInfo;
    }

    private void fillEntityInfoValue(EntityInfo entityInfo, DataSetSchema dataSetSchema, User user) {
        SemanticQueryResp queryResultWithColumns =
                getQueryResultWithSchemaResp(entityInfo, dataSetSchema, user);
        if (queryResultWithColumns != null) {
            if (!org.springframework.util.CollectionUtils.isEmpty(queryResultWithColumns.getResultList())
                    && queryResultWithColumns.getResultList().size() > 0) {
                Map<String, Object> result = queryResultWithColumns.getResultList().get(0);
                for (Map.Entry<String, Object> entry : result.entrySet()) {
                    String entryKey = getEntryKey(entry);
                    if (entry.getValue() == null || entryKey == null) {
                        continue;
                    }
                    entityInfo.getDimensions().stream().filter(i -> entryKey.equals(i.getBizName()))
                            .forEach(i -> i.setValue(entry.getValue().toString()));
                    entityInfo.getMetrics().stream().filter(i -> entryKey.equals(i.getBizName()))
                            .forEach(i -> i.setValue(entry.getValue().toString()));
                }
            }
        }
    }

    private SemanticQueryResp getQueryResultWithSchemaResp(EntityInfo entityInfo,
            DataSetSchema dataSetSchema, User user) {
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        semanticParseInfo.setDataSet(dataSetSchema.getDataSet());
        semanticParseInfo.setQueryType(QueryType.DETAIL);
        semanticParseInfo.setMetrics(getMetrics(entityInfo));
        semanticParseInfo.setDimensions(getDimensions(entityInfo));
        DateConf dateInfo = new DateConf();
        int unit = 1;
        TimeDefaultConfig timeDefaultConfig = dataSetSchema.getTagTypeTimeDefaultConfig();
        if (Objects.nonNull(timeDefaultConfig)) {
            unit = timeDefaultConfig.getUnit();
            String date = LocalDate.now().plusDays(-unit).toString();
            dateInfo.setDateMode(DateConf.DateMode.BETWEEN);
            dateInfo.setStartDate(date);
            dateInfo.setEndDate(date);
        } else {
            dateInfo.setUnit(unit);
            dateInfo.setDateMode(DateConf.DateMode.RECENT);
        }
        semanticParseInfo.setDateInfo(dateInfo);

        //add filter
        QueryFilter chatFilter = getQueryFilter(entityInfo);
        Set<QueryFilter> chatFilters = new LinkedHashSet();
        chatFilters.add(chatFilter);
        semanticParseInfo.setDimensionFilters(chatFilters);

        SemanticQueryResp queryResultWithColumns = null;
        try {
            QueryStructReq queryStructReq = QueryReqBuilder.buildStructReq(semanticParseInfo);
            queryResultWithColumns = queryByReq(queryStructReq, user);
        } catch (Exception e) {
            log.warn("setMainModel queryByStruct error, e:", e);
        }
        return queryResultWithColumns;
    }

    private QueryFilter getQueryFilter(EntityInfo entityInfo) {
        QueryFilter chatFilter = new QueryFilter();
        chatFilter.setValue(entityInfo.getEntityId());
        chatFilter.setOperator(FilterOperatorEnum.EQUALS);
        chatFilter.setBizName(getEntityPrimaryName(entityInfo));
        return chatFilter;
    }

    private Set<SchemaElement> getDimensions(EntityInfo modelInfo) {
        Set<SchemaElement> dimensions = new LinkedHashSet();
        for (DataInfo mainEntityDimension : modelInfo.getDimensions()) {
            SchemaElement dimension = new SchemaElement();
            dimension.setBizName(mainEntityDimension.getBizName());
            dimensions.add(dimension);
        }
        return dimensions;
    }

    private String getEntryKey(Map.Entry<String, Object> entry) {
        // metric parser special handle, TODO delete
        String entryKey = entry.getKey();
        if (entryKey.contains("__")) {
            entryKey = entryKey.split("__")[1];
        }
        return entryKey;
    }

    private Set<SchemaElement> getMetrics(EntityInfo modelInfo) {
        Set<SchemaElement> metrics = new LinkedHashSet();
        for (DataInfo metricValue : modelInfo.getMetrics()) {
            SchemaElement metric = new SchemaElement();
            BeanUtils.copyProperties(metricValue, metric);
            metrics.add(metric);
        }
        return metrics;
    }

    private String getEntityPrimaryName(EntityInfo entityInfo) {
        return entityInfo.getDataSetInfo().getPrimaryKey();
    }

}
