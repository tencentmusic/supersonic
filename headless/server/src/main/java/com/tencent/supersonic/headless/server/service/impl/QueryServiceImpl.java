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
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.Item;
import com.tencent.supersonic.headless.api.pojo.SingleItemQueryResult;
import com.tencent.supersonic.headless.api.pojo.request.ExplainSqlReq;
import com.tencent.supersonic.headless.api.pojo.request.ItemUseReq;
import com.tencent.supersonic.headless.api.pojo.request.ModelSchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.request.ParseSqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryDimValueReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryItemReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.AppDetailResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.ExplainResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemQueryResultResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemUseResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.cache.QueryCache;
import com.tencent.supersonic.headless.core.executor.QueryExecutor;
import com.tencent.supersonic.headless.core.parser.DefaultQueryParser;
import com.tencent.supersonic.headless.core.parser.QueryParser;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.SemanticModel;
import com.tencent.supersonic.headless.core.planner.QueryPlanner;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.server.annotation.S2SQLDataPermission;
import com.tencent.supersonic.headless.server.annotation.StructDataPermission;
import com.tencent.supersonic.headless.server.aspect.ApiHeaderCheckAspect;
import com.tencent.supersonic.headless.server.manager.SemanticSchemaManager;
import com.tencent.supersonic.headless.server.pojo.DimensionFilter;
import com.tencent.supersonic.headless.server.service.AppService;
import com.tencent.supersonic.headless.server.service.Catalog;
import com.tencent.supersonic.headless.server.service.QueryService;
import com.tencent.supersonic.headless.server.service.SchemaService;
import com.tencent.supersonic.headless.server.utils.QueryReqConverter;
import com.tencent.supersonic.headless.server.utils.QueryUtils;
import com.tencent.supersonic.headless.server.utils.StatUtils;
import java.util.ArrayList;
import java.util.HashSet;
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
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class QueryServiceImpl implements QueryService {

    protected final Cache<String, List<ItemUseResp>> itemUseCache =
            CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();

    private final StatUtils statUtils;
    private final QueryUtils queryUtils;
    private final QueryReqConverter queryReqConverter;
    private final Catalog catalog;
    private final AppService appService;
    private final QueryCache queryCache;
    private final SemanticSchemaManager semanticSchemaManager;

    private final QueryParser queryParser;

    private final QueryPlanner queryPlanner;

    public QueryServiceImpl(
            StatUtils statUtils,
            QueryUtils queryUtils,
            QueryReqConverter queryReqConverter,
            Catalog catalog,
            AppService appService,
            QueryCache queryCache,
            SemanticSchemaManager semanticSchemaManager,
            DefaultQueryParser queryParser,
            QueryPlanner queryPlanner) {
        this.statUtils = statUtils;
        this.queryUtils = queryUtils;
        this.queryReqConverter = queryReqConverter;
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
    public SemanticQueryResp queryBySql(QuerySqlReq querySQLReq, User user) {
        return queryBySemanticQuery(querySQLReq, user);
    }

    @Override
    public SemanticQueryResp queryByStruct(QueryStructReq queryStructCmd, User user) throws Exception {
        return queryBySemanticQuery(queryStructCmd, user);
    }

    public SemanticQueryResp queryByQueryStatement(QueryStatement queryStatement) {

        SemanticQueryResp queryResultWithColumns = null;
        QueryExecutor queryExecutor = queryPlanner.route(queryStatement);
        if (queryExecutor != null) {
            queryResultWithColumns = queryExecutor.execute(queryStatement);
            queryResultWithColumns.setSql(queryStatement.getSql());
            if (!CollectionUtils.isEmpty(queryStatement.getModelIds())) {
                queryUtils.fillItemNameInfo(queryResultWithColumns, queryStatement.getModelIds());
            }
        }
        return queryResultWithColumns;

    }

    private QueryStatement buildSqlQueryStatement(QuerySqlReq querySQLReq, User user) throws Exception {
        ModelSchemaFilterReq filter = new ModelSchemaFilterReq();
        filter.setModelIds(querySQLReq.getModelIds());
        SchemaService schemaService = ContextUtils.getBean(SchemaService.class);
        List<ModelSchemaResp> modelSchemaResps = schemaService.fetchModelSchema(filter, user);
        QueryStatement queryStatement = queryReqConverter.convert(querySQLReq, modelSchemaResps);
        queryStatement.setModelIds(querySQLReq.getModelIds());
        queryStatement.setEnableOptimize(queryUtils.enableOptimize());
        SemanticModel semanticModel = semanticSchemaManager.get(querySQLReq.getModelIdStr());
        queryStatement.setSemanticModel(semanticModel);
        return queryStatement;
    }

    @Override
    public SemanticQueryResp queryBySemanticQuery(SemanticQueryReq semanticQueryReq, User user) throws Exception {
        TaskStatusEnum state = TaskStatusEnum.SUCCESS;
        log.info("[semanticQueryReq:{}]", semanticQueryReq);
        try {
            //1.initStatInfo
            statUtils.initStatInfo(semanticQueryReq, user);
            //2.query from cache
            Object query = queryCache.query(semanticQueryReq);
            if (Objects.nonNull(query)) {
                return (SemanticQueryResp) query;
            }
            StatUtils.get().setUseResultCache(false);
            //3 query
            QueryStatement queryStatement = buildQueryStatement(semanticQueryReq, user);
            SemanticQueryResp result = query(queryStatement);
            //4 reset cache and set stateInfo
            Boolean setCacheSuccess = queryCache.put(semanticQueryReq, result);
            if (setCacheSuccess) {
                // if result is not null, update cache data
                statUtils.updateResultCacheKey(queryCache.getCacheKey(semanticQueryReq));
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

    private QueryStatement buildQueryStatement(SemanticQueryReq semanticQueryReq, User user) throws Exception {
        if (semanticQueryReq instanceof QuerySqlReq) {
            return buildSqlQueryStatement((QuerySqlReq) semanticQueryReq, user);
        }
        if (semanticQueryReq instanceof QueryStructReq) {
            return buildStructQueryStatement((QueryStructReq) semanticQueryReq, user);
        }
        if (semanticQueryReq instanceof QueryMultiStructReq) {
            return buildMultiStructQueryStatement((QueryMultiStructReq) semanticQueryReq, user);
        }
        return null;
    }

    private QueryStatement buildStructQueryStatement(QueryStructReq queryStructReq, User user) throws Exception {
        QueryStatement queryStatement = new QueryStatement();
        queryStatement.setQueryStructReq(queryStructReq);
        queryStatement.setIsS2SQL(false);
        queryStatement.setEnableOptimize(queryUtils.enableOptimize());
        queryStatement.setModelIds(queryStatement.getQueryStructReq().getModelIds());
        SemanticModel semanticModel = semanticSchemaManager.get(queryStructReq.getModelIdStr());
        queryStatement.setSemanticModel(semanticModel);
        return queryStatement;
    }

    private QueryStatement buildMultiStructQueryStatement(QueryMultiStructReq queryMultiStructReq, User user)
            throws Exception {
        List<QueryStatement> sqlParsers = new ArrayList<>();
        for (QueryStructReq queryStructReq : queryMultiStructReq.getQueryStructReqs()) {
            QueryStatement queryStatement = buildQueryStatement(queryStructReq, user);
            SemanticModel semanticModel = semanticSchemaManager.get(queryStructReq.getModelIdStr());
            queryStatement.setModelIds(queryStatement.getQueryStructReq().getModelIds());
            queryStatement.setSemanticModel(semanticModel);
            queryStatement.setEnableOptimize(queryUtils.enableOptimize());
            queryStatement = plan(queryStatement);
            sqlParsers.add(queryStatement);
        }
        log.info("multi sqlParser:{}", sqlParsers);
        return queryUtils.sqlParserUnion(queryMultiStructReq, sqlParsers);
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
        TaskStatusEnum state = TaskStatusEnum.SUCCESS;
        try {
            //1.initStatInfo
            statUtils.initStatInfo(queryMultiStructReq.getQueryStructReqs().get(0), user);
            //2.query from cache
            Object query = queryCache.query(queryMultiStructReq);
            if (Objects.nonNull(query)) {
                return (SemanticQueryResp) query;
            }
            StatUtils.get().setUseResultCache(false);

            //3.parse and optimizer
            List<QueryStatement> sqlParsers = new ArrayList<>();
            for (QueryStructReq queryStructReq : queryMultiStructReq.getQueryStructReqs()) {
                QueryStatement queryStatement = buildQueryStatement(queryStructReq, user);
                queryParser.parse(queryStatement);
                queryPlanner.plan(queryStatement);
                sqlParsers.add(queryStatement);
            }
            log.info("multi sqlParser:{}", sqlParsers);
            QueryStatement queryStatement = queryUtils.sqlParserUnion(queryMultiStructReq, sqlParsers);

            //4.route
            QueryExecutor executor = queryPlanner.route(queryStatement);

            SemanticQueryResp semanticQueryResp = null;
            if (executor != null) {
                semanticQueryResp = executor.execute(queryStatement);
                if (!CollectionUtils.isEmpty(queryStatement.getModelIds())) {
                    queryUtils.fillItemNameInfo(semanticQueryResp, queryStatement.getModelIds());
                }
            }
            if (Objects.isNull(semanticQueryResp)) {
                state = TaskStatusEnum.ERROR;
            }
            return semanticQueryResp;
        } catch (Exception e) {
            log.error("exception in queryByMultiStruct, e: ", e);
            state = TaskStatusEnum.ERROR;
            throw e;
        } finally {
            statUtils.statInfo2DbAsync(state);
        }
    }

    @Override
    @SneakyThrows
    public SemanticQueryResp queryDimValue(QueryDimValueReq queryDimValueReq, User user) {
        QuerySqlReq querySQLReq = buildQuerySqlReq(queryDimValueReq);
        return queryBySql(querySQLReq, user);
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
        T queryReq = explainSqlReq.getQueryReq();
        QueryStatement queryStatement = buildQueryStatement((QuerySqlReq) queryReq, user);
        queryStatement = plan(queryStatement);
        return getExplainResp(queryStatement);
    }

    @Override
    public QueryStatement explain(ParseSqlReq parseSqlReq) throws Exception {
        QueryStructReq queryStructCmd = new QueryStructReq();
        Set<Long> models = new HashSet<>();
        models.add(Long.valueOf(parseSqlReq.getRootPath()));
        queryStructCmd.setModelIds(models);
        QueryStatement queryStatement = new QueryStatement();
        queryStatement.setQueryStructReq(queryStructCmd);
        queryStatement.setParseSqlReq(parseSqlReq);
        queryStatement.setSql(parseSqlReq.getSql());
        queryStatement.setIsS2SQL(true);
        SemanticModel semanticModel = semanticSchemaManager.get(parseSqlReq.getRootPath());
        queryStatement.setSemanticModel(semanticModel);
        return plan(queryStatement);
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

    private QueryStatement plan(QueryStatement queryStatement) throws Exception {
        queryParser.parse(queryStatement);
        log.info("queryStatement:{}", queryStatement);
        queryPlanner.plan(queryStatement);
        return queryStatement;
    }

    private SemanticQueryResp query(QueryStatement queryStatement) throws Exception {
        SemanticQueryResp semanticQueryResp = null;
        log.info("[QueryStatement:{}]", queryStatement);
        try {
            //1 parse
            queryParser.parse(queryStatement);
            //2 plan
            QueryExecutor queryExecutor = queryPlanner.plan(queryStatement);
            //3 execute
            if (queryExecutor != null) {
                semanticQueryResp = queryExecutor.execute(queryStatement);
                if (!CollectionUtils.isEmpty(queryStatement.getModelIds())) {
                    queryUtils.fillItemNameInfo(semanticQueryResp, queryStatement.getModelIds());
                }
            }
            return semanticQueryResp;
        } catch (Exception e) {
            log.error("exception in query, e: ", e);
            throw e;
        }
    }
}
