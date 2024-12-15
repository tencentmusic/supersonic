package com.tencent.supersonic.headless;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.demo.S2VisitsDemo;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticTranslateResp;
import com.tencent.supersonic.headless.chat.utils.QueryReqBuilder;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TranslatorTest extends BaseTest {

    private Long dataSetId;

    private DatabaseResp databaseResp;

    @BeforeEach
    public void init() {
        agent = getAgentByName(S2VisitsDemo.AGENT_NAME);
        schema = schemaService.getSemanticSchema(agent.getDataSetIds());
        Optional<Long> id = agent.getDataSetIds().stream().findFirst();
        dataSetId = id.orElse(1L);
        databaseResp = databaseService.getDatabase(1L);
    }

    private void executeSql(String sql) {
        SemanticQueryResp queryResp = databaseService.executeSql(sql, databaseResp);
        assert StringUtils.isBlank(queryResp.getErrorMsg());
        System.out.println(
                String.format("Execute result: %s", JsonUtil.toString(queryResp.getResultList())));
    }

    @Test
    public void testSql() throws Exception {
        String sql =
                "SELECT SUM(访问次数) AS _总访问次数_ FROM 超音数数据集 WHERE 数据日期 >= '2024-11-15' AND 数据日期 <= '2024-12-15'";
        SemanticTranslateResp explain = semanticLayerService
                .translate(QueryReqBuilder.buildS2SQLReq(sql, dataSetId), User.getDefaultUser());
        assertNotNull(explain);
        assertNotNull(explain.getQuerySQL());
        assertTrue(explain.getQuerySQL().contains("count(imp_date)"));
        executeSql(explain.getQuerySQL());
    }

    @Test
    public void testSql_1() throws Exception {
        String sql = "SELECT 部门, SUM(访问次数) AS 总访问次数 FROM 超音数PVUV统计 GROUP BY 部门 ";
        SemanticTranslateResp explain = semanticLayerService
                .translate(QueryReqBuilder.buildS2SQLReq(sql, dataSetId), User.getDefaultUser());
        assertNotNull(explain);
        assertNotNull(explain.getQuerySQL());
        assertTrue(explain.getQuerySQL().contains("department"));
        assertTrue(explain.getQuerySQL().contains("count(imp_date)"));
        executeSql(explain.getQuerySQL());
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testSql_2() throws Exception {
        String sql =
                "WITH _department_visits_ AS (SELECT 部门, SUM(访问次数) AS _total_visits_ FROM 超音数数据集 WHERE 数据日期 >= '2024-11-15' AND 数据日期 <= '2024-12-15' GROUP BY 部门) SELECT 部门 FROM _department_visits_ ORDER BY _total_visits_ DESC LIMIT 2";
        SemanticTranslateResp explain = semanticLayerService
                .translate(QueryReqBuilder.buildS2SQLReq(sql, dataSetId), User.getDefaultUser());
        assertNotNull(explain);
        assertNotNull(explain.getQuerySQL());
        assertTrue(explain.getQuerySQL().toLowerCase().contains("department"));
        assertTrue(explain.getQuerySQL().toLowerCase().contains("count(imp_date)"));
        executeSql(explain.getQuerySQL());
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testSql_3() throws Exception {
        String sql =
                "WITH recent_data AS (SELECT 用户名, 访问次数 FROM 超音数数据集 WHERE 部门 = 'marketing' AND 数据日期 >= '2024-12-01' AND 数据日期 <= '2024-12-15') SELECT 用户名 FROM recent_data ORDER BY 访问次数 DESC LIMIT 1";
        SemanticTranslateResp explain = semanticLayerService
                .translate(QueryReqBuilder.buildS2SQLReq(sql, dataSetId), User.getDefaultUser());
        assertNotNull(explain);
        assertNotNull(explain.getQuerySQL());
        assertTrue(explain.getQuerySQL().toLowerCase().contains("department"));
        assertTrue(explain.getQuerySQL().toLowerCase().contains("count(imp_date)"));
        executeSql(explain.getQuerySQL());
    }

}
