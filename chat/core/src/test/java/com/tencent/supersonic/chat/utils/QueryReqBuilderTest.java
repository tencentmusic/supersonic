package com.tencent.supersonic.chat.utils;


import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateModeUtils;
import com.tencent.supersonic.semantic.api.query.request.QueryS2QLReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.jsqlparser.JSQLParserException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * QueryReqBuilderTest
 */
class QueryReqBuilderTest {

    @Test
    void buildS2QLReq() throws JSQLParserException {
        init();
        QueryStructReq queryStructReq = new QueryStructReq();
        queryStructReq.setModelId(1L);
        queryStructReq.setNativeQuery(false);

        Aggregator aggregator = new Aggregator();
        aggregator.setFunc(AggOperatorEnum.UNKNOWN);
        aggregator.setColumn("pv");
        queryStructReq.setAggregators(Arrays.asList(aggregator));

        queryStructReq.setGroups(Arrays.asList("department"));

        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateMode.LIST);
        dateConf.setDateList(Arrays.asList("2023-08-01"));
        queryStructReq.setDateInfo(dateConf);

        List<Order> orders = new ArrayList<>();
        Order order = new Order();
        order.setColumn("uv");
        orders.add(order);
        queryStructReq.setOrders(orders);

        QueryS2QLReq queryS2QLReq = QueryReqBuilder.buildS2QLReq(queryStructReq);
        Assert.assertEquals(
                "SELECT department, SUM(pv) FROM t_1 WHERE (sys_imp_date IN ('2023-08-01')) "
                        + "GROUP BY department ORDER BY uv LIMIT 2000", queryS2QLReq.getSql());

        queryStructReq.setNativeQuery(true);
        queryS2QLReq = QueryReqBuilder.buildS2QLReq(queryStructReq);
        Assert.assertEquals(
                "SELECT department, pv FROM t_1 WHERE (sys_imp_date IN ('2023-08-01')) "
                        + "ORDER BY uv LIMIT 2000",
                queryS2QLReq.getSql());

    }

    private void init() {
        MockedStatic<ContextUtils> mockContextUtils = Mockito.mockStatic(ContextUtils.class);
        SqlFilterUtils sqlFilterUtils = new SqlFilterUtils();
        mockContextUtils.when(() -> ContextUtils.getBean(SqlFilterUtils.class)).thenReturn(sqlFilterUtils);
        DateModeUtils dateModeUtils = new DateModeUtils();
        mockContextUtils.when(() -> ContextUtils.getBean(DateModeUtils.class)).thenReturn(dateModeUtils);
        dateModeUtils.setSysDateCol("sys_imp_date");
        dateModeUtils.setSysDateWeekCol("sys_imp_week");
        dateModeUtils.setSysDateMonthCol("sys_imp_month");
    }
}