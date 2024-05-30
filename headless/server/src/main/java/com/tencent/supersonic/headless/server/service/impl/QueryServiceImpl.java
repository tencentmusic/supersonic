package com.tencent.supersonic.headless.server.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.QueryParam;
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
import com.tencent.supersonic.headless.core.executor.accelerator.QueryAccelerator;
import com.tencent.supersonic.headless.core.parser.DefaultQueryParser;
import com.tencent.supersonic.headless.core.parser.QueryParser;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.SemanticModel;
import com.tencent.supersonic.headless.core.planner.QueryPlanner;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import com.tencent.supersonic.headless.server.annotation.S2DataPermission;
import com.tencent.supersonic.headless.server.manager.SemanticSchemaManager;
import com.tencent.supersonic.headless.server.service.AppService;
import com.tencent.supersonic.headless.server.service.Catalog;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.QueryService;
import com.tencent.supersonic.headless.server.utils.QueryReqConverter;
import com.tencent.supersonic.headless.server.utils.QueryUtils;
import com.tencent.supersonic.headless.server.utils.StatUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class QueryServiceImpl implements QueryService {

    private StatUtils statUtils;
    private final QueryUtils queryUtils;
    private final QueryReqConverter queryReqConverter;
    private final Catalog catalog;
    private final AppService appService;
    private final SemanticSchemaManager semanticSchemaManager;
    private final QueryParser queryParser;
    private final QueryPlanner queryPlanner;
    private final DataSetService dataSetService;

    public QueryServiceImpl(
            StatUtils statUtils,
            QueryUtils queryUtils,
            QueryReqConverter queryReqConverter,
            Catalog catalog,
            AppService appService,
            SemanticSchemaManager semanticSchemaManager,
            DefaultQueryParser queryParser,
            QueryPlanner queryPlanner,
            DataSetService dataSetService) {
        this.statUtils = statUtils;
        this.queryUtils = queryUtils;
        this.queryReqConverter = queryReqConverter;
        this.catalog = catalog;
        this.appService = appService;
        this.semanticSchemaManager = semanticSchemaManager;
        this.queryParser = queryParser;
        this.queryPlanner = queryPlanner;
        this.dataSetService = dataSetService;
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
            Object query = queryCache.query(queryReq, cacheKey);
            if (Objects.nonNull(query)) {
                return (SemanticQueryResp) query;
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
            queryStatement = plan(queryStatement);
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
        queryStatement = plan(queryStatement);
        return getExplainResp(queryStatement);
    }

    private ExplainResp getExplainResp(QueryStatement queryStatement) {
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

    private QueryStatement plan(QueryStatement queryStatement) throws Exception {
        queryParser.parse(queryStatement);
        queryPlanner.plan(queryStatement);
        return queryStatement;
    }

    private SemanticQueryResp query(QueryStatement queryStatement) throws Exception {
        SemanticQueryResp semanticQueryResp = null;
        try {
            //1 parse
            queryParser.parse(queryStatement);
            //2 plan
            QueryExecutor queryExecutor = queryPlanner.plan(queryStatement);
            //3 accelerate
            QueryAccelerator queryAccelerator = queryPlanner.accelerate(queryStatement);
            if (queryAccelerator != null) {
                semanticQueryResp = queryAccelerator.query(queryStatement);
                if (Objects.nonNull(semanticQueryResp) && !semanticQueryResp.getResultList().isEmpty()) {
                    log.info("query by Accelerator {}", queryAccelerator.getClass().getSimpleName());
                    return semanticQueryResp;
                }
            }
            //4 execute
            if (queryExecutor != null) {
                semanticQueryResp = queryExecutor.execute(queryStatement);
                queryUtils.fillItemNameInfo(semanticQueryResp, queryStatement.getSemanticSchemaResp());
            }
            return semanticQueryResp;
        } catch (Exception e) {
            log.error("exception in query, e: ", e);
            throw e;
        }
    }
}
