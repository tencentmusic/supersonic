package com.tencent.supersonic.headless.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import org.junit.jupiter.api.Test;

public class QueryBySqlTest extends BaseTest {

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
    public void testCacheQuery() throws Exception {
        SemanticQueryResp result1 = queryBySql("SELECT 部门, SUM(访问次数) AS 访问次数 FROM 超音数PVUV统计  GROUP BY 部门 ");
        SemanticQueryResp result2 = queryBySql("SELECT 部门, SUM(访问次数) AS 访问次数 FROM 超音数PVUV统计  GROUP BY 部门 ");
        assertEquals(result1, result2);
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
    public void testAuthorization() throws Exception {
        User alice = new User(2L, "alice", "alice", "alice@email", 0);
        assertThrows(InvalidPermissionException.class,
                () -> queryBySql("SELECT SUM(pv) FROM 超音数PVUV统计  WHERE department ='HR'", alice));
    }

}
