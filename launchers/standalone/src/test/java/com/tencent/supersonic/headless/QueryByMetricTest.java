package com.tencent.supersonic.headless;

import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.request.QueryMetricReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.service.MetricService;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.Assert.assertThrows;

public class QueryByMetricTest extends BaseTest {

    @Autowired
    protected MetricService metricService;

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testWithMetricAndDimensionNames() throws Exception {
        QueryMetricReq queryMetricReq = new QueryMetricReq();
        queryMetricReq.setMetricNames(Arrays.asList("停留时长", "访问次数"));
        queryMetricReq.setDimensionNames(Arrays.asList("用户名", "部门"));
        queryMetricReq.getFilters()
                .add(Filter.builder().name("数据日期").operator(FilterOperatorEnum.MINOR_THAN_EQUALS)
                        .relation(Filter.Relation.FILTER).value(LocalDate.now().toString())
                        .build());
        SemanticQueryResp queryResp = queryByMetric(queryMetricReq, User.getDefaultUser());
        Assert.assertNotNull(queryResp.getResultList());
        Assert.assertEquals(6, queryResp.getResultList().size());
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testWithDomainId() throws Exception {
        QueryMetricReq queryMetricReq = new QueryMetricReq();
        queryMetricReq.setDomainId(1L);
        queryMetricReq.setMetricNames(Arrays.asList("停留时长", "访问次数"));
        queryMetricReq.setDimensionNames(Arrays.asList("用户名", "部门"));
        queryMetricReq.getFilters()
                .add(Filter.builder().name("数据日期").operator(FilterOperatorEnum.MINOR_THAN_EQUALS)
                        .relation(Filter.Relation.FILTER).value(LocalDate.now().toString())
                        .build());
        SemanticQueryResp queryResp = queryByMetric(queryMetricReq, User.getDefaultUser());
        Assert.assertNotNull(queryResp.getResultList());
        Assert.assertEquals(6, queryResp.getResultList().size());

        queryMetricReq.setDomainId(2L);
        queryMetricReq.setMetricNames(Arrays.asList("停留时长", "访问次数"));
        queryMetricReq.setDimensionNames(Arrays.asList("用户名", "部门"));
        assertThrows(IllegalArgumentException.class,
                () -> queryByMetric(queryMetricReq, User.getDefaultUser()));
    }

    @Test
    public void testWithMetricAndDimensionIds() throws Exception {
        QueryMetricReq queryMetricReq = new QueryMetricReq();
        queryMetricReq.setDomainId(1L);
        queryMetricReq.setMetricIds(Arrays.asList(1L, 3L));
        queryMetricReq.setDimensionIds(Arrays.asList(1L, 2L));
        queryMetricReq.getFilters()
                .add(Filter.builder().name("数据日期").operator(FilterOperatorEnum.MINOR_THAN_EQUALS)
                        .relation(Filter.Relation.FILTER).value(LocalDate.now().toString())
                        .build());
        SemanticQueryResp queryResp = queryByMetric(queryMetricReq, User.getDefaultUser());
        Assert.assertNotNull(queryResp.getResultList());
        Assert.assertEquals(6, queryResp.getResultList().size());
    }

    private SemanticQueryResp queryByMetric(QueryMetricReq queryMetricReq, User user)
            throws Exception {
        QueryStructReq convert = metricService.convert(queryMetricReq);
        return semanticLayerService.queryByReq(convert.convert(), user);
    }
}
