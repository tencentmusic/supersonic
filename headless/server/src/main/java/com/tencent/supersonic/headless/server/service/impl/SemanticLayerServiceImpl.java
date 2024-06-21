package com.tencent.supersonic.headless.server.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.QueryParam;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.request.ExplainSqlReq;
import com.tencent.supersonic.headless.api.pojo.request.ItemUseReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryDimValueReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.ExplainResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemUseResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.cache.QueryCache;
import com.tencent.supersonic.headless.core.executor.QueryExecutor;
import com.tencent.supersonic.headless.core.translator.SemanticTranslator;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.SemanticModel;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import com.tencent.supersonic.headless.server.annotation.S2DataPermission;
import com.tencent.supersonic.headless.server.manager.SemanticSchemaManager;
import com.tencent.supersonic.headless.server.service.CatalogService;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.SchemaService;
import com.tencent.supersonic.headless.server.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.utils.QueryReqConverter;
import com.tencent.supersonic.headless.server.utils.QueryUtils;
import com.tencent.supersonic.headless.server.utils.StatUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
@Slf4j
public class SemanticLayerServiceImpl implements SemanticLayerService {

    private StatUtils statUtils;
    private final QueryUtils queryUtils;
    private final QueryReqConverter queryReqConverter;
    private final CatalogService catalog;
    private final SemanticSchemaManager semanticSchemaManager;
    private final DataSetService dataSetService;
    private final SchemaService schemaService;
    private final SemanticTranslator semanticTranslator;

    public SemanticLayerServiceImpl(
            StatUtils statUtils,
            QueryUtils queryUtils,
            QueryReqConverter queryReqConverter,
            CatalogService catalog,
            SemanticSchemaManager semanticSchemaManager,
            DataSetService dataSetService,
            SchemaService schemaService,
            SemanticTranslator semanticTranslator) {
        this.statUtils = statUtils;
        this.queryUtils = queryUtils;
        this.queryReqConverter = queryReqConverter;
        this.catalog = catalog;
        this.semanticSchemaManager = semanticSchemaManager;
        this.dataSetService = dataSetService;
        this.schemaService = schemaService;
        this.semanticTranslator = semanticTranslator;
    }

    public SemanticSchema getSemanticSchema() {
        return new SemanticSchema(schemaService.getDataSetSchema());
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
            log.info("cacheKey:{}", cacheKey);
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
        SemanticSchemaResp semanticSchemaResp = catalog.fetchSemanticSchema(filter);
        QueryStatement queryStatement = queryReqConverter.convert(querySqlReq, semanticSchemaResp);
        queryStatement.setModelIds(querySqlReq.getModelIds());
        queryStatement.setEnableOptimize(queryUtils.enableOptimize());
        queryStatement.setSemanticSchemaResp(semanticSchemaResp);
        queryStatement.setSemanticModel(semanticSchemaManager.getSemanticModel(semanticSchemaResp));
        return queryStatement;
    }

    private QueryStatement buildQueryStatement(SemanticQueryReq semanticQueryReq, User user) throws Exception {
        if (semanticQueryReq instanceof QuerySqlReq) {
            return buildSqlQueryStatement((QuerySqlReq) semanticQueryReq, user);
        }
        if (semanticQueryReq instanceof QueryStructReq) {
            return buildStructQueryStatement((QueryStructReq) semanticQueryReq);
        }
        if (semanticQueryReq instanceof QueryMultiStructReq) {
            return buildMultiStructQueryStatement((QueryMultiStructReq) semanticQueryReq, user);
        }
        return null;
    }

    private QueryStatement buildStructQueryStatement(QueryStructReq queryStructReq) {
        SchemaFilterReq filter = buildSchemaFilterReq(queryStructReq);
        SemanticSchemaResp semanticSchemaResp = catalog.fetchSemanticSchema(filter);
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
    @SneakyThrows
    public List<ItemUseResp> getStatInfo(ItemUseReq itemUseReq) {
        return catalog.getStatInfo(itemUseReq);
    }

    @Override
    public <T> ExplainResp explain(ExplainSqlReq<T> explainSqlReq, User user) throws Exception {
        T queryReq = explainSqlReq.getQueryReq();
        QueryStatement queryStatement = buildQueryStatement((SemanticQueryReq) queryReq, user);
        semanticTranslator.translate(queryStatement);

        String sql = "";
        if (Objects.nonNull(queryStatement)) {
            sql = queryStatement.getSql();
        }
        return ExplainResp.builder().sql(sql).build();
    }

    private QuerySqlReq buildQuerySqlReq(QueryDimValueReq queryDimValueReq) {
        QuerySqlReq querySqlReq = new QuerySqlReq();
        List<ModelResp> modelResps = catalog.getModelList(Lists.newArrayList(queryDimValueReq.getModelId()));
        DimensionResp dimensionResp = catalog.getDimension(queryDimValueReq.getDimensionBizName(),
                queryDimValueReq.getModelId());
        ModelResp modelResp = modelResps.get(0);
        String sql = String.format("select distinct %s from %s", dimensionResp.getName(), modelResp.getName());
        List<Dim> timeDims = modelResp.getTimeDimension();
        if (CollectionUtils.isNotEmpty(timeDims)) {
            sql = String.format("%s where %s >= '%s' and %s <= '%s'", sql, TimeDimensionEnum.DAY.getName(),
                    queryDimValueReq.getDateInfo().getStartDate(), TimeDimensionEnum.DAY.getName(),
                    queryDimValueReq.getDateInfo().getEndDate());
        }
        querySqlReq.setModelIds(Sets.newHashSet(queryDimValueReq.getModelId()));
        querySqlReq.setSql(sql);
        return querySqlReq;
    }

    private SemanticQueryResp query(QueryStatement queryStatement) throws Exception {
        SemanticQueryResp semanticQueryResp = null;
        try {
            //1 translate
            semanticTranslator.translate(queryStatement);

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
}
