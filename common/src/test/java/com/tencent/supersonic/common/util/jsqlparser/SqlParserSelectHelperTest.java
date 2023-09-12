package com.tencent.supersonic.common.util.jsqlparser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * SqlParserSelectHelper Test
 */
class SqlParserSelectHelperTest {


    @Test
    void getWhereFilterExpression() {

        List<FilterExpression> filterExpression = SqlParserSelectHelper.getFilterExpression(
                "SELECT department, user_id, field_a FROM s2 WHERE "
                        + "sys_imp_date = '2023-08-08' AND YEAR(publish_date) = 2023 "
                        + " AND user_id = 'alice'  ORDER BY pv DESC LIMIT 1");

        System.out.println(filterExpression);

        filterExpression = SqlParserSelectHelper.getFilterExpression(
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08' "
                        + " AND YEAR(publish_date) = 2023 "
                        + " AND MONTH(publish_date) = 8"
                        + " AND user_id = 'alice'  ORDER BY pv DESC LIMIT 1");

        System.out.println(filterExpression);

        filterExpression = SqlParserSelectHelper.getFilterExpression(
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08'"
                        + " AND YEAR(publish_date) = 2023 "
                        + " AND MONTH(publish_date) = 8 AND DAY(publish_date) =20 "
                        + " AND user_id = 'alice'  ORDER BY pv DESC LIMIT 1");
        System.out.println(filterExpression);

        filterExpression = SqlParserSelectHelper.getFilterExpression(
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08' "
                        + " AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1");

        System.out.println(filterExpression);

        filterExpression = SqlParserSelectHelper.getFilterExpression(
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08' "
                        + "AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1");

        System.out.println(filterExpression);

        filterExpression = SqlParserSelectHelper.getFilterExpression(
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08' "
                        + "AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1");

        System.out.println(filterExpression);

        filterExpression = SqlParserSelectHelper.getFilterExpression(
                "SELECT department, user_id, field_a FROM s2 WHERE "
                        + "user_id = 'alice' AND publish_date = '11'   ORDER BY pv DESC LIMIT 1");

        System.out.println(filterExpression);

        filterExpression = SqlParserSelectHelper.getFilterExpression(
                "SELECT department, user_id, field_a FROM s2 WHERE "
                        + "user_id = 'alice' AND  publish_date > 10000   ORDER BY pv DESC LIMIT 1");

        System.out.println(filterExpression);

        filterExpression = SqlParserSelectHelper.getFilterExpression(
                "SELECT department, user_id, field_a FROM s2 WHERE "
                        + "user_id like '%alice%' AND  publish_date > 10000   ORDER BY pv DESC LIMIT 1");

        System.out.println(filterExpression);
    }


    @Test
    void getAllFields() {

        List<String> allFields = SqlParserSelectHelper.getAllFields(
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08'"
                        + " AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1");

        Assert.assertEquals(allFields.size(), 6);

        allFields = SqlParserSelectHelper.getAllFields(
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date >= '2023-08-08'"
                        + " AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1");

        Assert.assertEquals(allFields.size(), 6);

        allFields = SqlParserSelectHelper.getAllFields(
                "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' and 用户 = 'alice'"
                        + " and 发布日期 ='11' group by 部门 limit 1");

        Assert.assertEquals(allFields.size(), 5);

        allFields = SqlParserSelectHelper.getAllFields(
                "SELECT user_name FROM 超音数 WHERE sys_imp_date <= '2023-09-03' AND "
                        + "sys_imp_date >= '2023-08-04' GROUP BY user_name ORDER BY sum(pv) DESC LIMIT 10 ");

        Assert.assertEquals(allFields.size(), 3);
    }


    @Test
    void getSelectFields() {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        List<String> selectFields = SqlParserSelectHelper.getSelectFields(sql);

        Assert.assertEquals(selectFields.contains("访问次数"), true);
        Assert.assertEquals(selectFields.contains("部门"), true);
    }

