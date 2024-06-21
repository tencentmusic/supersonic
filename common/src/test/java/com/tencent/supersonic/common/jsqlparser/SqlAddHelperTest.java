package com.tencent.supersonic.common.jsqlparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * SqlParserAddHelperTest Test
 */
class SqlAddHelperTest {

    @Test
    void testAddWhere() throws JSQLParserException {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        sql = SqlAddHelper.addWhere(sql, "column_a", 123444555);
        List<String> selectFields = SqlSelectHelper.getAllFields(sql);

        Assert.assertEquals(selectFields.contains("column_a"), true);

        sql = SqlAddHelper.addWhere(sql, "column_b", "123456666");
        selectFields = SqlSelectHelper.getAllFields(sql);

        Assert.assertEquals(selectFields.contains("column_b"), true);

        Expression expression = CCJSqlParserUtil.parseCondExpression(" ( column_c = 111  or column_d = 1111)");

        sql = SqlAddHelper.addWhere(
                "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                        + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1",
                expression);

        Assert.assertEquals(sql.contains("column_c = 111"), true);

        sql = "select 部门,sum (访问次数) from 超音数 where 用户 = alice or 发布日期 ='2023-07-03' group by 部门 limit 1";
        sql = SqlAddHelper.addParenthesisToWhere(sql);
        sql = SqlAddHelper.addWhere(sql, "数据日期", "2023-08-08");
        Assert.assertEquals(sql, "SELECT 部门, sum(访问次数) FROM 超音数 WHERE "
                + "(用户 = alice OR 发布日期 = '2023-07-03') AND 数据日期 = '2023-08-08' GROUP BY 部门 LIMIT 1");

    }

    @Test
    void testAddFunctionToSelect() {
        String sql = "SELECT user_name FROM 超音数 WHERE sys_imp_date <= '2023-09-03' AND "
                + "sys_imp_date >= '2023-08-04' GROUP BY user_name HAVING sum(pv) > 1000";
        List<Expression> havingExpressionList = SqlSelectHelper.getHavingExpression(sql);

        String replaceSql = SqlAddHelper.addFunctionToSelect(sql, havingExpressionList);
        System.out.println(replaceSql);
        Assert.assertEquals("SELECT user_name, sum(pv) FROM 超音数 WHERE sys_imp_date <= '2023-09-03' "
                        + "AND sys_imp_date >= '2023-08-04' GROUP BY user_name HAVING sum(pv) > 1000",
                replaceSql);

        sql = "SELECT user_name,sum(pv) FROM 超音数 WHERE sys_imp_date <= '2023-09-03' AND "
                + "sys_imp_date >= '2023-08-04' GROUP BY user_name HAVING sum(pv) > 1000";
        havingExpressionList = SqlSelectHelper.getHavingExpression(sql);

        replaceSql = SqlAddHelper.addFunctionToSelect(sql, havingExpressionList);
        System.out.println(replaceSql);
        Assert.assertEquals("SELECT user_name, sum(pv) FROM 超音数 WHERE sys_imp_date <= '2023-09-03' "
                        + "AND sys_imp_date >= '2023-08-04' GROUP BY user_name HAVING sum(pv) > 1000",
                replaceSql);

        sql = "SELECT user_name,sum(pv) FROM 超音数 WHERE (sys_imp_date <= '2023-09-03') AND "
                + "sys_imp_date = '2023-08-04' GROUP BY user_name HAVING sum(pv) > 1000";
        havingExpressionList = SqlSelectHelper.getHavingExpression(sql);

        replaceSql = SqlAddHelper.addFunctionToSelect(sql, havingExpressionList);
        System.out.println(replaceSql);
        Assert.assertEquals("SELECT user_name, sum(pv) FROM 超音数 WHERE (sys_imp_date <= '2023-09-03') "
                        + "AND sys_imp_date = '2023-08-04' GROUP BY user_name HAVING sum(pv) > 1000",
                replaceSql);

    }

