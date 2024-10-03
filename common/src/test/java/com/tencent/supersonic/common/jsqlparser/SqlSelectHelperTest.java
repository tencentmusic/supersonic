package com.tencent.supersonic.common.jsqlparser;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.Select;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.List;

/** SqlParserSelectHelper Test */
class SqlSelectHelperTest {

    @Test
    void testGetWhereFilterExpression() {

        Select selectStatement = SqlSelectHelper
                .getSelect("select 用户名, 访问次数 from 超音数 where 用户名 in ('alice', 'lucy')");
        System.out.println(selectStatement);

        List<FieldExpression> fieldExpression = SqlSelectHelper
                .getFilterExpression("SELECT department, user_id, field_a FROM s2 WHERE "
                        + "sys_imp_date = '2023-08-08' AND YEAR(publish_date) = 2023 "
                        + " AND user_id = 'alice'  ORDER BY pv DESC LIMIT 1");

        System.out.println(fieldExpression);

        fieldExpression = SqlSelectHelper.getFilterExpression(
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08' "
                        + " AND YEAR(publish_date) = 2023 " + " AND MONTH(publish_date) = 8"
                        + " AND user_id = 'alice'  ORDER BY pv DESC LIMIT 1");

        System.out.println(fieldExpression);

        fieldExpression = SqlSelectHelper.getFilterExpression(
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08'"
                        + " AND YEAR(publish_date) = 2023 "
                        + " AND MONTH(publish_date) = 8 AND DAY(publish_date) =20 "
                        + " AND user_id = 'alice'  ORDER BY pv DESC LIMIT 1");
        System.out.println(fieldExpression);

        fieldExpression = SqlSelectHelper.getFilterExpression(
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08' "
                        + " AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1");

        System.out.println(fieldExpression);

        fieldExpression = SqlSelectHelper.getFilterExpression(
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08' "
                        + "AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1");

        System.out.println(fieldExpression);

        fieldExpression = SqlSelectHelper.getFilterExpression(
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08' "
                        + "AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1");

        System.out.println(fieldExpression);

        fieldExpression = SqlSelectHelper
                .getFilterExpression("SELECT department, user_id, field_a FROM s2 WHERE "
                        + "user_id = 'alice' AND publish_date = '11'   ORDER BY pv DESC LIMIT 1");

        System.out.println(fieldExpression);

        fieldExpression = SqlSelectHelper
                .getFilterExpression("SELECT department, user_id, field_a FROM s2 WHERE "
                        + "user_id = 'alice' AND  publish_date > 10000   ORDER BY pv DESC LIMIT 1");

        System.out.println(fieldExpression);

        fieldExpression = SqlSelectHelper
                .getFilterExpression("SELECT department, user_id, field_a FROM s2 WHERE "
                        + "user_id like '%alice%' AND  publish_date > 10000   ORDER BY pv DESC LIMIT 1");

        System.out.println(fieldExpression);

        fieldExpression = SqlSelectHelper.getFilterExpression("SELECT department, pv FROM s2 WHERE "
                + "user_id like '%alice%' AND  publish_date > 10000  "
                + "group by department having sum(pv) > 2000 ORDER BY pv DESC LIMIT 1");

        System.out.println(fieldExpression);

        fieldExpression = SqlSelectHelper.getFilterExpression("SELECT department, pv FROM s2 WHERE "
                + "(user_id like '%alice%' AND  publish_date > 10000)  and sys_imp_date = '2023-08-08' "
                + "group by department having sum(pv) > 2000 ORDER BY pv DESC LIMIT 1");

        System.out.println(fieldExpression);

        fieldExpression = SqlSelectHelper.getFilterExpression("SELECT department, pv FROM s2 WHERE "
                + "(user_id like '%alice%' AND  publish_date > 10000) and song_name in "
                + "('七里香','晴天') and sys_imp_date = '2023-08-08' "
                + "group by department having sum(pv) > 2000 ORDER BY pv DESC LIMIT 1");

        System.out.println(fieldExpression);

        fieldExpression = SqlSelectHelper.getFilterExpression("SELECT department, pv FROM s2 WHERE "
                + "(user_id like '%alice%' AND  publish_date > 10000) and song_name in (1,2) "
                + "and sys_imp_date = '2023-08-08' "
                + "group by department having sum(pv) > 2000 ORDER BY pv DESC LIMIT 1");

        System.out.println(fieldExpression);

        fieldExpression = SqlSelectHelper.getFilterExpression("SELECT department, pv FROM s2 WHERE "
                + "(user_id like '%alice%' AND  publish_date > 10000) and 1 in (1) "
                + "and sys_imp_date = '2023-08-08' "
                + "group by department having sum(pv) > 2000 ORDER BY pv DESC LIMIT 1");

        System.out.println(fieldExpression);

        fieldExpression =
                SqlSelectHelper.getFilterExpression("SELECT sum(销量) / (SELECT sum(销量) FROM 营销月模型 "
                        + "WHERE MONTH(数据日期) = 9) FROM 营销月模型 WHERE 国家中文名 = '肯尼亚' AND MONTH(数据日期) = 9");

        System.out.println(fieldExpression);

        fieldExpression = SqlSelectHelper.getFilterExpression(
                "select 等级, count(*) from 歌手 where 别名 = '港台' or 活跃区域 = '港台' and"
                        + " datediff('day', 数据日期, '2023-12-24') <= 0 group by 等级");

        System.out.println(fieldExpression);
    }

