package com.tencent.supersonic.headless;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.demo.S2VisitsDemo;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticTranslateResp;
import com.tencent.supersonic.headless.chat.utils.QueryReqBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TranslatorTest extends BaseTest {

    private DataSetResp dataSet;

    @BeforeEach
    public void init() {
        agent = getAgentByName(S2VisitsDemo.AGENT_NAME);
        schema = schemaService.getSemanticSchema(agent.getDataSetIds());
        if (Objects.nonNull(agent)) {
            long dataSetId = agent.getDataSetIds().stream().findFirst().get();
            dataSet = dataSetService.getDataSet(dataSetId);
        }
    }

    @Test
    public void testSql() throws Exception {
        String sql =
                "SELECT SUM(访问次数) AS _总访问次数_ FROM 超音数数据集 WHERE 数据日期 >= '2024-11-15' AND 数据日期 <= '2024-12-15'";
        SemanticTranslateResp explain = semanticLayerService
                .translate(QueryReqBuilder.buildS2SQLReq(sql, dataSet), User.getDefaultUser());
        assertNotNull(explain);
        assertNotNull(explain.getQuerySQL());
        assertTrue(explain.getQuerySQL().contains("count(1)"));
        executeSql(explain.getQuerySQL());
    }

    @Test
    public void testSql_1() throws Exception {
        String sql = "SELECT 部门, SUM(访问次数) AS 总访问次数 FROM 超音数PVUV统计 GROUP BY 部门 ";
        SemanticTranslateResp explain = semanticLayerService
                .translate(QueryReqBuilder.buildS2SQLReq(sql, dataSet), User.getDefaultUser());
        assertNotNull(explain);
        assertNotNull(explain.getQuerySQL());
        assertTrue(explain.getQuerySQL().contains("department"));
        assertTrue(explain.getQuerySQL().contains("count(1)"));
        executeSql(explain.getQuerySQL());
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testSql_2() throws Exception {
        String sql =
                "WITH _department_visits_ AS (SELECT 部门, SUM(访问次数) AS _total_visits_ FROM 超音数数据集 WHERE 数据日期 >= '2024-11-15' AND 数据日期 <= '2024-12-15' GROUP BY 部门) SELECT 部门 FROM _department_visits_ ORDER BY _total_visits_ DESC LIMIT 2";
        SemanticTranslateResp explain = semanticLayerService
                .translate(QueryReqBuilder.buildS2SQLReq(sql, dataSet), User.getDefaultUser());
        assertNotNull(explain);
        assertNotNull(explain.getQuerySQL());
        assertTrue(explain.getQuerySQL().toLowerCase().contains("department"));
        assertTrue(explain.getQuerySQL().toLowerCase().contains("count(1)"));
        executeSql(explain.getQuerySQL());
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testSql_3() throws Exception {
        String sql =
                "WITH recent_data AS (SELECT 用户名, 访问次数 FROM 超音数数据集 WHERE 部门 = 'marketing' AND 数据日期 >= '2024-12-01' AND 数据日期 <= '2024-12-15') SELECT 用户名 FROM recent_data ORDER BY 访问次数 DESC LIMIT 1";
        SemanticTranslateResp explain = semanticLayerService
                .translate(QueryReqBuilder.buildS2SQLReq(sql, dataSet), User.getDefaultUser());
        assertNotNull(explain);
        assertNotNull(explain.getQuerySQL());
        assertTrue(explain.getQuerySQL().toLowerCase().contains("department"));
        assertTrue(explain.getQuerySQL().toLowerCase().contains("count(1)"));
        executeSql(explain.getQuerySQL());
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testSql_unionALL() throws Exception {
        String sql = new String(
                Files.readAllBytes(
                        Paths.get(ClassLoader.getSystemResource("sql/testUnion.sql").toURI())),
                StandardCharsets.UTF_8);
        SemanticTranslateResp explain = semanticLayerService
                .translate(QueryReqBuilder.buildS2SQLReq(sql, dataSet), User.getDefaultUser());
        assertNotNull(explain);
        assertNotNull(explain.getQuerySQL());
        assertTrue(explain.getQuerySQL().contains("user_name"));
        assertTrue(explain.getQuerySQL().contains("pv"));
        executeSql(explain.getQuerySQL());
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testSql_with() throws Exception {
        String sql = new String(
                Files.readAllBytes(
                        Paths.get(ClassLoader.getSystemResource("sql/testWith.sql").toURI())),
                StandardCharsets.UTF_8);
        SemanticTranslateResp explain = semanticLayerService
                .translate(QueryReqBuilder.buildS2SQLReq(sql, dataSet), User.getDefaultUser());
        assertNotNull(explain);
        assertNotNull(explain.getQuerySQL());
        executeSql(explain.getQuerySQL());
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testSql_subquery() throws Exception {
        String sql = new String(
                Files.readAllBytes(
                        Paths.get(ClassLoader.getSystemResource("sql/testSubquery.sql").toURI())),
                StandardCharsets.UTF_8);
        SemanticTranslateResp explain = semanticLayerService
                .translate(QueryReqBuilder.buildS2SQLReq(sql, dataSet), User.getDefaultUser());
        assertNotNull(explain);
        assertNotNull(explain.getQuerySQL());
        executeSql(explain.getQuerySQL());
    }

}
