package com.tencent.supersonic.headless.core.service.impl;

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
import com.tencent.supersonic.common.util.cache.CacheUtils;
import com.tencent.supersonic.headless.common.core.pojo.SingleItemQueryResult;
import com.tencent.supersonic.headless.common.core.pojo.Cache;
import com.tencent.supersonic.headless.common.core.request.ExplainSqlReq;
import com.tencent.supersonic.headless.common.core.request.ItemUseReq;
import com.tencent.supersonic.headless.common.core.request.MetricReq;
import com.tencent.supersonic.headless.common.core.request.QueryItemReq;
import com.tencent.supersonic.headless.common.core.request.QueryDimValueReq;
import com.tencent.supersonic.headless.common.core.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.common.core.request.QueryS2SQLReq;
import com.tencent.supersonic.headless.common.core.request.QueryStructReq;
import com.tencent.supersonic.headless.common.core.response.ItemQueryResultResp;
import com.tencent.supersonic.headless.common.core.response.ItemUseResp;
import com.tencent.supersonic.headless.common.server.enums.QueryType;
import com.tencent.supersonic.headless.common.server.pojo.Dim;
import com.tencent.supersonic.headless.common.server.pojo.Item;
import com.tencent.supersonic.headless.common.server.request.ModelSchemaFilterReq;
import com.tencent.supersonic.headless.common.server.response.AppDetailResp;
import com.tencent.supersonic.headless.common.server.response.DimensionResp;
import com.tencent.supersonic.headless.common.server.response.ExplainResp;
import com.tencent.supersonic.headless.common.server.response.MetricResp;
import com.tencent.supersonic.headless.common.server.response.ModelResp;
import com.tencent.supersonic.headless.common.server.response.ModelSchemaResp;
import com.tencent.supersonic.headless.common.server.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.core.annotation.ApiHeaderCheck;
import com.tencent.supersonic.headless.core.annotation.S2SQLDataPermission;
import com.tencent.supersonic.headless.core.annotation.StructDataPermission;
import com.tencent.supersonic.headless.core.aspect.ApiHeaderCheckAspect;
import com.tencent.supersonic.headless.core.executor.QueryExecutor;
import com.tencent.supersonic.headless.core.parser.convert.QueryReqConverter;
import com.tencent.supersonic.headless.core.persistence.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.service.HeadlessQueryEngine;
import com.tencent.supersonic.headless.core.service.QueryService;
import com.tencent.supersonic.headless.core.service.SchemaService;
import com.tencent.supersonic.headless.core.utils.QueryUtils;
import com.tencent.supersonic.headless.core.utils.StatUtils;
import com.tencent.supersonic.headless.server.pojo.DimensionFilter;
import com.tencent.supersonic.headless.server.service.AppService;
import com.tencent.supersonic.headless.server.service.Catalog;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@Slf4j
public class QueryServiceImpl implements QueryService {

    protected final com.google.common.cache.Cache<String, List<ItemUseResp>> itemUseCache =
            CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();

    private final StatUtils statUtils;
    private final CacheUtils cacheUtils;
    private final QueryUtils queryUtils;
    private final QueryReqConverter queryReqConverter;
    private final Catalog catalog;
    private final AppService appService;

    @Value("${query.cache.enable:true}")
    private Boolean cacheEnable;

    private final HeadlessQueryEngine headlessQueryEngine;

    public QueryServiceImpl(
            StatUtils statUtils,
            CacheUtils cacheUtils,
            QueryUtils queryUtils,
            QueryReqConverter queryReqConverter,
            HeadlessQueryEngine headlessQueryEngine,
            Catalog catalog,
            AppService appService) {
        this.statUtils = statUtils;
        this.cacheUtils = cacheUtils;
        this.queryUtils = queryUtils;
        this.queryReqConverter = queryReqConverter;
        this.headlessQueryEngine = headlessQueryEngine;
        this.catalog = catalog;
        this.appService = appService;
    }

    @Override
    @S2SQLDataPermission
    @SneakyThrows
    public Object queryBySql(QueryS2SQLReq queryS2SQLReq, User user) {
        statUtils.initStatInfo(queryS2SQLReq, user);
        QueryStatement queryStatement = new QueryStatement();
        try {
            queryStatement = convertToQueryStatement(queryS2SQLReq, user);
        } catch (Exception e) {
            log.info("convertToQueryStatement has a exception:", e);
        }
        log.info("queryStatement:{}", queryStatement);
        QueryResultWithSchemaResp results = headlessQueryEngine.execute(queryStatement);
        statUtils.statInfo2DbAsync(TaskStatusEnum.SUCCESS);
        return results;
    }

    public Object queryByQueryStatement(QueryStatement queryStatement) {
        return headlessQueryEngine.execute(queryStatement);
    }

