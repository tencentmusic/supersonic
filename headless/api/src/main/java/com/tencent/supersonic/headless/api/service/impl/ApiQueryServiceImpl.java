package com.tencent.supersonic.headless.api.service.impl;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.ApiItemType;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.headless.api.aspect.ApiHeaderCheckAspect;
import com.tencent.supersonic.headless.api.service.ApiQueryService;
import com.tencent.supersonic.headless.api.service.AppService;
import com.tencent.supersonic.headless.common.model.pojo.Item;
import com.tencent.supersonic.headless.common.model.response.AppDetailResp;
import com.tencent.supersonic.headless.common.model.response.DimensionResp;
import com.tencent.supersonic.headless.common.model.response.MetricResp;
import com.tencent.supersonic.headless.common.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.common.query.pojo.ApiQuerySingleResult;
import com.tencent.supersonic.headless.common.query.request.MetaQueryApiReq;
import com.tencent.supersonic.headless.common.query.request.QueryApiReq;
import com.tencent.supersonic.headless.common.query.request.QueryStructReq;
import com.tencent.supersonic.headless.common.query.response.ApiQueryResultResp;
import com.tencent.supersonic.headless.model.domain.DimensionService;
import com.tencent.supersonic.headless.model.domain.MetricService;
import com.tencent.supersonic.headless.model.domain.pojo.DimensionFilter;
import com.tencent.supersonic.headless.model.domain.pojo.MetaFilter;
import com.tencent.supersonic.headless.api.annotation.ApiHeaderCheck;
import com.tencent.supersonic.headless.query.service.QueryService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class ApiQueryServiceImpl implements ApiQueryService {

    private static final long result_size = 10000;

    private AppService appService;

    private MetricService metricService;

    private DimensionService dimensionService;

    private QueryService queryService;

    public ApiQueryServiceImpl(AppService appService,
                               MetricService metricService,
                               DimensionService dimensionService,
                               QueryService queryService) {
        this.appService = appService;
        this.metricService = metricService;
        this.dimensionService = dimensionService;
        this.queryService = queryService;
    }

    @Override
    @ApiHeaderCheck
    public ApiQueryResultResp metricDataQueryById(QueryApiReq queryApiReq,
                                                  HttpServletRequest request) throws Exception {
        AppDetailResp appDetailResp = getAppDetailResp(request);
        authCheck(appDetailResp, queryApiReq.getIds(), ApiItemType.METRIC);
        List<ApiQuerySingleResult> results = Lists.newArrayList();
        Map<Long, Item> map = appDetailResp.getConfig().getItems().stream()
                .collect(Collectors.toMap(Item::getId, i -> i));
        for (Long id : queryApiReq.getIds()) {
            Item item = map.get(id);
            ApiQuerySingleResult apiQuerySingleResult = dataQuery(appDetailResp.getId(),
                    item, queryApiReq.getDateConf());
            results.add(apiQuerySingleResult);
        }
        return ApiQueryResultResp.builder().results(results).build();
    }

    @Override
    @ApiHeaderCheck
    public QueryResultWithSchemaResp dataQueryByStruct(QueryStructReq queryStructReq,
                                                       HttpServletRequest request) throws Exception {
        AppDetailResp appDetailResp = getAppDetailResp(request);
        structAuthCheck(appDetailResp, queryStructReq);
        return queryService.queryByStruct(queryStructReq, User.getAppUser(appDetailResp.getId()));
    }

    @Override
    @ApiHeaderCheck
    public Object metaQuery(MetaQueryApiReq metaQueryApiReq, HttpServletRequest request) {
        AppDetailResp appDetailResp = getAppDetailResp(request);
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setIds(metaQueryApiReq.getIds());
        if (ApiItemType.METRIC.equals(metaQueryApiReq.getType())) {
            authCheck(appDetailResp, metaQueryApiReq.getIds(), ApiItemType.METRIC);
            return metricService.getMetrics(metaFilter);
        } else if (ApiItemType.DIMENSION.equals(metaQueryApiReq.getType())) {
            authCheck(appDetailResp, metaQueryApiReq.getIds(), ApiItemType.DIMENSION);
            return dimensionService.getDimensions(metaFilter);
        } else if (ApiItemType.TAG.equals(metaQueryApiReq.getType())) {
            throw new InvalidArgumentException("标签元数据类型正在支持中");
        }
        throw new InvalidArgumentException("不支持的元数据类型:" + metaQueryApiReq.getType());
    }

    private ApiQuerySingleResult dataQuery(Integer appId, Item item, DateConf dateConf) throws Exception {
        MetricResp metricResp = metricService.getMetric(item.getId());
        List<Item> items = item.getRelateItems();
        List<DimensionResp> dimensionResps = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(items)) {
            List<Long> ids = items.stream().map(Item::getId).collect(Collectors.toList());
            DimensionFilter dimensionFilter = new DimensionFilter();
            dimensionFilter.setIds(ids);
            dimensionResps = dimensionService.getDimensions(dimensionFilter);
        }
        QueryStructReq queryStructReq = buildQueryStructReq(dimensionResps, metricResp, dateConf);
        QueryResultWithSchemaResp queryResultWithSchemaResp =
                queryService.queryByStruct(queryStructReq, User.getAppUser(appId));
        ApiQuerySingleResult apiQuerySingleResult = new ApiQuerySingleResult();
        apiQuerySingleResult.setItem(item);
        apiQuerySingleResult.setResult(queryResultWithSchemaResp);
        return apiQuerySingleResult;
    }

    private AppDetailResp getAppDetailResp(HttpServletRequest request) {
        int appId = Integer.parseInt(request.getHeader(ApiHeaderCheckAspect.APPID));
        return appService.getApp(appId);
    }

    private void authCheck(AppDetailResp appDetailResp, List<Long> ids, ApiItemType type) {
        Set<Long> idsInApp = appDetailResp.getConfig().getAllItems().stream()
                .filter(item -> type.equals(item.getType())).map(Item::getId).collect(Collectors.toSet());
        if (!idsInApp.containsAll(ids)) {
            throw new InvalidArgumentException("查询范围超过应用申请范围, 请检查");
        }
    }

    private void structAuthCheck(AppDetailResp appDetailResp, QueryStructReq queryStructReq) {
        List<Long> metricIdsToQuery = metricService.getMetrics(new MetaFilter(queryStructReq.getModelIds()))
                .stream().filter(metricResp -> queryStructReq.getMetrics().contains(metricResp.getBizName()))
                .map(MetricResp::getId).collect(Collectors.toList());
        List<Long> dimensionIdsToQuery = dimensionService.getDimensions(new MetaFilter(queryStructReq.getModelIds()))
                .stream().filter(dimensionResp -> queryStructReq.getGroups().contains(dimensionResp.getBizName()))
                .map(DimensionResp::getId).collect(Collectors.toList());
        authCheck(appDetailResp, metricIdsToQuery, ApiItemType.METRIC);
        authCheck(appDetailResp, dimensionIdsToQuery, ApiItemType.DIMENSION);
    }

    private QueryStructReq buildQueryStructReq(List<DimensionResp> dimensionResps,
                                               MetricResp metricResp, DateConf dateConf) {
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
        queryStructReq.setLimit(result_size);
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

}
