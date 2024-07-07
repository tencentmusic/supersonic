package com.tencent.supersonic.headless;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.QueryMetricReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.web.service.MetricService;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

import static org.junit.Assert.assertThrows;

public class QueryByMetricTest extends BaseTest {

    @Autowired
    protected MetricService metricService;

    @Test
    public void testWithMetricAndDimensionBizNames() throws Exception {
        QueryMetricReq queryMetricReq = new QueryMetricReq();
        queryMetricReq.setMetricNames(Arrays.asList("stay_hours", "pv"));
        queryMetricReq.setDimensionNames(Arrays.asList("user_name", "department"));
        SemanticQueryResp queryResp = queryByMetric(queryMetricReq, User.getFakeUser());
        Assert.assertNotNull(queryResp.getResultList());
        Assert.assertEquals(6, queryResp.getResultList().size());
    }

    @Test
    public void testWithMetricAndDimensionNames() throws Exception {
        QueryMetricReq queryMetricReq = new QueryMetricReq();
        queryMetricReq.setMetricNames(Arrays.asList("停留时长", "访问次数"));
        queryMetricReq.setDimensionNames(Arrays.asList("用户", "部门"));
        SemanticQueryResp queryResp = queryByMetric(queryMetricReq, User.getFakeUser());
        Assert.assertNotNull(queryResp.getResultList());
        Assert.assertEquals(6, queryResp.getResultList().size());
    }

    @Test
    public void testWithDomainId() throws Exception {
        QueryMetricReq queryMetricReq = new QueryMetricReq();
        queryMetricReq.setDomainId(1L);
        queryMetricReq.setMetricNames(Arrays.asList("stay_hours", "pv"));
        queryMetricReq.setDimensionNames(Arrays.asList("user_name", "department"));
        SemanticQueryResp queryResp = queryByMetric(queryMetricReq, User.getFakeUser());
        Assert.assertNotNull(queryResp.getResultList());
        Assert.assertEquals(6, queryResp.getResultList().size());

        queryMetricReq.setDomainId(2L);
        queryMetricReq.setMetricNames(Arrays.asList("stay_hours", "pv"));
        queryMetricReq.setDimensionNames(Arrays.asList("user_name", "department"));
        assertThrows(IllegalArgumentException.class,
                () -> queryByMetric(queryMetricReq, User.getFakeUser()));
    }

    @Test
    public void testWithMetricAndDimensionIds() throws Exception {
        QueryMetricReq queryMetricReq = new QueryMetricReq();
        queryMetricReq.setDomainId(1L);
        queryMetricReq.setMetricIds(Arrays.asList(1L, 3L));
        queryMetricReq.setDimensionIds(Arrays.asList(1L, 2L));
        SemanticQueryResp queryResp = queryByMetric(queryMetricReq, User.getFakeUser());
        Assert.assertNotNull(queryResp.getResultList());
        Assert.assertEquals(6, queryResp.getResultList().size());
    }

    private SemanticQueryResp queryByMetric(QueryMetricReq queryMetricReq, User user) throws Exception {
        QueryStructReq convert = metricService.convert(queryMetricReq);
        return semanticLayerService.queryByReq(convert.convert(), user);
    }
}
