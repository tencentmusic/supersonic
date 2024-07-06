package com.tencent.supersonic.headless;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.enums.QueryMethod;
import com.tencent.supersonic.headless.api.pojo.request.TranslateSqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.TranslateResp;
import com.tencent.supersonic.headless.chat.utils.QueryReqBuilder;
import com.tencent.supersonic.util.DataUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TranslateTest extends BaseTest {

    @Test
    public void testSqlExplain() throws Exception {
        String sql = "SELECT 部门, SUM(访问次数) AS 访问次数 FROM 超音数PVUV统计  GROUP BY 部门 ";
        TranslateSqlReq<QuerySqlReq> translateSqlReq = TranslateSqlReq.<QuerySqlReq>builder()
                .queryTypeEnum(QueryMethod.SQL)
                .queryReq(QueryReqBuilder.buildS2SQLReq(sql, DataUtils.getMetricAgentView()))
                .build();
        TranslateResp explain = semanticLayerService.translate(translateSqlReq, User.getFakeUser());
        assertNotNull(explain);
        assertNotNull(explain.getSql());
        assertTrue(explain.getSql().contains("department"));
        assertTrue(explain.getSql().contains("pv"));
    }

    @Test
    public void testStructExplain() throws Exception {
        QueryStructReq queryStructReq = buildQueryStructReq(Arrays.asList("department"));
        TranslateSqlReq<QueryStructReq> translateSqlReq = TranslateSqlReq.<QueryStructReq>builder()
                .queryTypeEnum(QueryMethod.STRUCT)
                .queryReq(queryStructReq)
                .build();
        TranslateResp explain = semanticLayerService.translate(translateSqlReq, User.getFakeUser());
        assertNotNull(explain);
        assertNotNull(explain.getSql());
        assertTrue(explain.getSql().contains("department"));
        assertTrue(explain.getSql().contains("pv"));
    }

}
