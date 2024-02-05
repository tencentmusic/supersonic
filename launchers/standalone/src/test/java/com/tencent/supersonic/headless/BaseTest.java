package com.tencent.supersonic.headless;

import com.tencent.supersonic.BaseApplication;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.service.QueryService;
import com.tencent.supersonic.util.DataUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.time.LocalDate.now;

public class BaseTest extends BaseApplication {

    @Autowired
    protected QueryService queryService;

    protected SemanticQueryResp queryBySql(String sql) throws Exception {
        return queryBySql(sql, User.getFakeUser());
    }

    protected SemanticQueryResp queryBySql(String sql, User user) throws Exception {
        return queryService.queryByReq(buildQuerySqlReq(sql), user);
    }

    protected SemanticQueryReq buildQuerySqlReq(String sql) {
        QuerySqlReq querySqlCmd = new QuerySqlReq();
        querySqlCmd.setSql(sql);
        querySqlCmd.setModelIds(DataUtils.getMetricAgentIModelIds());
        return querySqlCmd;
    }

    protected QueryStructReq buildQueryStructReq(List<String> groups) {
        return buildQueryStructReq(groups, QueryType.METRIC);
    }

    protected QueryStructReq buildQueryStructReq(List<String> groups, QueryType queryType) {
        QueryStructReq queryStructReq = new QueryStructReq();
        for (Long modelId : DataUtils.getMetricAgentIModelIds()) {
            queryStructReq.addModelId(modelId);
        }
        queryStructReq.setQueryType(queryType);
        Aggregator aggregator = new Aggregator();
        aggregator.setFunc(AggOperatorEnum.SUM);
        aggregator.setColumn("pv");
        queryStructReq.setAggregators(Arrays.asList(aggregator));

        if (CollectionUtils.isNotEmpty(groups)) {
            queryStructReq.setGroups(groups);
        }

        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateMode.BETWEEN);
        dateConf.setEndDate(now().plusDays(0).toString());
        dateConf.setStartDate(now().plusDays(-365).toString());
        queryStructReq.setDateInfo(dateConf);

        List<Order> orders = new ArrayList<>();
        Order order = new Order();
        order.setColumn("pv");
        orders.add(order);
        queryStructReq.setOrders(orders);
        return queryStructReq;
    }

    protected QueryStructReq buildQueryStructReq(List<String> groups,
                                                 Aggregator aggregator) {
        QueryStructReq queryStructReq = new QueryStructReq();
        for (Long modelId : DataUtils.getMetricAgentIModelIds()) {
            queryStructReq.addModelId(modelId);
        }
        queryStructReq.setQueryType(QueryType.METRIC);
        queryStructReq.setAggregators(Arrays.asList(aggregator));

        if (CollectionUtils.isNotEmpty(groups)) {
            queryStructReq.setGroups(groups);
        }

        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateMode.BETWEEN);
        dateConf.setEndDate(now().plusDays(0).toString());
        dateConf.setStartDate(now().plusDays(-365).toString());
        queryStructReq.setDateInfo(dateConf);
        return queryStructReq;
    }

}
