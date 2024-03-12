package com.tencent.supersonic.headless.server.service.impl;

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
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.Item;
import com.tencent.supersonic.headless.api.pojo.QueryParam;
import com.tencent.supersonic.headless.api.pojo.SingleItemQueryResult;
import com.tencent.supersonic.headless.api.pojo.request.ExplainSqlReq;
import com.tencent.supersonic.headless.api.pojo.request.ItemUseReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryDimValueReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryItemReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryTagReq;
import com.tencent.supersonic.headless.api.pojo.request.SchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.AppDetailResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.ExplainResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemQueryResultResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemUseResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.cache.QueryCache;
import com.tencent.supersonic.headless.core.executor.QueryExecutor;
import com.tencent.supersonic.headless.core.parser.DefaultQueryParser;
import com.tencent.supersonic.headless.core.parser.QueryParser;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.SemanticModel;
import com.tencent.supersonic.headless.core.planner.QueryPlanner;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.server.annotation.S2DataPermission;
import com.tencent.supersonic.headless.server.aspect.ApiHeaderCheckAspect;
import com.tencent.supersonic.headless.server.manager.SemanticSchemaManager;
import com.tencent.supersonic.headless.server.pojo.DimensionFilter;
import com.tencent.supersonic.headless.server.service.AppService;
import com.tencent.supersonic.headless.server.service.Catalog;
import com.tencent.supersonic.headless.server.service.QueryService;
import com.tencent.supersonic.headless.server.utils.QueryReqConverter;
import com.tencent.supersonic.headless.server.utils.QueryUtils;
import com.tencent.supersonic.headless.server.utils.StatUtils;
import com.tencent.supersonic.headless.server.utils.TagConverter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Slf4j
public class QueryServiceImpl implements QueryService {