    @Test
    void testGetAllFields() {

        List<String> allFields = SqlSelectHelper.getAllSelectFields(
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08'"
                        + " AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1");

        Assert.assertEquals(allFields.size(), 6);

        allFields = SqlSelectHelper.getAllSelectFields(
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date >= '2023-08-08'"
                        + " AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1");

        Assert.assertEquals(allFields.size(), 6);

        allFields = SqlSelectHelper.getAllSelectFields(
                "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' and 用户 = 'alice'"
                        + " and 发布日期 ='11' group by 部门 limit 1");

        Assert.assertEquals(allFields.size(), 5);

        allFields = SqlSelectHelper.getAllSelectFields(
                "SELECT user_name FROM 超音数 WHERE sys_imp_date <= '2023-09-03' AND "
                        + "sys_imp_date >= '2023-08-04' GROUP BY user_name ORDER BY sum(pv) DESC LIMIT 10 ");

        Assert.assertEquals(allFields.size(), 3);

        allFields = SqlSelectHelper.getAllSelectFields(
                "SELECT user_name FROM 超音数 WHERE sys_imp_date <= '2023-09-03' AND "
                        + "sys_imp_date >= '2023-08-04' GROUP BY user_name HAVING sum(pv) > 1000");

        Assert.assertEquals(allFields.size(), 3);

        allFields = SqlSelectHelper
                .getAllSelectFields("SELECT department, user_id, field_a FROM s2 WHERE "
                        + "(user_id = 'alice' AND publish_date = '11') and sys_imp_date "
                        + "= '2023-08-08' ORDER BY pv DESC LIMIT 1");

        Assert.assertEquals(allFields.size(), 6);

        allFields = SqlSelectHelper.getAllSelectFields(
                "SELECT * FROM CSpider  WHERE (评分 < (SELECT min(评分) FROM CSpider WHERE 语种 = '英文' ))"
                        + " AND 数据日期 = '2023-10-12'");

        Assert.assertEquals(allFields.size(), 3);

        allFields = SqlSelectHelper.getAllSelectFields("SELECT sum(销量) / (SELECT sum(销量) FROM 营销 "
                + "WHERE MONTH(数据日期) = 9) FROM 营销 WHERE 国家中文名 = '中国' AND MONTH(数据日期) = 9");

        Assert.assertEquals(allFields.size(), 3);

        allFields = SqlSelectHelper.getAllSelectFields(
                "SELECT 用户, 页面  FROM 超音数用户部门 GROUP BY 用户, 页面 ORDER BY count(*) DESC");

        Assert.assertEquals(allFields.size(), 2);
    }

    @Test
    void testGetSelectFields() {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        List<String> selectFields = SqlSelectHelper.getSelectFields(sql);

        Assert.assertEquals(selectFields.contains("访问次数"), true);
        Assert.assertEquals(selectFields.contains("部门"), true);
    }

