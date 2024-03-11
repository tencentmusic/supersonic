package com.tencent.supersonic.headless;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.enums.QueryMethod;
import com.tencent.supersonic.headless.api.pojo.request.ExplainSqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.ExplainResp;
import com.tencent.supersonic.headless.core.utils.QueryReqBuilder;
import com.tencent.supersonic.util.DataUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExplainTest extends BaseTest {

    @Test
    public void testSqlExplain() throws Exception {
        String sql = "SELECT 部门, SUM(访问次数) AS 访问次数 FROM 超音数PVUV统计  GROUP BY 部门 ";
        ExplainSqlReq<QuerySqlReq> explainSqlReq = ExplainSqlReq.<QuerySqlReq>builder()
                .queryTypeEnum(QueryMethod.SQL)
                .queryReq(QueryReqBuilder.buildS2SQLReq(sql, DataUtils.getMetricAgentView()))
                .build();
        ExplainResp explain = queryService.explain(explainSqlReq, User.getFakeUser());
        assertNotNull(explain);
        assertNotNull(explain.getSql());
        assertTrue(explain.getSql().contains("department"));
        assertTrue(explain.getSql().contains("pv"));
    }

    @Test
    public void testStructExplain() throws Exception {
        QueryStructReq queryStructReq = buildQueryStructReq(Arrays.asList("department"));
        ExplainSqlReq<QueryStructReq> explainSqlReq = ExplainSqlReq.<QueryStructReq>builder()
                .queryTypeEnum(QueryMethod.STRUCT)
                .queryReq(queryStructReq)
                .build();
        ExplainResp explain = queryService.explain(explainSqlReq, User.getFakeUser());
        assertNotNull(explain);
        assertNotNull(explain.getSql());
        assertTrue(explain.getSql().contains("department"));
        assertTrue(explain.getSql().contains("pv"));
    }

}
