package com.tencent.supersonic.common.util.jsqlparser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * CCJSqlParserUtils Test
 */
class CCJSqlParserUtilsTest {

    @Test
    void replaceFields() {

        Map<String, String> fieldToBizName = initParams();
        String replaceSql = "select 歌曲名 from 歌曲库 where datediff('day', 发布日期, '2023-08-09') <= 1 and 歌手名 = '邓紫棋' and 数据日期 = '2023-08-09' and 歌曲发布时 = '2023-08-01' order by 播放量 desc limit 11";

        replaceSql = CCJSqlParserUtils.replaceFields(replaceSql, fieldToBizName);

        replaceSql = CCJSqlParserUtils.replaceFields(
                "select 歌曲名 from 歌曲库 where datediff('day', 发布日期, '2023-08-09') <= 1 and 歌手名 = '邓紫棋' and 数据日期 = '2023-08-09' order by 播放量 desc limit 11",
                fieldToBizName);

        replaceSql = CCJSqlParserUtils.replaceFields(
                "select 歌曲名 from 歌曲库 where datediff('year', 发布日期, '2023-08-09') <= 0.5 and 歌手名 = '邓紫棋' and 数据日期 = '2023-08-09' order by 播放量 desc limit 11",
                fieldToBizName);

        replaceSql = CCJSqlParserUtils.replaceFields(
                "select 歌曲名 from 歌曲库 where datediff('year', 发布日期, '2023-08-09') >= 0.5 and 歌手名 = '邓紫棋' and 数据日期 = '2023-08-09' order by 播放量 desc limit 11",
                fieldToBizName);

        replaceSql = CCJSqlParserUtils.replaceFields(
                "select 部门,用户 from 超音数 where 数据日期 = '2023-08-08' and 用户 =alice and 发布日期 ='11' order by 访问次数 desc limit 1",
                fieldToBizName);

        replaceSql = CCJSqlParserUtils.replaceTable(replaceSql, "s2");

        replaceSql = CCJSqlParserUtils.addFieldsToSelect(replaceSql, Collections.singletonList("field_a"));

        replaceSql = CCJSqlParserUtils.replaceFields(
                "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' and 用户 =alice and 发布日期 ='11' group by 部门 limit 1",
                fieldToBizName);
        Assert.assertEquals(replaceSql,
                "SELECT department, sum(pv) FROM 超音数 WHERE sys_imp_date = '2023-08-08' AND user_id = user_id AND publish_date = '11' GROUP BY department LIMIT 1");

        replaceSql = "select sum(访问次数) from 超音数 where 数据日期 >= '2023-08-06' and 数据日期 <= '2023-08-06' and 部门 = 'hr'";
        replaceSql = CCJSqlParserUtils.replaceFields(replaceSql, fieldToBizName);

        System.out.println(replaceSql);
    }


    @Test
    void getAllFields() {

        List<String> allFields = CCJSqlParserUtils.getAllFields(
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08' AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1");

        Assert.assertEquals(allFields.size(), 6);

        allFields = CCJSqlParserUtils.getAllFields(
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date >= '2023-08-08' AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1");

        Assert.assertEquals(allFields.size(), 6);

        allFields = CCJSqlParserUtils.getAllFields(
                "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' and 用户 = 'alice' and 发布日期 ='11' group by 部门 limit 1");

        Assert.assertEquals(allFields.size(), 5);
    }

    @Test
    void replaceTable() {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        String replaceSql = CCJSqlParserUtils.replaceTable(sql, "s2");

        Assert.assertEquals(
                "SELECT 部门, sum(访问次数) FROM s2 WHERE 数据日期 = '2023-08-08' AND 用户 = alice AND 发布日期 = '11' GROUP BY 部门 LIMIT 1",
                replaceSql);
    }


    @Test
    void getSelectFields() {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        List<String> selectFields = CCJSqlParserUtils.getSelectFields(sql);

        Assert.assertEquals(selectFields.contains("访问次数"), true);
        Assert.assertEquals(selectFields.contains("部门"), true);
    }

    @Test
    void getWhereFields() {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' and 用户 = 'alice' and 发布日期 ='11' group by 部门 limit 1";
        List<String> selectFields = CCJSqlParserUtils.getWhereFields(sql);

        Assert.assertEquals(selectFields.contains("发布日期"), true);
        Assert.assertEquals(selectFields.contains("数据日期"), true);
        Assert.assertEquals(selectFields.contains("用户"), true);
    }


    @Test
    void addWhere() throws JSQLParserException {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        sql = CCJSqlParserUtils.addWhere(sql, "column_a", 123444555);
        List<String> selectFields = CCJSqlParserUtils.getAllFields(sql);

        Assert.assertEquals(selectFields.contains("column_a"), true);

        sql = CCJSqlParserUtils.addWhere(sql, "column_b", "123456666");
        selectFields = CCJSqlParserUtils.getAllFields(sql);

        Assert.assertEquals(selectFields.contains("column_b"), true);

        Expression expression = CCJSqlParserUtil.parseCondExpression(" ( column_c = 111  or column_d = 1111)");

        sql = CCJSqlParserUtils.addWhere(
                "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' and 用户 =alice and 发布日期 ='11' group by 部门 limit 1",
                expression);

        Assert.assertEquals(sql.contains("column_c = 111"), true);

    }


    @Test
    void hasAggregateFunction() throws JSQLParserException {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        boolean hasAggregateFunction = CCJSqlParserUtils.hasAggregateFunction(sql);

        Assert.assertEquals(hasAggregateFunction, true);
        sql = "select 部门,count (访问次数) from 超音数 where 数据日期 = '2023-08-08' and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        hasAggregateFunction = CCJSqlParserUtils.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, true);

        sql = "SELECT count(1) FROM s2 WHERE sys_imp_date = '2023-08-08' AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1";
        hasAggregateFunction = CCJSqlParserUtils.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, true);

        sql = "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08' AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1";
        hasAggregateFunction = CCJSqlParserUtils.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, false);

        sql = "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08' AND user_id = 'alice' AND publish_date = '11'";
        hasAggregateFunction = CCJSqlParserUtils.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, false);

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