package com.tencent.supersonic.headless;

import static java.time.LocalDate.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.util.DataUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class QueryBySqlTest extends BaseTest {

    @Test
    public void testDetailQuery() throws Exception {
        SemanticQueryResp semanticQueryResp = queryBySql("SELECT 用户,访问次数 FROM 超音数PVUV统计 WHERE 用户='alice' ");

        assertEquals(2, semanticQueryResp.getColumns().size());
        QueryColumn firstColumn = semanticQueryResp.getColumns().get(0);
        assertEquals("用户", firstColumn.getName());
        QueryColumn secondColumn = semanticQueryResp.getColumns().get(1);
        assertEquals("访问次数", secondColumn.getName());
        assertTrue(semanticQueryResp.getResultList().size() > 0);
    }

    @Test
    public void testSumQuery() throws Exception {
        SemanticQueryResp semanticQueryResp = queryBySql("SELECT SUM(访问次数) AS 访问次数 FROM 超音数PVUV统计 ");

        assertEquals(1, semanticQueryResp.getColumns().size());
        QueryColumn queryColumn = semanticQueryResp.getColumns().get(0);
        assertEquals("访问次数", queryColumn.getName());
        assertEquals(1, semanticQueryResp.getResultList().size());
    }

    @Test
    public void testGroupByQuery() throws Exception {
        SemanticQueryResp result = queryBySql("SELECT 部门, SUM(访问次数) AS 访问次数 FROM 超音数PVUV统计  GROUP BY 部门 ");
        assertEquals(2, result.getColumns().size());
        QueryColumn firstColumn = result.getColumns().get(0);
        QueryColumn secondColumn = result.getColumns().get(1);
        assertEquals("部门", firstColumn.getName());
        assertEquals("访问次数", secondColumn.getName());
        assertEquals(4, result.getResultList().size());
    }

    @Test
    public void testFilterQuery() throws Exception {
        SemanticQueryResp result = queryBySql(
                "SELECT 部门, SUM(访问次数) AS 访问次数 FROM 超音数PVUV统计 WHERE 部门 ='HR' GROUP BY 部门 ");
        assertEquals(2, result.getColumns().size());
        QueryColumn firstColumn = result.getColumns().get(0);
        QueryColumn secondColumn = result.getColumns().get(1);
        assertEquals("部门", firstColumn.getName());
        assertEquals("访问次数", secondColumn.getName());
        assertEquals(1, result.getResultList().size());
        assertEquals("HR", result.getResultList().get(0).get("department").toString());
    }

    @Test
    public void testDateSumQuery() throws Exception {
        String startDate = now().plusDays(-365).toString();
        String endDate = now().plusDays(0).toString();
        String sql = "SELECT SUM(访问次数) AS 访问次数 FROM 超音数PVUV统计 WHERE 数据日期 >= '%s' AND 数据日期 <= '%s' ";
        SemanticQueryResp semanticQueryResp = queryBySql(String.format(sql, startDate, endDate));
        assertEquals(1, semanticQueryResp.getColumns().size());
        QueryColumn queryColumn = semanticQueryResp.getColumns().get(0);
        assertEquals("访问次数", queryColumn.getName());
        assertEquals(1, semanticQueryResp.getResultList().size());
    }

    @Test
    public void testCacheQuery() throws Exception {
        queryBySql("SELECT 部门, SUM(访问次数) AS 访问次数 FROM 超音数PVUV统计  GROUP BY 部门 ");
        SemanticQueryResp result2 = queryBySql("SELECT 部门, SUM(访问次数) AS 访问次数 FROM 超音数PVUV统计  GROUP BY 部门 ");
        assertTrue(result2.isUseCache());
    }

    @Test
    public void testBizNameQuery() throws Exception {
        SemanticQueryResp result1 = queryBySql("SELECT SUM(pv) FROM 超音数PVUV统计  WHERE department ='HR'");
        SemanticQueryResp result2 = queryBySql("SELECT SUM(访问次数) FROM 超音数PVUV统计  WHERE 部门 ='HR'");
        assertEquals(1, result1.getColumns().size());
        assertEquals(1, result2.getColumns().size());
        assertEquals(result1.getColumns().get(0), result2.getColumns().get(0));
        assertEquals(result1.getResultList(), result2.getResultList());
    }

    @Test
    public void testAuthorization_model() {
        User alice = DataUtils.getUserAlice();
        assertThrows(InvalidPermissionException.class,
                () -> queryBySql("SELECT SUM(pv) FROM 超音数PVUV统计  WHERE department ='HR'", alice));
    }

    @Test
    public void testAuthorization_sensitive_metric() throws Exception {
        User tom = DataUtils.getUserTom();
        SemanticQueryResp semanticQueryResp =
                queryBySql("SELECT SUM(stay_hours) FROM 停留时长统计  WHERE department ='HR'", tom);
        Assertions.assertEquals(false, semanticQueryResp.getColumns().get(0).getAuthorized());
        Assertions.assertEquals("******",
                semanticQueryResp.getResultList().get(0).get("SUM(stay_hours)"));
    }

    @Test
    public void testAuthorization_row_permission() throws Exception {
        User tom = DataUtils.getUserTom();
        SemanticQueryResp semanticQueryResp =
                queryBySql("SELECT SUM(stay_hours) FROM 停留时长统计  WHERE department ='HR'", tom);
        Assertions.assertNotNull(semanticQueryResp.getQueryAuthorization().getMessage());
        Assertions.assertTrue(semanticQueryResp.getSql().contains("user_name = 'tom'"));
    }

}
