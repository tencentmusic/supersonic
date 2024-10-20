package com.tencent.supersonic.headless;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class QueryDimensionTest extends BaseTest {

    @Test
    public void testQueryDimValue() {
        DimensionValueReq queryDimValueReq = new DimensionValueReq();
        queryDimValueReq.setModelId(1L);
        queryDimValueReq.setBizName("department");

        SemanticQueryResp queryResp =
                semanticLayerService.queryDimensionValue(queryDimValueReq, User.getDefaultUser());
        Assert.assertNotNull(queryResp.getResultList());
        Assert.assertEquals(4, queryResp.getResultList().size());
    }
}
