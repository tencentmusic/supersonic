package com.tencent.supersonic.headless.integration;

import static java.time.LocalDate.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;

public class QueryByStructTest extends BaseTest {

    @Test
    public void testSumQuery() throws Exception {
        QueryStructReq queryStructReq = buildQueryStructReq(null);
        SemanticQueryResp semanticQueryResp = queryByReq(queryStructReq, User.getFakeUser());
        assertEquals(1, semanticQueryResp.getColumns().size());
        QueryColumn queryColumn = semanticQueryResp.getColumns().get(0);
        assertEquals("访问次数", queryColumn.getName());
        assertEquals(1, semanticQueryResp.getResultList().size());
    }

    @Test
    public void testGroupByQuery() throws Exception {
        QueryStructReq queryStructReq = buildQueryStructReq(Arrays.asList("department"));
        SemanticQueryResp result = queryByReq(queryStructReq, User.getFakeUser());
        assertEquals(2, result.getColumns().size());
        QueryColumn firstColumn = result.getColumns().get(0);
        QueryColumn secondColumn = result.getColumns().get(1);
        assertEquals("部门", firstColumn.getName());
        assertEquals("访问次数", secondColumn.getName());
        assertNotNull(result.getResultList().size());
    }

    @Test
    public void testCacheQuery() throws Exception {
        QueryStructReq queryStructReq1 = buildQueryStructReq(Arrays.asList("department"));
        QueryStructReq queryStructReq2 = buildQueryStructReq(Arrays.asList("department"));
        SemanticQueryResp result1 = queryByReq(queryStructReq1, User.getFakeUser());
        SemanticQueryResp result2 = queryByReq(queryStructReq2, User.getFakeUser());
        assertEquals(result1, result2);
    }

    private QueryStructReq buildQueryStructReq(List<String> groups) {
        QueryStructReq queryStructReq = new QueryStructReq();
        queryStructReq.addModelId(1L);
        queryStructReq.addModelId(2L);
        queryStructReq.addModelId(3L);

        queryStructReq.setQueryType(QueryType.METRIC);
        Aggregator aggregator = new Aggregator();
        aggregator.setFunc(AggOperatorEnum.SUM);
        aggregator.setColumn("pv");
        queryStructReq.setAggregators(Arrays.asList(aggregator));

        if (CollectionUtils.isNotEmpty(groups)) {
            queryStructReq.setGroups(groups);
            queryStructReq.setGroups(Arrays.asList("department"));
        }

        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateMode.BETWEEN);
        dateConf.setStartDate(now().plusDays(-1).toString());
        dateConf.setEndDate(now().plusDays(-10).toString());
        queryStructReq.setDateInfo(dateConf);

        List<Order> orders = new ArrayList<>();
        Order order = new Order();
        order.setColumn("pv");
        orders.add(order);
        queryStructReq.setOrders(orders);
        return queryStructReq;
    }
}
