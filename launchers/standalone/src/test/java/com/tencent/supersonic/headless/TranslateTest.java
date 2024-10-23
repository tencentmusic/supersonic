package com.tencent.supersonic.headless;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticTranslateResp;
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
        SemanticTranslateResp explain = semanticLayerService.translate(
                QueryReqBuilder.buildS2SQLReq(sql, DataUtils.getMetricAgentView()),
                User.getDefaultUser());
        assertNotNull(explain);
        assertNotNull(explain.getQuerySQL());
        assertTrue(explain.getQuerySQL().contains("department"));
        assertTrue(explain.getQuerySQL().contains("pv"));
    }

    @Test
    public void testStructExplain() throws Exception {
        QueryStructReq queryStructReq = buildQueryStructReq(Arrays.asList("department"));
        SemanticTranslateResp explain =
                semanticLayerService.translate(queryStructReq, User.getDefaultUser());
        assertNotNull(explain);
        assertNotNull(explain.getQuerySQL());
        assertTrue(explain.getQuerySQL().contains("department"));
        assertTrue(explain.getQuerySQL().contains("pv"));
    }
}