    private QueryStatement convertToQueryStatement(QueryS2SQLReq querySqlCmd, User user) throws Exception {
        ModelSchemaFilterReq filter = new ModelSchemaFilterReq();
        filter.setModelIds(querySqlCmd.getModelIds());
        SchemaService schemaService = ContextUtils.getBean(SchemaService.class);
        List<ModelSchemaResp> modelSchemaResps = schemaService.fetchModelSchema(filter, user);
        QueryStatement queryStatement = queryReqConverter.convert(querySqlCmd, modelSchemaResps);
        queryStatement.setModelIds(querySqlCmd.getModelIds());
        return queryStatement;
    }

    @Override
    public QueryResultWithSchemaResp queryByStruct(QueryStructReq queryStructCmd, User user) throws Exception {
        QueryResultWithSchemaResp queryResultWithColumns = null;
        log.info("[queryStructCmd:{}]", queryStructCmd);
        try {
            statUtils.initStatInfo(queryStructCmd, user);
            String cacheKey = cacheUtils.generateCacheKey(getKeyByModelIds(queryStructCmd.getModelIds()),
                    queryStructCmd.generateCommandMd5());
            handleGlobalCacheDisable(queryStructCmd);
            boolean isCache = isCache(queryStructCmd);
            if (isCache) {
                queryResultWithColumns = queryByCache(cacheKey, queryStructCmd);
                if (queryResultWithColumns != null) {
                    statUtils.statInfo2DbAsync(TaskStatusEnum.SUCCESS);
                    return queryResultWithColumns;
                }
            }
            StatUtils.get().setUseResultCache(false);
            QueryStatement queryStatement = new QueryStatement();
            queryStatement.setQueryStructReq(queryStructCmd);
            queryStatement.setIsS2SQL(false);
            queryStatement = headlessQueryEngine.plan(queryStatement);
            QueryExecutor queryExecutor = headlessQueryEngine.route(queryStatement);
            if (queryExecutor != null) {
                queryResultWithColumns = headlessQueryEngine.execute(queryStatement);
                if (isCache) {
                    // if queryResultWithColumns is not null, update cache data
                    queryUtils.cacheResultLogic(cacheKey, queryResultWithColumns);
                }
            }
            statUtils.statInfo2DbAsync(TaskStatusEnum.SUCCESS);
            return queryResultWithColumns;
        } catch (Exception e) {
            log.warn("exception in queryByStruct, e: ", e);
            statUtils.statInfo2DbAsync(TaskStatusEnum.ERROR);
            throw e;
        }
    }

    @Override
    @StructDataPermission
    @SneakyThrows
    public QueryResultWithSchemaResp queryByStructWithAuth(QueryStructReq queryStructCmd, User user) {
        return queryByStruct(queryStructCmd, user);
    }

    @Override
    public QueryResultWithSchemaResp queryByMultiStruct(QueryMultiStructReq queryMultiStructReq, User user)
            throws Exception {
        statUtils.initStatInfo(queryMultiStructReq.getQueryStructReqs().get(0), user);
        String cacheKey = cacheUtils.generateCacheKey(
                getKeyByModelIds(queryMultiStructReq.getQueryStructReqs().get(0).getModelIds()),
                queryMultiStructReq.generateCommandMd5());
        boolean isCache = isCache(queryMultiStructReq);
        QueryResultWithSchemaResp queryResultWithColumns;
        if (isCache) {
            queryResultWithColumns = queryByCache(cacheKey, queryMultiStructReq);
            if (queryResultWithColumns != null) {
                statUtils.statInfo2DbAsync(TaskStatusEnum.SUCCESS);
                return queryResultWithColumns;
            }
        }
        log.info("stat queryByStructWithoutCache, queryMultiStructReq:{}", queryMultiStructReq);
        try {
            QueryStatement sqlParser = getQueryStatementByMultiStruct(queryMultiStructReq);
            queryResultWithColumns = headlessQueryEngine.execute(sqlParser);
            if (queryResultWithColumns != null) {
                statUtils.statInfo2DbAsync(TaskStatusEnum.SUCCESS);
                queryUtils.fillItemNameInfo(queryResultWithColumns, queryMultiStructReq);
            }
            return queryResultWithColumns;
        } catch (Exception e) {
            log.warn("exception in queryByMultiStruct, e: ", e);
            statUtils.statInfo2DbAsync(TaskStatusEnum.ERROR);
            throw e;
        }
    }

    private QueryStatement getQueryStatementByMultiStruct(QueryMultiStructReq queryMultiStructReq) throws Exception {
        List<QueryStatement> sqlParsers = new ArrayList<>();
        for (QueryStructReq queryStructCmd : queryMultiStructReq.getQueryStructReqs()) {
            QueryStatement queryStatement = new QueryStatement();
            queryStatement.setQueryStructReq(queryStructCmd);
            queryStatement.setIsS2SQL(false);
            queryStatement = headlessQueryEngine.plan(queryStatement);
            queryUtils.checkSqlParse(queryStatement);
            sqlParsers.add(queryStatement);
        }
        log.info("multi sqlParser:{}", sqlParsers);
        return queryUtils.sqlParserUnion(queryMultiStructReq, sqlParsers);
    }

