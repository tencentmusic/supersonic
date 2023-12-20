package com.tencent.supersonic.headless.query.service;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.headless.api.model.pojo.Item;
import com.tencent.supersonic.headless.api.model.response.AppDetailResp;
import com.tencent.supersonic.headless.api.model.response.DimensionResp;
import com.tencent.supersonic.headless.api.model.response.MetricResp;
import com.tencent.supersonic.headless.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.api.query.pojo.ApiQuerySingleResult;
import com.tencent.supersonic.headless.api.query.request.QueryApiPreviewReq;
import com.tencent.supersonic.headless.api.query.request.QueryApiReq;
import com.tencent.supersonic.headless.api.query.request.QueryStructReq;
import com.tencent.supersonic.headless.api.query.response.ApiQueryResultResp;
import com.tencent.supersonic.headless.model.domain.AppService;
import com.tencent.supersonic.headless.model.domain.DimensionService;
import com.tencent.supersonic.headless.model.domain.MetricService;
import com.tencent.supersonic.headless.model.domain.pojo.DimensionFilter;
import com.tencent.supersonic.headless.query.annotation.ApiDataPermission;
import com.tencent.supersonic.headless.query.aspect.ApiDataAspect;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Api service for other apps
 * The current version defaults to query metrics data.
 */
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
    public ApiQueryResultResp preview(QueryApiPreviewReq queryApiReq) throws Exception {
        Item item = queryApiReq.getItem();
        ApiQuerySingleResult apiQuerySingleResult = query(item, queryApiReq.getDateConf());
        return ApiQueryResultResp.builder().results(Lists.newArrayList(apiQuerySingleResult)).build();
    }

    public ApiQuerySingleResult query(Item item, DateConf dateConf) throws Exception {
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
                queryService.queryByStruct(queryStructReq, User.getAppUser(0));
        ApiQuerySingleResult apiQuerySingleResult = new ApiQuerySingleResult();
        apiQuerySingleResult.setItem(item);
        apiQuerySingleResult.setResult(queryResultWithSchemaResp);
        return apiQuerySingleResult;
    }

    @Override
    @ApiDataPermission
    public ApiQueryResultResp query(QueryApiReq queryApiReq, HttpServletRequest request) throws Exception {
        int appId = Integer.parseInt(request.getHeader(ApiDataAspect.APPID));
        AppDetailResp appDetailResp = appService.getApp(appId);
        Set<Long> idsInApp = appDetailResp.getConfig().getItems().stream()
                .map(Item::getId).collect(Collectors.toSet());
        if (!idsInApp.containsAll(queryApiReq.getIds())) {
            throw new InvalidArgumentException("查询范围超过应用申请范围, 请检查");
        }
        List<ApiQuerySingleResult> results = Lists.newArrayList();
        Map<Long, Item> map = appDetailResp.getConfig().getItems().stream()
                .collect(Collectors.toMap(Item::getId, i -> i));
        for (Long id : queryApiReq.getIds()) {
            Item item = map.get(id);
            ApiQuerySingleResult apiQuerySingleResult = query(item, queryApiReq.getDateConf());
            results.add(apiQuerySingleResult);
        }
        return ApiQueryResultResp.builder().results(results).build();
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