    private StatUtils statUtils;
    private final QueryUtils queryUtils;
    private final QueryReqConverter queryReqConverter;
    private final TagConverter tagConverter;
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
            TagConverter tagConverter, Catalog catalog,
            AppService appService,
            QueryCache queryCache,
            SemanticSchemaManager semanticSchemaManager,
            DefaultQueryParser queryParser,
            QueryPlanner queryPlanner) {
        this.statUtils = statUtils;
        this.queryUtils = queryUtils;
        this.queryReqConverter = queryReqConverter;
        this.tagConverter = tagConverter;
        this.catalog = catalog;
        this.appService = appService;
        this.queryCache = queryCache;
        this.semanticSchemaManager = semanticSchemaManager;
        this.queryParser = queryParser;
        this.queryPlanner = queryPlanner;
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
            String cacheKey = queryCache.getCacheKey(queryReq);
            Object query = queryCache.query(queryReq, cacheKey);
            if (Objects.nonNull(query)) {
                return (SemanticQueryResp) query;
            }
            StatUtils.get().setUseResultCache(false);
            //3 query
            QueryStatement queryStatement = buildQueryStatement(queryReq);
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

    private QueryStatement buildSqlQueryStatement(QuerySqlReq querySqlReq) throws Exception {
        SchemaFilterReq filter = buildSchemaFilterReq(querySqlReq);
        SemanticSchemaResp semanticSchemaResp = catalog.fetchSemanticSchema(filter);
        QueryStatement queryStatement = queryReqConverter.convert(querySqlReq, semanticSchemaResp);
        queryStatement.setModelIds(querySqlReq.getModelIds());
        queryStatement.setEnableOptimize(queryUtils.enableOptimize());
        queryStatement.setSemanticSchemaResp(semanticSchemaResp);
        SemanticModel semanticModel = semanticSchemaManager.getSemanticModel(semanticSchemaResp);
        queryStatement.setSemanticModel(semanticModel);
        return queryStatement;
    }

    private QueryStatement buildQueryStatement(SemanticQueryReq semanticQueryReq) throws Exception {
        if (semanticQueryReq instanceof QuerySqlReq) {
            return buildSqlQueryStatement((QuerySqlReq) semanticQueryReq);
        }
        if (semanticQueryReq instanceof QueryStructReq) {
            return buildStructQueryStatement((QueryStructReq) semanticQueryReq);
        }
        if (semanticQueryReq instanceof QueryMultiStructReq) {
            return buildMultiStructQueryStatement((QueryMultiStructReq) semanticQueryReq);
        }
        if (semanticQueryReq instanceof QueryTagReq) {
            return buildTagQueryStatement((QueryTagReq) semanticQueryReq);
        }
        return null;
    }

    private QueryStatement buildStructQueryStatement(QueryStructReq queryStructReq) throws Exception {
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
        SemanticModel semanticModel = semanticSchemaManager.getSemanticModel(semanticSchemaResp);
        queryStatement.setSemanticModel(semanticModel);
        return queryStatement;
    }

    private QueryStatement buildMultiStructQueryStatement(QueryMultiStructReq queryMultiStructReq)
            throws Exception {
        List<QueryStatement> sqlParsers = new ArrayList<>();
        for (QueryStructReq queryStructReq : queryMultiStructReq.getQueryStructReqs()) {
            QueryStatement queryStatement = buildQueryStatement(queryStructReq);
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

    private QueryStatement buildTagQueryStatement(QueryTagReq queryTagReq)
            throws Exception {
        SchemaFilterReq schemaFilterReq = new SchemaFilterReq();
        SchemaFilterReq filter = buildSchemaFilterReq(queryTagReq);
        schemaFilterReq.setModelIds(queryTagReq.getModelIds());
        SemanticSchemaResp semanticSchemaResp = catalog.fetchSemanticSchema(filter);
        QueryStatement queryStatement = tagConverter.convert(queryTagReq, semanticSchemaResp);
        queryStatement.setModelIds(queryTagReq.getModelIds());
        queryStatement.setEnableOptimize(queryUtils.enableOptimize());
        queryStatement.setSemanticSchemaResp(semanticSchemaResp);
        SemanticModel semanticModel = semanticSchemaManager.getTagSemanticModel(semanticSchemaResp);
        queryStatement.setSemanticModel(semanticModel);
        return queryStatement;
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
        QueryStatement queryStatement = buildQueryStatement((SemanticQueryReq) queryReq);
        queryStatement = plan(queryStatement);
        return getExplainResp(queryStatement);
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

    private SingleItemQueryResult dataQuery(Integer appId, Item item, DateConf dateConf, Long limit) throws Exception {
        MetricResp metricResp = catalog.getMetric(item.getId());
        item.setCreatedBy(metricResp.getCreatedBy());
        item.setBizName(metricResp.getBizName());
        item.setName(metricResp.getName());
        List<Item> items = item.getRelateItems();
        List<DimensionResp> dimensionResps = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(items)) {
            List<Long> ids = items.stream().map(Item::getId).collect(Collectors.toList());
            DimensionFilter dimensionFilter = new DimensionFilter();
            dimensionFilter.setIds(ids);
            dimensionResps = catalog.getDimensions(dimensionFilter);
        }
        QueryStructReq queryStructReq = buildQueryStructReq(dimensionResps, metricResp, dateConf, limit);
        SemanticQueryResp semanticQueryResp = queryByReq(queryStructReq, User.getAppUser(appId));
        SingleItemQueryResult apiQuerySingleResult = new SingleItemQueryResult();
        apiQuerySingleResult.setItem(item);
        apiQuerySingleResult.setResult(semanticQueryResp);
        return apiQuerySingleResult;
    }

    private AppDetailResp getAppDetailResp(HttpServletRequest request) {
        int appId = Integer.parseInt(request.getHeader(ApiHeaderCheckAspect.APPID));
        return appService.getApp(appId);
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
        Set<Long> idsInApp = appDetailResp.allItems().stream()
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
            //3 execute
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