    @Test
    void testAddAggregateToField() {
        String sql = "SELECT user_name FROM 超音数 WHERE sys_imp_date <= '2023-09-03' AND "
                + "sys_imp_date >= '2023-08-04' GROUP BY user_name HAVING sum(pv) > 1000";
        List<Expression> havingExpressionList = SqlSelectHelper.getHavingExpression(sql);

        String replaceSql = SqlAddHelper.addFunctionToSelect(sql, havingExpressionList);
        System.out.println(replaceSql);
        Assert.assertEquals("SELECT user_name, sum(pv) FROM 超音数 WHERE sys_imp_date <= '2023-09-03' "
                        + "AND sys_imp_date >= '2023-08-04' GROUP BY user_name HAVING sum(pv) > 1000",
                replaceSql);

        sql = "SELECT user_name,sum(pv) FROM 超音数 WHERE sys_imp_date <= '2023-09-03' AND "
                + "sys_imp_date >= '2023-08-04' GROUP BY user_name HAVING sum(pv) > 1000";
        havingExpressionList = SqlSelectHelper.getHavingExpression(sql);

        replaceSql = SqlAddHelper.addFunctionToSelect(sql, havingExpressionList);
        System.out.println(replaceSql);
        Assert.assertEquals("SELECT user_name, sum(pv) FROM 超音数 WHERE sys_imp_date <= '2023-09-03' "
                        + "AND sys_imp_date >= '2023-08-04' GROUP BY user_name HAVING sum(pv) > 1000",
                replaceSql);

        sql = "SELECT user_name,sum(pv) FROM 超音数 WHERE (sys_imp_date <= '2023-09-03') AND "
                + "sys_imp_date = '2023-08-04' GROUP BY user_name HAVING sum(pv) > 1000";
        havingExpressionList = SqlSelectHelper.getHavingExpression(sql);

        replaceSql = SqlAddHelper.addFunctionToSelect(sql, havingExpressionList);
        System.out.println(replaceSql);
        Assert.assertEquals("SELECT user_name, sum(pv) FROM 超音数 WHERE (sys_imp_date <= '2023-09-03') "
                        + "AND sys_imp_date = '2023-08-04' GROUP BY user_name HAVING sum(pv) > 1000",
                replaceSql);
    }

