package com.tencent.supersonic.headless;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.demo.S2VisitsDemo;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticTranslateResp;
import com.tencent.supersonic.headless.chat.utils.QueryReqBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TranslateTest extends BaseTest {

    private Long dataSetId;

    @BeforeEach
    public void init() {
        agent = getAgentByName(S2VisitsDemo.AGENT_NAME);
        schema = schemaService.getSemanticSchema(agent.getDataSetIds());
        Optional<Long> id = agent.getDataSetIds().stream().findFirst();
        dataSetId = id.orElse(1L);
    }

    @Test
    public void testSqlExplain() throws Exception {
        String sql = "SELECT 部门, SUM(访问次数) AS 访问次数 FROM 超音数PVUV统计 GROUP BY 部门 ";
        SemanticTranslateResp explain = semanticLayerService
                .translate(QueryReqBuilder.buildS2SQLReq(sql, dataSetId), User.getDefaultUser());
        assertNotNull(explain);
        assertNotNull(explain.getQuerySQL());
        assertTrue(explain.getQuerySQL().contains("department"));
        assertTrue(explain.getQuerySQL().contains("pv"));
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testStructExplain() throws Exception {
        QueryStructReq queryStructReq =
                buildQueryStructReq(Collections.singletonList("department"));
        SemanticTranslateResp explain =
                semanticLayerService.translate(queryStructReq, User.getDefaultUser());
        assertNotNull(explain);
        assertNotNull(explain.getQuerySQL());
        assertTrue(explain.getQuerySQL().contains("department"));
        assertTrue(explain.getQuerySQL().contains("stay_hours"));
    }
}