    @Test
    void testGetWhereFields() {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08'"
                + " and 用户 = 'alice' and 发布日期 ='11' group by 部门 limit 1";
        List<String> selectFields = SqlSelectHelper.getWhereFields(sql);

        Assert.assertEquals(selectFields.contains("发布日期"), true);
        Assert.assertEquals(selectFields.contains("数据日期"), true);
        Assert.assertEquals(selectFields.contains("用户"), true);

        sql = "select 部门,用户 from 超音数 where 数据日期 = '2023-08-08'"
                + " and 用户 = 'alice' and 发布日期 ='11' order by 访问次数 limit 1";
        selectFields = SqlSelectHelper.getWhereFields(sql);

        Assert.assertEquals(selectFields.contains("发布日期"), true);
        Assert.assertEquals(selectFields.contains("数据日期"), true);
        Assert.assertEquals(selectFields.contains("用户"), true);

        sql = "select 部门,用户 from 超音数 where"
                + " (用户 = 'alice' and 发布日期 ='11') and 数据日期 = '2023-08-08' "
                + "order by 访问次数 limit 1";
        selectFields = SqlSelectHelper.getWhereFields(sql);

        Assert.assertEquals(selectFields.contains("发布日期"), true);
        Assert.assertEquals(selectFields.contains("数据日期"), true);
        Assert.assertEquals(selectFields.contains("用户"), true);
    }

    @Test
    void testGetOrderByFields() {

        String sql = "select 部门,用户 from 超音数 where 数据日期 = '2023-08-08'"
                + " and 用户 = 'alice' and 发布日期 ='11' order by 访问次数 limit 1";
        List<String> selectFields = SqlSelectHelper.getOrderByFields(sql);

        Assert.assertEquals(selectFields.contains("访问次数"), true);

        sql = "SELECT user_name FROM 超音数 WHERE sys_imp_date <= '2023-09-03' AND "
                + "sys_imp_date >= '2023-08-04' GROUP BY user_name ORDER BY sum(pv) DESC LIMIT 10 ";
        selectFields = SqlSelectHelper.getOrderByFields(sql);

        Assert.assertEquals(selectFields.contains("pv"), true);
    }

    @Test
    void testGetGroupByFields() {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08'"
                + " and 用户 = 'alice' and 发布日期 ='11' group by 部门 limit 1";
        List<String> selectFields = SqlSelectHelper.getGroupByFields(sql);

        Assert.assertEquals(selectFields.contains("部门"), true);
    }

    @Test
    void testGetHavingExpression() {

        String sql = "SELECT user_name FROM 超音数 WHERE sys_imp_date <= '2023-09-03' AND "
                + "sys_imp_date >= '2023-08-04' GROUP BY user_name HAVING sum(pv) > 1000";
        List<Expression> leftExpressionList = SqlSelectHelper.getHavingExpression(sql);

        Assert.assertEquals(leftExpressionList.get(0).toString(), "sum(pv)");
    }

    @Test
    void testGetAggregateFields() {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08'"
                + " and 用户 = 'alice' and 发布日期 ='11' group by 部门 limit 1";
        List<String> selectFields = SqlSelectHelper.getAggregateFields(sql);
        Assert.assertEquals(selectFields.contains("访问次数"), true);
    }

    @Test
    void testGetTableName() {

        String sql = "select 部门,sum (访问次数) from `超音数` where 数据日期 = '2023-08-08'"
                + " and 用户 = 'alice' and 发布日期 ='11' group by 部门 limit 1";
        String tableName = SqlSelectHelper.getTableName(sql);
        Assert.assertEquals(tableName, "超音数");
    }

    @Test
    void testGetPureSelectFields() {

        String sql = "select TIMESTAMPDIFF(MONTH, 发布日期, '2018-06-01')  from `超音数` "
                + "where 数据日期 = '2023-08-08' and 用户 = 'alice'";
        List<String> selectFields = SqlSelectHelper.gePureSelectFields(sql);
        Assert.assertEquals(selectFields.size(), 0);

        sql = "select 发布日期,数据日期  from `超音数` where " + "数据日期 = '2023-08-08' and 用户 = 'alice'";
        selectFields = SqlSelectHelper.gePureSelectFields(sql);
        Assert.assertEquals(selectFields.size(), 2);

        sql = "select 发布日期,数据日期,TIMESTAMPDIFF(MONTH, 发布日期, '2018-06-01')  from `超音数` where "
                + "数据日期 = '2023-08-08' and 用户 = 'alice'";
        selectFields = SqlSelectHelper.gePureSelectFields(sql);
        Assert.assertEquals(selectFields.size(), 2);
    }

    @Test
    void testHasGroupBy() {
        String sql =
                "WITH DepartmentVisits AS (SELECT 部门, SUM(访问次数) AS pv FROM 超音数数据集 WHERE 数据日期 >= '2024-08-29' "
                        + "AND 数据日期 <= '2024-09-29' GROUP BY 部门) SELECT COUNT(*) FROM DepartmentVisits WHERE pv > 100";
        Boolean hasGroupBy = SqlSelectHelper.hasGroupBy(sql);
        Assert.assertEquals(hasGroupBy, true);
    }
}