    @Override
    @SneakyThrows
    public QueryResultWithSchemaResp queryDimValue(QueryDimValueReq queryDimValueReq, User user) {
        QueryS2SQLReq queryS2SQLReq = generateDimValueQuerySql(queryDimValueReq);
        return (QueryResultWithSchemaResp) queryBySql(queryS2SQLReq, user);
    }

    private void handleGlobalCacheDisable(QueryStructReq queryStructCmd) {
        if (!cacheEnable) {
            Cache cacheInfo = new Cache();
            cacheInfo.setCache(false);
            queryStructCmd.setCacheInfo(cacheInfo);
        }
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

        if (QueryType.SQL.equals(queryTypeEnum) && queryReq instanceof QueryS2SQLReq) {
            QueryStatement queryStatement = convertToQueryStatement((QueryS2SQLReq) queryReq, user);
            return getExplainResp(queryStatement);
        }
        if (QueryType.STRUCT.equals(queryTypeEnum) && queryReq instanceof QueryStructReq) {
            QueryStatement queryStatement = new QueryStatement();
            queryStatement.setQueryStructReq((QueryStructReq) queryReq);
            queryStatement.setIsS2SQL(false);
            queryStatement = headlessQueryEngine.plan(queryStatement);
            return getExplainResp(queryStatement);
        }
        if (QueryType.STRUCT.equals(queryTypeEnum) && queryReq instanceof QueryMultiStructReq) {
            QueryMultiStructReq queryMultiStructReq = (QueryMultiStructReq) queryReq;
            QueryStatement queryStatement = getQueryStatementByMultiStruct(queryMultiStructReq);
            return getExplainResp(queryStatement);
        }

        throw new IllegalArgumentException("Parameters are invalid, explainSqlReq: " + explainSqlReq);
    }

    @Override
    @ApiHeaderCheck
    public ItemQueryResultResp metricDataQueryById(QueryItemReq queryItemReq,
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
        List<Item> items = item.getRelateItems();
        List<DimensionResp> dimensionResps = Lists.newArrayList();
        if (!org.springframework.util.CollectionUtils.isEmpty(items)) {
            List<Long> ids = items.stream().map(Item::getId).collect(Collectors.toList());
            DimensionFilter dimensionFilter = new DimensionFilter();
            dimensionFilter.setIds(ids);
            dimensionResps = catalog.getDimensions(dimensionFilter);
        }
        QueryStructReq queryStructReq = buildQueryStructReq(dimensionResps, metricResp, dateConf, limit);
        QueryResultWithSchemaResp queryResultWithSchemaResp =
                queryByStruct(queryStructReq, User.getAppUser(appId));
        SingleItemQueryResult apiQuerySingleResult = new SingleItemQueryResult();
        apiQuerySingleResult.setItem(item);
        apiQuerySingleResult.setResult(queryResultWithSchemaResp);
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

    public QueryStatement parseMetricReq(MetricReq metricReq) throws Exception {
        QueryStructReq queryStructCmd = new QueryStructReq();
        return headlessQueryEngine.physicalSql(queryStructCmd, metricReq);
    }

    private boolean isCache(QueryStructReq queryStructCmd) {
        if (!cacheEnable) {
            return false;
        }
        if (queryStructCmd.getCacheInfo() != null) {
            return queryStructCmd.getCacheInfo().getCache();
        }
        return false;
    }

    private boolean isCache(QueryMultiStructReq queryStructCmd) {
        if (!cacheEnable) {
            return false;
        }
        if (!CollectionUtils.isEmpty(queryStructCmd.getQueryStructReqs())
                && queryStructCmd.getQueryStructReqs().get(0).getCacheInfo() != null) {
            return queryStructCmd.getQueryStructReqs().get(0).getCacheInfo().getCache();
        }
        return false;
    }

    private QueryResultWithSchemaResp queryByCache(String key, Object queryCmd) {

        Object resultObject = cacheUtils.get(key);
        if (Objects.nonNull(resultObject)) {
            log.info("queryByStructWithCache, key:{}, queryCmd:{}", key, queryCmd.toString());
            statUtils.updateResultCacheKey(key);
            return (QueryResultWithSchemaResp) resultObject;
        }
        return null;
    }

    private QueryS2SQLReq generateDimValueQuerySql(QueryDimValueReq queryDimValueReq) {
        QueryS2SQLReq queryS2SQLReq = new QueryS2SQLReq();
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
        queryS2SQLReq.setModelIds(Sets.newHashSet(queryDimValueReq.getModelId()));
        queryS2SQLReq.setSql(sql);
        return queryS2SQLReq;
    }

    private String getKeyByModelIds(List<Long> modelIds) {
        return String.join(",", modelIds.stream()
                .map(Object::toString).collect(Collectors.toList()));
    }

}