    @Test
    void getWhereFields() {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08'"
                + " and 用户 = 'alice' and 发布日期 ='11' group by 部门 limit 1";
        List<String> selectFields = SqlParserSelectHelper.getWhereFields(sql);

        Assert.assertEquals(selectFields.contains("发布日期"), true);
        Assert.assertEquals(selectFields.contains("数据日期"), true);
        Assert.assertEquals(selectFields.contains("用户"), true);

        sql = "select 部门,用户 from 超音数 where 数据日期 = '2023-08-08'"
                + " and 用户 = 'alice' and 发布日期 ='11' order by 访问次数 limit 1";
        selectFields = SqlParserSelectHelper.getWhereFields(sql);

        Assert.assertEquals(selectFields.contains("发布日期"), true);
        Assert.assertEquals(selectFields.contains("数据日期"), true);
        Assert.assertEquals(selectFields.contains("用户"), true);
    }

    @Test
    void getOrderByFields() {

        String sql = "select 部门,用户 from 超音数 where 数据日期 = '2023-08-08'"
                + " and 用户 = 'alice' and 发布日期 ='11' order by 访问次数 limit 1";
        List<String> selectFields = SqlParserSelectHelper.getOrderByFields(sql);

        Assert.assertEquals(selectFields.contains("访问次数"), true);

        sql = "SELECT user_name FROM 超音数 WHERE sys_imp_date <= '2023-09-03' AND "
                + "sys_imp_date >= '2023-08-04' GROUP BY user_name ORDER BY sum(pv) DESC LIMIT 10 ";
        selectFields = SqlParserSelectHelper.getOrderByFields(sql);

        Assert.assertEquals(selectFields.contains("pv"), true);
    }


    @Test
    void addWhere() throws JSQLParserException {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        sql = SqlParserUpdateHelper.addWhere(sql, "column_a", 123444555);
        List<String> selectFields = SqlParserSelectHelper.getAllFields(sql);

        Assert.assertEquals(selectFields.contains("column_a"), true);

        sql = SqlParserUpdateHelper.addWhere(sql, "column_b", "123456666");
        selectFields = SqlParserSelectHelper.getAllFields(sql);

        Assert.assertEquals(selectFields.contains("column_b"), true);

        Expression expression = CCJSqlParserUtil.parseCondExpression(" ( column_c = 111  or column_d = 1111)");

        sql = SqlParserUpdateHelper.addWhere(
                "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                        + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1",
                expression);

        Assert.assertEquals(sql.contains("column_c = 111"), true);

    }


    @Test
    void hasAggregateFunction() throws JSQLParserException {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        boolean hasAggregateFunction = SqlParserSelectHelper.hasAggregateFunction(sql);

        Assert.assertEquals(hasAggregateFunction, true);
        sql = "select 部门,count (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        hasAggregateFunction = SqlParserSelectHelper.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, true);

        sql = "SELECT count(1) FROM s2 WHERE sys_imp_date = '2023-08-08' AND user_id = 'alice'"
                + " AND publish_date = '11' ORDER BY pv DESC LIMIT 1";
        hasAggregateFunction = SqlParserSelectHelper.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, true);

        sql = "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08' "
                + "AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1";
        hasAggregateFunction = SqlParserSelectHelper.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, false);

        sql = "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08'"
                + " AND user_id = 'alice' AND publish_date = '11'";
        hasAggregateFunction = SqlParserSelectHelper.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, false);


        sql = "SELECT user_name, pv FROM t_34 WHERE sys_imp_date <= '2023-09-03' "
                + "AND sys_imp_date >= '2023-08-04' GROUP BY user_name ORDER BY sum(pv) DESC LIMIT 10";
        hasAggregateFunction = SqlParserSelectHelper.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, true);
    }

    private Map<String, String> initParams() {
        Map<String, String> fieldToBizName = new HashMap<>();
        fieldToBizName.put("部门", "department");
        fieldToBizName.put("用户", "user_id");
        fieldToBizName.put("数据日期", "sys_imp_date");
        fieldToBizName.put("发布日期", "publish_date");
        fieldToBizName.put("访问次数", "pv");
        fieldToBizName.put("歌曲名", "song_name");
        fieldToBizName.put("歌手名", "singer_name");
        fieldToBizName.put("播放", "play_count");
        fieldToBizName.put("歌曲发布时间", "song_publis_date");
        fieldToBizName.put("歌曲发布年份", "song_publis_year");
        fieldToBizName.put("转3.0前后30天结算份额衰减", "fdafdfdsa_fdas");
        return fieldToBizName;
    }
}
