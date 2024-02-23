package com.tencent.supersonic.headless;

import static org.junit.Assert.assertThrows;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.QueryMetricReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class QueryByMetricTest extends BaseTest {

    @Test
    public void testWithMetricAndDimensionBizNames() throws Exception {
        QueryMetricReq queryMetricReq = new QueryMetricReq();
        queryMetricReq.setMetricNames(Arrays.asList("stay_hours", "pv"));
        queryMetricReq.setDimensionNames(Arrays.asList("user_name", "department"));
        SemanticQueryResp queryResp = queryService.queryByMetric(queryMetricReq, User.getFakeUser());
        Assert.assertNotNull(queryResp.getResultList());
        Assert.assertEquals(6, queryResp.getResultList().size());
    }

    @Test
    public void testWithMetricAndDimensionNames() throws Exception {
        QueryMetricReq queryMetricReq = new QueryMetricReq();
        queryMetricReq.setMetricNames(Arrays.asList("停留时长", "访问次数"));
        queryMetricReq.setDimensionNames(Arrays.asList("用户", "部门"));
        SemanticQueryResp queryResp = queryService.queryByMetric(queryMetricReq, User.getFakeUser());
        Assert.assertNotNull(queryResp.getResultList());
        Assert.assertEquals(6, queryResp.getResultList().size());
    }

    @Test
    public void testWithDomainId() throws Exception {
        QueryMetricReq queryMetricReq = new QueryMetricReq();
        queryMetricReq.setDomainId(1L);
        queryMetricReq.setMetricNames(Arrays.asList("stay_hours", "pv"));
        queryMetricReq.setDimensionNames(Arrays.asList("user_name", "department"));
        SemanticQueryResp queryResp = queryService.queryByMetric(queryMetricReq, User.getFakeUser());
        Assert.assertNotNull(queryResp.getResultList());
        Assert.assertEquals(6, queryResp.getResultList().size());

        queryMetricReq.setDomainId(2L);
        queryMetricReq.setMetricNames(Arrays.asList("stay_hours", "pv"));
        queryMetricReq.setDimensionNames(Arrays.asList("user_name", "department"));
        assertThrows(IllegalArgumentException.class,
                () -> queryService.queryByMetric(queryMetricReq, User.getFakeUser()));
    }

    @Test
    public void testWithMetricAndDimensionIds() throws Exception {
        QueryMetricReq queryMetricReq = new QueryMetricReq();
        queryMetricReq.setDomainId(1L);
        queryMetricReq.setMetricIds(Arrays.asList(1L, 4L));
        queryMetricReq.setDimensionIds(Arrays.asList(1L, 2L));
        SemanticQueryResp queryResp = queryService.queryByMetric(queryMetricReq, User.getFakeUser());
        Assert.assertNotNull(queryResp.getResultList());
        Assert.assertEquals(6, queryResp.getResultList().size());
    }

}
