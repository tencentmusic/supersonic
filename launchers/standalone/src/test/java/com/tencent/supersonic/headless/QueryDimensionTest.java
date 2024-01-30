package com.tencent.supersonic.headless;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.QueryDimValueReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class QueryDimensionTest extends BaseTest {

    @Test
    public void testQueryDimValue() {
        QueryDimValueReq queryDimValueReq = new QueryDimValueReq();
        queryDimValueReq.setModelId(1L);
        queryDimValueReq.setDimensionBizName("department");

        SemanticQueryResp queryResp = queryService.queryDimValue(queryDimValueReq, User.getFakeUser());
        Assert.assertNotNull(queryResp.getResultList());
        Assert.assertEquals(4, queryResp.getResultList().size());
    }

}
