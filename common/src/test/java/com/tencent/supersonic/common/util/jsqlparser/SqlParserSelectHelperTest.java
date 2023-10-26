package com.tencent.supersonic.common.util.jsqlparser;

import java.util.List;
import net.sf.jsqlparser.expression.Expression;
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

        filterExpression = SqlParserSelectHelper.getFilterExpression(
                "SELECT department, pv FROM s2 WHERE "
                        + "user_id like '%alice%' AND  publish_date > 10000  "
                        + "group by department having sum(pv) > 2000 ORDER BY pv DESC LIMIT 1");

        System.out.println(filterExpression);

        filterExpression = SqlParserSelectHelper.getFilterExpression(
                "SELECT department, pv FROM s2 WHERE "
                        + "(user_id like '%alice%' AND  publish_date > 10000)  and sys_imp_date = '2023-08-08' "
                        + "group by department having sum(pv) > 2000 ORDER BY pv DESC LIMIT 1");

        System.out.println(filterExpression);

        filterExpression = SqlParserSelectHelper.getFilterExpression(
                "SELECT department, pv FROM s2 WHERE "
                        + "(user_id like '%alice%' AND  publish_date > 10000) and song_name in "
                        + "('七里香','晴天') and sys_imp_date = '2023-08-08' "
                        + "group by department having sum(pv) > 2000 ORDER BY pv DESC LIMIT 1");

        System.out.println(filterExpression);

        filterExpression = SqlParserSelectHelper.getFilterExpression(
                "SELECT department, pv FROM s2 WHERE "
                        + "(user_id like '%alice%' AND  publish_date > 10000) and song_name in (1,2) "
                        + "and sys_imp_date = '2023-08-08' "
                        + "group by department having sum(pv) > 2000 ORDER BY pv DESC LIMIT 1");

        System.out.println(filterExpression);

        filterExpression = SqlParserSelectHelper.getFilterExpression(
                "SELECT department, pv FROM s2 WHERE "
                        + "(user_id like '%alice%' AND  publish_date > 10000) and 1 in (1) "
                        + "and sys_imp_date = '2023-08-08' "
                        + "group by department having sum(pv) > 2000 ORDER BY pv DESC LIMIT 1");

        System.out.println(filterExpression);

        filterExpression = SqlParserSelectHelper.getFilterExpression("SELECT sum(销量) / (SELECT sum(销量) FROM 营销月模型 "
                + "WHERE MONTH(数据日期) = 9) FROM 营销月模型 WHERE 国家中文名 = '肯尼亚' AND MONTH(数据日期) = 9");

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

        allFields = SqlParserSelectHelper.getAllFields(
                "SELECT user_name FROM 超音数 WHERE sys_imp_date <= '2023-09-03' AND "
                        + "sys_imp_date >= '2023-08-04' GROUP BY user_name HAVING sum(pv) > 1000");

        Assert.assertEquals(allFields.size(), 3);

        allFields = SqlParserSelectHelper.getAllFields(
                "SELECT department, user_id, field_a FROM s2 WHERE "
                        + "(user_id = 'alice' AND publish_date = '11') and sys_imp_date "
                        + "= '2023-08-08' ORDER BY pv DESC LIMIT 1");

        Assert.assertEquals(allFields.size(), 6);

        allFields = SqlParserSelectHelper.getAllFields(
                "SELECT * FROM CSpider  WHERE (评分 < (SELECT min(评分) FROM CSpider WHERE 语种 = '英文' ))"
                        + " AND 数据日期 = '2023-10-12'");

        Assert.assertEquals(allFields.size(), 3);

        allFields = SqlParserSelectHelper.getAllFields("SELECT sum(销量) / (SELECT sum(销量) FROM 营销 "
                + "WHERE MONTH(数据日期) = 9) FROM 营销 WHERE 国家中文名 = '中国' AND MONTH(数据日期) = 9");

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

        sql = "select 部门,用户 from 超音数 where"
                + " (用户 = 'alice' and 发布日期 ='11') and 数据日期 = '2023-08-08' "
                + "order by 访问次数 limit 1";
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
    void getGroupByFields() {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08'"
                + " and 用户 = 'alice' and 发布日期 ='11' group by 部门 limit 1";
        List<String> selectFields = SqlParserSelectHelper.getGroupByFields(sql);

        Assert.assertEquals(selectFields.contains("部门"), true);

    }

    @Test
    void getHavingExpression() {

        String sql = "SELECT user_name FROM 超音数 WHERE sys_imp_date <= '2023-09-03' AND "
                + "sys_imp_date >= '2023-08-04' GROUP BY user_name HAVING sum(pv) > 1000";
        Expression leftExpression = SqlParserSelectHelper.getHavingExpression(sql);

        Assert.assertEquals(leftExpression.toString(), "sum(pv)");

    }

    @Test
    void getAggregateFields() {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08'"
                + " and 用户 = 'alice' and 发布日期 ='11' group by 部门 limit 1";
        List<String> selectFields = SqlParserSelectHelper.getAggregateFields(sql);
        Assert.assertEquals(selectFields.contains("访问次数"), true);

    }

}
