package com.tencent.supersonic.headless.server.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.ApiItemType;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.Item;
import com.tencent.supersonic.headless.api.pojo.SingleItemQueryResult;
import com.tencent.supersonic.headless.api.request.ExplainSqlReq;
import com.tencent.supersonic.headless.api.request.ItemUseReq;
import com.tencent.supersonic.headless.api.request.ModelSchemaFilterReq;
import com.tencent.supersonic.headless.api.request.QueryDimValueReq;
import com.tencent.supersonic.headless.api.request.QueryItemReq;
import com.tencent.supersonic.headless.api.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.api.response.AppDetailResp;
import com.tencent.supersonic.headless.api.response.DimensionResp;
import com.tencent.supersonic.headless.api.response.ExplainResp;
import com.tencent.supersonic.headless.api.response.ItemQueryResultResp;
import com.tencent.supersonic.headless.api.response.ItemUseResp;
import com.tencent.supersonic.headless.api.response.MetricResp;
import com.tencent.supersonic.headless.api.response.ModelResp;
import com.tencent.supersonic.headless.api.response.ModelSchemaResp;
import com.tencent.supersonic.headless.api.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.executor.QueryExecutor;
import com.tencent.supersonic.headless.core.parser.QueryParser;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.SemanticModel;
import com.tencent.supersonic.headless.core.planner.QueryPlanner;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.server.annotation.S2SQLDataPermission;
import com.tencent.supersonic.headless.server.annotation.StructDataPermission;
import com.tencent.supersonic.headless.server.aspect.ApiHeaderCheckAspect;
import com.tencent.supersonic.headless.server.cache.CacheManager;
import com.tencent.supersonic.headless.server.cache.QueryCache;
import com.tencent.supersonic.headless.server.manager.SemanticSchemaManager;
import com.tencent.supersonic.headless.server.pojo.DimensionFilter;
import com.tencent.supersonic.headless.server.service.AppService;
import com.tencent.supersonic.headless.server.service.Catalog;
import com.tencent.supersonic.headless.server.service.QueryService;
import com.tencent.supersonic.headless.server.service.SchemaService;
import com.tencent.supersonic.headless.server.service.SemantciQueryEngine;
import com.tencent.supersonic.headless.server.utils.QueryReqConverter;
import com.tencent.supersonic.headless.server.utils.QueryUtils;
import com.tencent.supersonic.headless.server.utils.StatUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class QueryServiceImpl implements QueryService {

    protected final Cache<String, List<ItemUseResp>> itemUseCache =
            CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();

    private final StatUtils statUtils;
    private final CacheManager cacheManager;
    private final QueryUtils queryUtils;
    private final QueryReqConverter queryReqConverter;
    private final Catalog catalog;
    private final AppService appService;

    @Value("${query.cache.enable:true}")
    private Boolean cacheEnable;

    private final QueryCache queryCache;

    private final SemantciQueryEngine semantciQueryEngine;
    private final SemanticSchemaManager semanticSchemaManager;

    private final QueryParser queryParser;

    private final QueryPlanner queryPlanner;

    public QueryServiceImpl(
            StatUtils statUtils,
            CacheManager cacheManager,
            QueryUtils queryUtils,
            QueryReqConverter queryReqConverter,
            @Lazy SemantciQueryEngine semantciQueryEngine,
            Catalog catalog,
            AppService appService,
            QueryCache queryCache,
            SemanticSchemaManager semanticSchemaManager,
            QueryParser queryParser,
            QueryPlanner queryPlanner) {
        this.statUtils = statUtils;
        this.cacheManager = cacheManager;
        this.queryUtils = queryUtils;
        this.queryReqConverter = queryReqConverter;
        this.semantciQueryEngine = semantciQueryEngine;
        this.catalog = catalog;
        this.appService = appService;
        this.queryCache = queryCache;
        this.semanticSchemaManager = semanticSchemaManager;
        this.queryParser = queryParser;
        this.queryPlanner = queryPlanner;
    }

    @Override
    @S2SQLDataPermission
    @SneakyThrows
    public Object queryBySql(QuerySqlReq querySQLReq, User user) {
        statUtils.initStatInfo(querySQLReq, user);
        QueryStatement queryStatement = new QueryStatement();
        try {
            queryStatement = convertToQueryStatement(querySQLReq, user);
        } catch (Exception e) {
            log.info("convertToQueryStatement has a exception:", e);
        }
        log.info("queryStatement:{}", queryStatement);
        SemanticQueryResp results = semantciQueryEngine.execute(queryStatement);
        statUtils.statInfo2DbAsync(TaskStatusEnum.SUCCESS);
        return results;
    }

    public SemanticQueryResp queryByQueryStatement(QueryStatement queryStatement) {
        return semantciQueryEngine.execute(queryStatement);
    }

    private QueryStatement convertToQueryStatement(QuerySqlReq querySQLReq, User user) throws Exception {
        ModelSchemaFilterReq filter = new ModelSchemaFilterReq();
        filter.setModelIds(querySQLReq.getModelIds());
        SchemaService schemaService = ContextUtils.getBean(SchemaService.class);
        List<ModelSchemaResp> modelSchemaResps = schemaService.fetchModelSchema(filter, user);
        QueryStatement queryStatement = queryReqConverter.convert(querySQLReq, modelSchemaResps);
        queryStatement.setModelIds(querySQLReq.getModelIds());
        return queryStatement;
    }

    @Override
    public SemanticQueryResp queryByStruct(QueryStructReq queryStructReq, User user) throws Exception {
        SemanticQueryResp semanticQueryResp = null;
        log.info("[queryStructReq:{}]", queryStructReq);
        try {
            //1.initStatInfo
            statUtils.initStatInfo(queryStructReq, user);
            //2.query from cache
            Object query = queryCache.query(queryStructReq);
            if (Objects.nonNull(query)) {
                statUtils.statInfo2DbAsync(TaskStatusEnum.SUCCESS);
                return (SemanticQueryResp) query;
            }
            StatUtils.get().setUseResultCache(false);
            //3 parse
            QueryStatement queryStatement = buildQueryStatement(queryStructReq);
            queryStatement = queryParser.parse(queryStatement);

            //4 plan
            QueryExecutor queryExecutor = queryPlanner.plan(queryStatement);

            //5 execute
            if (queryExecutor != null) {
                semanticQueryResp = queryExecutor.execute(queryStatement);
                if (!CollectionUtils.isEmpty(queryStatement.getModelIds())) {
                    queryUtils.fillItemNameInfo(semanticQueryResp, queryStatement.getModelIds());
                }
            }
            //6 reset cache and set stateInfo
            Boolean putCacheSuccess = queryCache.put(queryStructReq, semanticQueryResp);
            if (putCacheSuccess) {
                // if semanticQueryResp is not null, update cache data
                statUtils.updateResultCacheKey(queryCache.getCacheKey(queryStructReq));
            }
            if (Objects.nonNull(semanticQueryResp)) {
                statUtils.statInfo2DbAsync(TaskStatusEnum.SUCCESS);
            }
            return semanticQueryResp;
        } catch (Exception e) {
            log.warn("exception in queryByStruct, e: ", e);
            statUtils.statInfo2DbAsync(TaskStatusEnum.ERROR);
            throw e;
        }
    }

    private QueryStatement buildQueryStatement(QueryStructReq queryStructReq) throws Exception {
        QueryStatement queryStatement = new QueryStatement();
        queryStatement.setQueryStructReq(queryStructReq);
        queryStatement.setIsS2SQL(false);
        queryStatement.setEnableOptimize(queryUtils.enableOptimize());
        queryStatement.setModelIds(queryStatement.getQueryStructReq().getModelIds());
        SemanticModel semanticModel = semanticSchemaManager.get(queryStructReq.getModelIdStr());
        queryStatement.setSemanticModel(semanticModel);
        return queryStatement;
    }

    @Override
    @StructDataPermission
    @SneakyThrows
    public SemanticQueryResp queryByStructWithAuth(QueryStructReq queryStructReq, User user) {
        return queryByStruct(queryStructReq, user);
    }

    @Override
    public SemanticQueryResp queryByMultiStruct(QueryMultiStructReq queryMultiStructReq, User user)
            throws Exception {

        try {
            //1.initStatInfo
            statUtils.initStatInfo(queryMultiStructReq.getQueryStructReqs().get(0), user);
            //2.query from cache
            Object query = queryCache.query(queryMultiStructReq);
            if (Objects.nonNull(query)) {
                statUtils.statInfo2DbAsync(TaskStatusEnum.SUCCESS);
                return (SemanticQueryResp) query;
            }
            StatUtils.get().setUseResultCache(false);
            log.info("stat queryByStructWithoutCache, queryMultiStructReq:{}", queryMultiStructReq);
            List<QueryStatement> sqlParsers = new ArrayList<>();

            for (QueryStructReq queryStructReq : queryMultiStructReq.getQueryStructReqs()) {
                QueryStatement queryStatement = buildQueryStatement(queryStructReq);
                queryStatement = queryParser.parse(queryStatement);
                queryPlanner.optimizer(queryStatement);
                sqlParsers.add(queryStatement);
            }
            log.info("multi sqlParser:{}", sqlParsers);
            QueryStatement queryStatement = queryUtils.sqlParserUnion(queryMultiStructReq, sqlParsers);
            QueryExecutor executor = queryPlanner.route(queryStatement);

            SemanticQueryResp semanticQueryResp = null;
            if (executor != null) {
                semanticQueryResp = executor.execute(queryStatement);
                if (!CollectionUtils.isEmpty(queryStatement.getModelIds())) {
                    queryUtils.fillItemNameInfo(semanticQueryResp, queryStatement.getModelIds());
                }
            }
            if (Objects.nonNull(semanticQueryResp)) {
                statUtils.statInfo2DbAsync(TaskStatusEnum.SUCCESS);
            }
            return semanticQueryResp;
        } catch (Exception e) {
            log.warn("exception in queryByMultiStruct, e: ", e);
            statUtils.statInfo2DbAsync(TaskStatusEnum.ERROR);
            throw e;
        }
    }

    private QueryStatement buildQueryStatement(QueryMultiStructReq queryMultiStructReq) throws Exception {
        List<QueryStatement> sqlParsers = new ArrayList<>();
        for (QueryStructReq queryStructReq : queryMultiStructReq.getQueryStructReqs()) {
            QueryStatement queryStatement = buildQueryStatement(queryStructReq);
            queryStatement = semantciQueryEngine.plan(queryStatement);
            queryUtils.checkSqlParse(queryStatement);
            sqlParsers.add(queryStatement);
        }
        log.info("multi sqlParser:{}", sqlParsers);
        return queryUtils.sqlParserUnion(queryMultiStructReq, sqlParsers);
    }

    @Override
    @SneakyThrows
    public SemanticQueryResp queryDimValue(QueryDimValueReq queryDimValueReq, User user) {
        QuerySqlReq querySQLReq = buildQuerySqlReq(queryDimValueReq);
        return (SemanticQueryResp) queryBySql(querySQLReq, user);
    }

    @Override
    @SneakyThrows
    public List<ItemUseResp> getStatInfo(ItemUseReq itemUseReq) {
        if (itemUseReq.getCacheEnable()) {
            return itemUseCache.get(JsonUtil.toString(itemUseReq), () -> {
                List<ItemUseResp> data = statUtils.getStatInfo(itemUseReq);
                itemUseCache.put(JsonUtil.toString(itemUseReq), data);
                return data;
            });
        }
        return statUtils.getStatInfo(itemUseReq);
    }

    @Override
    public <T> ExplainResp explain(ExplainSqlReq<T> explainSqlReq, User user) throws Exception {
        QueryType queryTypeEnum = explainSqlReq.getQueryTypeEnum();
        T queryReq = explainSqlReq.getQueryReq();

        if (QueryType.SQL.equals(queryTypeEnum) && queryReq instanceof QuerySqlReq) {
            QueryStatement queryStatement = convertToQueryStatement((QuerySqlReq) queryReq, user);
            return getExplainResp(queryStatement);
        }
        if (QueryType.STRUCT.equals(queryTypeEnum) && queryReq instanceof QueryStructReq) {
            QueryStatement queryStatement = buildQueryStatement((QueryStructReq) queryReq);
            queryStatement = semantciQueryEngine.plan(queryStatement);
            return getExplainResp(queryStatement);
        }
        if (QueryType.STRUCT.equals(queryTypeEnum) && queryReq instanceof QueryMultiStructReq) {
            QueryMultiStructReq queryMultiStructReq = (QueryMultiStructReq) queryReq;
            QueryStatement queryStatement = buildQueryStatement(queryMultiStructReq);
            return getExplainResp(queryStatement);
        }

        throw new IllegalArgumentException("Parameters are invalid, explainSqlReq: " + explainSqlReq);
    }

    @Override
    public ItemQueryResultResp queryMetricDataById(QueryItemReq queryItemReq,
            HttpServletRequest request) throws Exception {
        AppDetailResp appDetailResp = getAppDetailResp(request);
        authCheck(appDetailResp, queryItemReq.getIds(), ApiItemType.METRIC);
        List<SingleItemQueryResult> results = Lists.newArrayList();
        Map<Long, Item> map = appDetailResp.getConfig().getItems().stream()
                .collect(Collectors.toMap(Item::getId, i -> i));
        for (Long id : queryItemReq.getIds()) {
            Item item = map.get(id);
            SingleItemQueryResult apiQuerySingleResult = dataQuery(appDetailResp.getId(),
                    item, queryItemReq.getDateConf(), queryItemReq.getLimit());
            results.add(apiQuerySingleResult);
        }
        return ItemQueryResultResp.builder().results(results).build();
    }

    private SingleItemQueryResult dataQuery(Integer appId, Item item, DateConf dateConf, Long limit) throws Exception {
        MetricResp metricResp = catalog.getMetric(item.getId());
        item.setCreatedBy(metricResp.getCreatedBy());
        item.setBizName(metricResp.getBizName());
        item.setName(metricResp.getName());
        List<Item> items = item.getRelateItems();
        List<DimensionResp> dimensionResps = Lists.newArrayList();
        if (!org.springframework.util.CollectionUtils.isEmpty(items)) {
            List<Long> ids = items.stream().map(Item::getId).collect(Collectors.toList());
            DimensionFilter dimensionFilter = new DimensionFilter();
            dimensionFilter.setIds(ids);
            dimensionResps = catalog.getDimensions(dimensionFilter);
        }
        QueryStructReq queryStructReq = buildQueryStructReq(dimensionResps, metricResp, dateConf, limit);
        SemanticQueryResp semanticQueryResp =
                queryByStruct(queryStructReq, User.getAppUser(appId));
        SingleItemQueryResult apiQuerySingleResult = new SingleItemQueryResult();
        apiQuerySingleResult.setItem(item);
        apiQuerySingleResult.setResult(semanticQueryResp);
        return apiQuerySingleResult;
    }

    private AppDetailResp getAppDetailResp(HttpServletRequest request) {
        int appId = Integer.parseInt(request.getHeader(ApiHeaderCheckAspect.APPID));
        return appService.getApp(appId);
    }

    private QueryStructReq buildQueryStructReq(List<DimensionResp> dimensionResps,
            MetricResp metricResp, DateConf dateConf, Long limit) {
        Set<Long> modelIds = dimensionResps.stream().map(DimensionResp::getModelId).collect(Collectors.toSet());
        modelIds.add(metricResp.getModelId());
        QueryStructReq queryStructReq = new QueryStructReq();
        queryStructReq.setGroups(dimensionResps.stream()
                .map(DimensionResp::getBizName).collect(Collectors.toList()));
        queryStructReq.getGroups().add(0, getTimeDimension(dateConf));
        Aggregator aggregator = new Aggregator();
        aggregator.setColumn(metricResp.getBizName());
        queryStructReq.setAggregators(Lists.newArrayList(aggregator));
        queryStructReq.setDateInfo(dateConf);
        queryStructReq.setModelIds(modelIds);
        queryStructReq.setLimit(limit);
        return queryStructReq;
    }

    private String getTimeDimension(DateConf dateConf) {
        if (Constants.MONTH.equals(dateConf.getPeriod())) {
            return TimeDimensionEnum.MONTH.getName();
        } else if (Constants.WEEK.equals(dateConf.getPeriod())) {
            return TimeDimensionEnum.WEEK.getName();
        } else {
            return TimeDimensionEnum.DAY.getName();
        }
    }

    private void authCheck(AppDetailResp appDetailResp, List<Long> ids, ApiItemType type) {
        Set<Long> idsInApp = appDetailResp.getConfig().getAllItems().stream()
                .filter(item -> type.equals(item.getType())).map(Item::getId).collect(Collectors.toSet());
        if (!idsInApp.containsAll(ids)) {
            throw new InvalidArgumentException("查询范围超过应用申请范围, 请检查");
        }
    }

    private ExplainResp getExplainResp(QueryStatement queryStatement) {
        String sql = "";
        if (Objects.nonNull(queryStatement)) {
            sql = queryStatement.getSql();
        }
        return ExplainResp.builder().sql(sql).build();
    }

    private boolean isCache(QueryMultiStructReq queryStructReq) {
        if (!cacheEnable) {
            return false;
        }
        if (!CollectionUtils.isEmpty(queryStructReq.getQueryStructReqs())
                && queryStructReq.getQueryStructReqs().get(0).getCacheInfo() != null) {
            return queryStructReq.getQueryStructReqs().get(0).getCacheInfo().getCache();
        }
        return false;
    }

    private SemanticQueryResp queryByCache(String key, Object queryCmd) {

        Object resultObject = cacheManager.get(key);
        if (Objects.nonNull(resultObject)) {
            log.info("queryByStructWithCache, key:{}, queryCmd:{}", key, queryCmd.toString());
            statUtils.updateResultCacheKey(key);
            return (SemanticQueryResp) resultObject;
        }
        return null;
    }

    private QuerySqlReq buildQuerySqlReq(QueryDimValueReq queryDimValueReq) {
        QuerySqlReq querySQLReq = new QuerySqlReq();
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
        querySQLReq.setModelIds(Sets.newHashSet(queryDimValueReq.getModelId()));
        querySQLReq.setSql(sql);
        return querySQLReq;
    }

    private String getKeyByModelIds(List<Long> modelIds) {
        return String.join(",", modelIds.stream()
                .map(Object::toString).collect(Collectors.toList()));
    }

}