    @Test
    void testAddAggregateToMetricField() {
        String sql = "select department, pv from t_1 where sys_imp_date = '2023-09-11' order by pv desc limit 10";

        Map<String, String> filedNameToAggregate = new HashMap<>();
        filedNameToAggregate.put("pv", "sum");

        Set<String> groupByFields = new HashSet<>();
        groupByFields.add("department");

        String replaceSql = SqlAddHelper.addAggregateToField(sql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, sum(pv) FROM t_1 WHERE sys_imp_date = '2023-09-11' "
                        + "GROUP BY department ORDER BY sum(pv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, pv from t_1 where sys_imp_date = '2023-09-11' and pv >1  "
                + "order by pv desc limit 10";
        replaceSql = SqlAddHelper.addAggregateToField(sql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, sum(pv) FROM t_1 WHERE sys_imp_date = '2023-09-11' "
                        + "AND sum(pv) > 1 GROUP BY department ORDER BY sum(pv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, pv from t_1 where pv >1  order by pv desc limit 10";
        replaceSql = SqlAddHelper.addAggregateToField(sql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, sum(pv) FROM t_1 WHERE sum(pv) > 1 "
                        + "GROUP BY department ORDER BY sum(pv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, pv from t_1 where sum(pv) >1  order by pv desc limit 10";
        replaceSql = SqlAddHelper.addAggregateToField(sql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, sum(pv) FROM t_1 WHERE sum(pv) > 1 "
                        + "GROUP BY department ORDER BY sum(pv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, sum(pv) from t_1 where sys_imp_date = '2023-09-11' and sum(pv) >1 "
                + "GROUP BY department order by pv desc limit 10";
        replaceSql = SqlAddHelper.addAggregateToField(sql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, sum(pv) FROM t_1 WHERE sys_imp_date = '2023-09-11' "
                        + "AND sum(pv) > 1 GROUP BY department ORDER BY sum(pv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, pv from t_1 where sys_imp_date = '2023-09-11' and pv >1 "
                + "GROUP BY department order by pv desc limit 10";
        replaceSql = SqlAddHelper.addAggregateToField(sql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, sum(pv) FROM t_1 WHERE sys_imp_date = '2023-09-11' "
                        + "AND sum(pv) > 1 GROUP BY department ORDER BY sum(pv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, pv from t_1 where sys_imp_date = '2023-09-11' and pv >1 and department = 'HR' "
                + "GROUP BY department order by pv desc limit 10";
        replaceSql = SqlAddHelper.addAggregateToField(sql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, sum(pv) FROM t_1 WHERE sys_imp_date = '2023-09-11' AND sum(pv) > 1 "
                        + "AND department = 'HR' GROUP BY department ORDER BY sum(pv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, pv from t_1 where (pv >1 and department = 'HR') "
                + " and sys_imp_date = '2023-09-11' GROUP BY department order by pv desc limit 10";
        replaceSql = SqlAddHelper.addAggregateToField(sql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, sum(pv) FROM t_1 WHERE (sum(pv) > 1 AND department = 'HR') AND "
                        + "sys_imp_date = '2023-09-11' GROUP BY department ORDER BY sum(pv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, sum(pv) as pv from t_1 where sys_imp_date = '2023-09-11' GROUP BY "
                + "department order by pv desc limit 10";
        replaceSql = SqlReplaceHelper.replaceAlias(sql);
        replaceSql = SqlAddHelper.addAggregateToField(replaceSql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, sum(pv) AS pv "
                        + "FROM t_1 WHERE sys_imp_date = '2023-09-11' GROUP BY department "
                        + "ORDER BY sum(pv) DESC LIMIT 10",
                replaceSql);
    }

    @Test
    void testAddAggregateToCountDiscountMetricField() {
        String sql = "select department, uv from t_1 where sys_imp_date = '2023-09-11' order by uv desc limit 10";

        Map<String, String> filedNameToAggregate = new HashMap<>();
        filedNameToAggregate.put("uv", "count_distinct");

        Set<String> groupByFields = new HashSet<>();
        groupByFields.add("department");

        String replaceSql = SqlAddHelper.addAggregateToField(sql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, count(DISTINCT uv) FROM t_1 WHERE sys_imp_date = '2023-09-11' "
                        + "GROUP BY department ORDER BY count(DISTINCT uv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, uv from t_1 where sys_imp_date = '2023-09-11' and uv >1  "
                + "order by uv desc limit 10";
        replaceSql = SqlAddHelper.addAggregateToField(sql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, count(DISTINCT uv) FROM t_1 WHERE sys_imp_date = '2023-09-11' "
                        + "AND count(DISTINCT uv) > 1 GROUP BY department ORDER BY count(DISTINCT uv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, uv from t_1 where uv >1  order by uv desc limit 10";
        replaceSql = SqlAddHelper.addAggregateToField(sql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, count(DISTINCT uv) FROM t_1 WHERE count(DISTINCT uv) > 1 "
                        + "GROUP BY department ORDER BY count(DISTINCT uv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, uv from t_1 where count(DISTINCT uv) >1  order by uv desc limit 10";
        replaceSql = SqlAddHelper.addAggregateToField(sql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, count(DISTINCT uv) FROM t_1 WHERE count(DISTINCT uv) > 1 "
                        + "GROUP BY department ORDER BY count(DISTINCT uv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, count(DISTINCT uv) from t_1 where sys_imp_date = '2023-09-11'"
                + " and count(DISTINCT uv) >1 "
                + "GROUP BY department order by count(DISTINCT uv) desc limit 10";
        replaceSql = SqlAddHelper.addAggregateToField(sql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, count(DISTINCT uv) FROM t_1 WHERE sys_imp_date = '2023-09-11' "
                        + "AND count(DISTINCT uv) > 1 GROUP BY department ORDER BY count(DISTINCT uv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, uv from t_1 where sys_imp_date = '2023-09-11' and uv >1 "
                + "GROUP BY department order by count(DISTINCT uv) desc limit 10";
        replaceSql = SqlAddHelper.addAggregateToField(sql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, count(DISTINCT uv) FROM t_1 WHERE sys_imp_date = '2023-09-11' "
                        + "AND count(DISTINCT uv) > 1 GROUP BY department ORDER BY count(DISTINCT uv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, uv from t_1 where sys_imp_date = '2023-09-11' and uv >1 and department = 'HR' "
                + "GROUP BY department order by uv desc limit 10";
        replaceSql = SqlAddHelper.addAggregateToField(sql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, count(DISTINCT uv) FROM t_1 WHERE sys_imp_date = "
                        + "'2023-09-11' AND count(DISTINCT uv) > 1 "
                        + "AND department = 'HR' GROUP BY department ORDER BY count(DISTINCT uv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, uv from t_1 where (uv >1 and department = 'HR') "
                + " and sys_imp_date = '2023-09-11' GROUP BY department order by uv desc limit 10";
        replaceSql = SqlAddHelper.addAggregateToField(sql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, count(DISTINCT uv) FROM t_1 WHERE (count(DISTINCT uv) > "
                        + "1 AND department = 'HR') AND "
                        + "sys_imp_date = '2023-09-11' GROUP BY department ORDER BY "
                        + "count(DISTINCT uv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, count(DISTINCT uv) as uv from t_1 where sys_imp_date = '2023-09-11' GROUP BY "
                + "department order by uv desc limit 10";
        replaceSql = SqlReplaceHelper.replaceAlias(sql);
        replaceSql = SqlAddHelper.addAggregateToField(replaceSql, filedNameToAggregate);
        replaceSql = SqlAddHelper.addGroupBy(replaceSql, groupByFields);

        Assert.assertEquals(
                "SELECT department, count(DISTINCT uv) AS uv "
                        + "FROM t_1 WHERE sys_imp_date = '2023-09-11' GROUP BY department "
                        + "ORDER BY count(DISTINCT uv) DESC LIMIT 10",
                replaceSql);
    }

    @Test
    void testAddGroupBy() {
        String sql = "select department, sum(pv) from t_1 where sys_imp_date = '2023-09-11' "
                + "order by sum(pv) desc limit 10";

        Set<String> groupByFields = new HashSet<>();
        groupByFields.add("department");

        String replaceSql = SqlAddHelper.addGroupBy(sql, groupByFields);

        Assert.assertEquals(
                "SELECT department, sum(pv) FROM t_1 WHERE sys_imp_date = '2023-09-11' "
                        + "GROUP BY department ORDER BY sum(pv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, sum(pv) from t_1 where (department = 'HR') and sys_imp_date = '2023-09-11' "
                + "order by sum(pv) desc limit 10";

        replaceSql = SqlAddHelper.addGroupBy(sql, groupByFields);

        Assert.assertEquals(
                "SELECT department, sum(pv) FROM t_1 WHERE (department = 'HR') AND sys_imp_date "
                        + "= '2023-09-11' GROUP BY department ORDER BY sum(pv) DESC LIMIT 10",
                replaceSql);
    }

    @Test
    void testAddHaving() {
        String sql = "select department, sum(pv) from t_1 where sys_imp_date = '2023-09-11' and "
                + "sum(pv) > 2000 group by department order by sum(pv) desc limit 10";
        List<String> groupByFields = new ArrayList<>();
        groupByFields.add("department");

        Set<String> fieldNames = new HashSet<>();
        fieldNames.add("pv");

        String replaceSql = SqlAddHelper.addHaving(sql, fieldNames);

        Assert.assertEquals(
                "SELECT department, sum(pv) FROM t_1 WHERE sys_imp_date = '2023-09-11' "
                        + "GROUP BY department HAVING sum(pv) > 2000 ORDER BY sum(pv) DESC LIMIT 10",
                replaceSql);

        sql = "select department, sum(pv) from t_1 where (sum(pv) > 2000)  and sys_imp_date = '2023-09-11' "
                + "group by department order by sum(pv) desc limit 10";

        replaceSql = SqlAddHelper.addHaving(sql, fieldNames);

        Assert.assertEquals(
                "SELECT department, sum(pv) FROM t_1 WHERE sys_imp_date = '2023-09-11' "
                        + "GROUP BY department HAVING sum(pv) > 2000 ORDER BY sum(pv) DESC LIMIT 10",
                replaceSql);
    }

    @Test
    void testAddParenthesisToWhere() {
        String sql = "select 歌曲名 from 歌曲库 where datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌曲名 = '邓紫棋' and 数据日期 = '2023-08-09' and 歌曲发布时 = '2023-08-01'"
                + " order by 播放量 desc limit 11";

        String replaceSql = SqlAddHelper.addParenthesisToWhere(sql);

        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE (datediff('day', 发布日期, '2023-08-09') <= 1 "
                        + "AND 歌曲名 = '邓紫棋' AND 数据日期 = '2023-08-09' AND 歌曲发布时 = '2023-08-01') "
                        + "ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);
    }

    @Test
    void testAddFieldsToSelect() {
        String correctS2SQL = "SELECT 用户, 页面  FROM 超音数用户部门 GROUP BY 用户, 页面 ORDER BY count(*) DESC";
        String replaceFields = SqlAddHelper.addFieldsToSelect(correctS2SQL,
                SqlSelectHelper.getOrderByFields(correctS2SQL));

        Assert.assertEquals(
                "SELECT 用户, 页面 FROM 超音数用户部门 GROUP BY 用户, 页面 ORDER BY count(*) DESC", replaceFields);
    }

}
