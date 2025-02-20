package com.tencent.supersonic.common.jsqlparser;

import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * SqlParserReplaceHelperTest
 */
class SqlReplaceHelperTest {

    @Test
    void testReplaceAggField() {
        String sql = "SELECT 维度1,sum(播放量) FROM 数据库 "
                + "WHERE (歌手名 = '张三') AND 数据日期 = '2023-11-17' GROUP BY 维度1";
        Map<String, Pair<String, String>> fieldMap = new HashMap<>();
        fieldMap.put("播放量", Pair.of("收听用户数", AggOperatorEnum.COUNT_DISTINCT.name()));
        sql = SqlReplaceHelper.replaceAggFields(sql, fieldMap);
        System.out.println(sql);
        Assert.assertEquals("SELECT 维度1, count(DISTINCT 收听用户数) FROM 数据库 "
                + "WHERE (歌手名 = '张三') AND 数据日期 = '2023-11-17' GROUP BY 维度1", sql);
    }

    @Test
    void testReplaceValue() {

        String replaceSql = "select 歌曲名 from 歌曲库 where datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌手名 = '杰伦' and 数据日期 = '2023-08-09' and 歌曲发布时 = '2023-08-01'"
                + " order by 播放量 desc limit 11";

        Map<String, Map<String, String>> filedNameToValueMap = new HashMap<>();

        Map<String, String> valueMap = new HashMap<>();
        valueMap.put("杰伦", "周杰伦");
        filedNameToValueMap.put("歌手名", valueMap);

        replaceSql = SqlReplaceHelper.replaceValue(replaceSql, filedNameToValueMap);

        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE datediff('day', 发布日期, '2023-08-09') <= 1 AND "
                        + "歌手名 = '周杰伦' AND 数据日期 = '2023-08-09' AND 歌曲发布时 = '2023-08-01' "
                        + "ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);

        replaceSql = "select 歌曲名 from 歌曲库 where datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌手名 = '周杰' and 歌手名 = '林俊' and 歌手名 = '陈' and 数据日期 = '2023-08-09' and 歌曲发布时 = '2023-08-01'"
                + " order by 播放量 desc limit 11";

        Map<String, Map<String, String>> filedNameToValueMap2 = new HashMap<>();

        Map<String, String> valueMap2 = new HashMap<>();
        valueMap2.put("周杰伦", "周杰伦");
        valueMap2.put("林俊杰", "林俊杰");
        valueMap2.put("陈奕迅", "陈奕迅");
        filedNameToValueMap2.put("歌手名", valueMap2);

        replaceSql = SqlReplaceHelper.replaceValue(replaceSql, filedNameToValueMap2, false);

        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE datediff('day', 发布日期, '2023-08-09') <= 1 AND 歌手名 = '周杰伦' "
                        + "AND 歌手名 = '林俊杰' AND 歌手名 = '陈' AND 数据日期 = '2023-08-09' AND 歌曲发布时 = '2023-08-01' ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);

        replaceSql = "select 歌曲名 from 歌曲库 where (datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌手名 = '周杰' and 歌手名 = '林俊' and 歌手名 = '陈' and 歌曲发布时 = '2023-08-01') and 数据日期 = '2023-08-09' "
                + " order by 播放量 desc limit 11";

        replaceSql = SqlReplaceHelper.replaceValue(replaceSql, filedNameToValueMap2, false);

        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE (datediff('day', 发布日期, '2023-08-09') <= 1 AND 歌手名 = '周杰伦' AND "
                        + "歌手名 = '林俊杰' AND 歌手名 = '陈' AND 歌曲发布时 = '2023-08-01') AND 数据日期 = '2023-08-09' ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);

        replaceSql = "select 歌曲名 from 歌曲库 where (datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌手名 = '周杰' and 歌手名 = '林俊' and 歌手名 = '陈' and 歌曲发布时 = '2023-08-01' and 播放量 < ("
                + "select min(播放量) from 歌曲库 where 语种 = '英文' " + ") ) and 数据日期 = '2023-08-09' "
                + " order by 播放量 desc limit 11";

        replaceSql = SqlReplaceHelper.replaceValue(replaceSql, filedNameToValueMap2, false);

        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE (datediff('day', 发布日期, '2023-08-09') <= 1 AND 歌手名 = '周杰伦' AND 歌手名 = '林俊杰' AND "
                        + "歌手名 = '陈' AND 歌曲发布时 = '2023-08-01' AND 播放量 < (SELECT min(播放量) FROM 歌曲库 WHERE 语种 = '英文')) "
                        + "AND 数据日期 = '2023-08-09' ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);

        Map<String, Map<String, String>> filedNameToValueMap3 = new HashMap<>();

        Map<String, String> valueMap3 = new HashMap<>();
        valueMap3.put("周杰伦", "1");
        valueMap3.put("林俊杰", "2");
        valueMap3.put("陈奕迅", "3");
        filedNameToValueMap3.put("歌手名", valueMap3);
        replaceSql = "SELECT 歌曲名 FROM 歌曲库 WHERE  歌手名 in ('周杰伦','林俊杰','陈奕迅') ";
        replaceSql = SqlReplaceHelper.replaceValue(replaceSql, filedNameToValueMap3, true);

        Assert.assertEquals("SELECT 歌曲名 FROM 歌曲库 WHERE 歌手名 IN ('1', '2', '3')", replaceSql);
    }

    @Test
    void testReplaceFieldNameByValue() {

        String replaceSql = "select 歌曲名 from 歌曲库 where datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌曲名 = '邓紫棋' and 数据日期 = '2023-08-09' and 歌曲发布时 = '2023-08-01'"
                + " order by 播放量 desc limit 11";

        Map<String, Set<String>> fieldValueToFieldNames = new HashMap<>();
        fieldValueToFieldNames.put("邓紫棋", Collections.singleton("歌手名"));

        replaceSql = SqlReplaceHelper.replaceFieldNameByValue(replaceSql, fieldValueToFieldNames);

        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE datediff('day', 发布日期, '2023-08-09') <= 1 AND "
                        + "歌手名 = '邓紫棋' AND 数据日期 = '2023-08-09' AND 歌曲发布时 = '2023-08-01' "
                        + "ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);

        replaceSql = "select 歌曲名 from 歌曲库 where datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌曲名 like '%邓紫棋%' and 数据日期 = '2023-08-09' and 歌曲发布时 = '2023-08-01'"
                + " order by 播放量 desc limit 11";

        replaceSql = SqlReplaceHelper.replaceFieldNameByValue(replaceSql, fieldValueToFieldNames);

        Assert.assertEquals("SELECT 歌曲名 FROM 歌曲库 WHERE datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "AND 歌曲名 LIKE '%邓紫棋%' AND 数据日期 = '2023-08-09' AND 歌曲发布时 = "
                + "'2023-08-01' ORDER BY 播放量 DESC LIMIT 11", replaceSql);

        Set<String> fieldNames = new HashSet<>();
        fieldNames.add("歌手名");
        fieldNames.add("歌曲名");
        fieldNames.add("专辑名");

        fieldValueToFieldNames.put("林俊杰", fieldNames);
        replaceSql = "select 歌曲名 from 歌曲库 where datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌手名 = '林俊杰' and 数据日期 = '2023-08-09' and 歌曲发布时 = '2023-08-01'"
                + " order by 播放量 desc limit 11";

        replaceSql = SqlReplaceHelper.replaceFieldNameByValue(replaceSql, fieldValueToFieldNames);

        Assert.assertEquals("SELECT 歌曲名 FROM 歌曲库 WHERE datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "AND 歌手名 = '林俊杰' AND 数据日期 = '2023-08-09' AND 歌曲发布时 = "
                + "'2023-08-01' ORDER BY 播放量 DESC LIMIT 11", replaceSql);

        replaceSql = "select 歌曲名 from 歌曲库 where (datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌手名 = '林俊杰' and 歌曲发布时 = '2023-08-01') and 数据日期 = '2023-08-09'"
                + " order by 播放量 desc limit 11";

        replaceSql = SqlReplaceHelper.replaceFieldNameByValue(replaceSql, fieldValueToFieldNames);

        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE (datediff('day', 发布日期, '2023-08-09') <= 1 AND "
                        + "歌手名 = '林俊杰' AND 歌曲发布时 = '2023-08-01') AND 数据日期 = '2023-08-09' "
                        + "ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);
    }

    @Test
    void testReplaceUnionFields() {
        Map<String, String> fieldToBizName1 = new HashMap<>();
        fieldToBizName1.put("公司成立时间", "company_established_time");
        fieldToBizName1.put("年营业额", "annual_turnover");
        String replaceSql = "SELECT * FROM 互联网企业 ORDER BY 公司成立时间 DESC LIMIT 3 "
                + "UNION SELECT * FROM 互联网企业 ORDER BY 年营业额 DESC LIMIT 5";
        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName1);
        replaceSql = SqlReplaceHelper.replaceTable(replaceSql, "internet");
        Assert.assertEquals(
                "SELECT * FROM internet ORDER BY company_established_time DESC LIMIT 3 "
                        + "UNION SELECT * FROM internet ORDER BY annual_turnover DESC LIMIT 5",
                replaceSql);
    }

    @Test
    void testReplaceFunctionField() {
        Map<String, String> fieldToBizName = initParams();
        String replaceSql =
                "SELECT TIMESTAMPDIFF (MONTH,歌曲发布时间,CURDATE()) AS 发布月数 FROM 歌曲库 WHERE  歌手名 = '邓紫棋' ";

        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);
        Assert.assertEquals("SELECT TIMESTAMPDIFF(MONTH, song_publis_date, CURDATE()) AS 发布月数 "
                + "FROM 歌曲库 WHERE singer_name = '邓紫棋'", replaceSql);
    }

    @Test
    void testReplaceTable() {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        String replaceSql = SqlReplaceHelper.replaceTable(sql, "s2");

        Assert.assertEquals("SELECT 部门, sum(访问次数) FROM s2 WHERE 数据日期 = '2023-08-08' "
                + "AND 用户 = alice AND 发布日期 = '11' GROUP BY 部门 LIMIT 1", replaceSql);

        sql = "select * from 互联网企业 order by 公司成立时间 desc limit 3 union select * from 互联网企业 order by 年营业额 desc limit 5";
        replaceSql = SqlReplaceHelper.replaceTable(sql, "internet");
        Assert.assertEquals("SELECT * FROM internet ORDER BY 公司成立时间 DESC LIMIT 3 "
                + "UNION SELECT * FROM internet ORDER BY 年营业额 DESC LIMIT 5", replaceSql);

        sql = "SELECT * FROM CSpider音乐 WHERE (评分 < (SELECT min(评分) "
                + "FROM CSpider音乐 WHERE 语种 = '英文')) AND 数据日期 = '2023-10-11'";
        replaceSql = SqlReplaceHelper.replaceTable(sql, "cspider");

        Assert.assertEquals("SELECT * FROM cspider WHERE (评分 < (SELECT min(评分) FROM "
                + "cspider WHERE 语种 = '英文')) AND 数据日期 = '2023-10-11'", replaceSql);

        sql = "SELECT 歌曲名称, sum(评分) FROM CSpider音乐 WHERE(1 < 2) AND 数据日期 = '2023-10-15' "
                + "GROUP BY 歌曲名称 HAVING sum(评分) < ( SELECT min(评分) FROM CSpider音乐 WHERE 语种 = '英文')";

        replaceSql = SqlReplaceHelper.replaceTable(sql, "cspider");

        Assert.assertEquals("SELECT 歌曲名称, sum(评分) FROM cspider WHERE (1 < 2) AND 数据日期 = "
                + "'2023-10-15' GROUP BY 歌曲名称 HAVING sum(评分) < (SELECT min(评分) "
                + "FROM cspider WHERE 语种 = '英文')", replaceSql);

        sql = "WITH _部门访问次数_ AS ( SELECT 部门, SUM(访问次数) AS _总访问次数_ FROM 超音数数据集 WHERE 数据日期 >= '2024-07-11'"
                + " AND 数据日期 <= '2024-10-09' GROUP BY 部门 HAVING SUM(访问次数) > 100 ) SELECT 用户, SUM(访问次数) "
                + "AS _访问次数汇总_ FROM 超音数数据集 WHERE 部门 IN ( SELECT 部门 FROM _部门访问次数_ ) AND 数据日期 >= '2024-07-11' "
                + "AND 数据日期 <= '2024-10-09' GROUP BY 用户";

        replaceSql = SqlReplaceHelper.replaceTable(sql, "t_1");

        Assert.assertEquals("WITH _部门访问次数_ AS (SELECT 部门, SUM(访问次数) AS _总访问次数_ FROM t_1 "
                + "WHERE 数据日期 >= '2024-07-11' AND 数据日期 <= '2024-10-09' GROUP BY 部门 HAVING SUM(访问次数) > 100) "
                + "SELECT 用户, SUM(访问次数) AS _访问次数汇总_ FROM t_1 WHERE 部门 IN (SELECT 部门 FROM _部门访问次数_) "
                + "AND 数据日期 >= '2024-07-11' AND 数据日期 <= '2024-10-09' GROUP BY 用户", replaceSql);
    }

    @Test
    void testReplaceFunctionName() {

        String sql = "select 公司名称,平均(注册资本),总部地点 from 互联网企业 where\n"
                + "年营业额 >= 28800000000 and 最大(注册资本)>10000 \n"
                + "  group by  公司名称 having 平均(注册资本)>10000  order by \n" + "平均(注册资本) desc limit 5";
        Map<String, String> map = new HashMap<>();
        map.put("平均", "avg");
        map.put("最大", "max");
        sql = SqlReplaceHelper.replaceFunction(sql, map);
        System.out.println(sql);
        Assert.assertEquals("SELECT 公司名称, avg(注册资本), 总部地点 FROM 互联网企业 WHERE 年营业额 >= 28800000000 AND "
                + "max(注册资本) > 10000 GROUP BY 公司名称 HAVING avg(注册资本) > 10000 ORDER BY avg(注册资本) DESC LIMIT 5",
                sql);

        sql = "select MONTH(数据日期) as 月份, avg(访问次数) as 平均访问次数 from 内容库产品 where"
                + " datediff('month', 数据日期, '2023-09-02') <= 6 group by MONTH(数据日期)";
        Map<String, String> functionMap = new HashMap<>();
        functionMap.put("MONTH".toLowerCase(), "toMonth");
        String replaceSql = SqlReplaceHelper.replaceFunction(sql, functionMap);

        Assert.assertEquals(
                "SELECT toMonth(数据日期) AS 月份, avg(访问次数) AS 平均访问次数 FROM 内容库产品 WHERE"
                        + " datediff('month', 数据日期, '2023-09-02') <= 6 GROUP BY toMonth(数据日期)",
                replaceSql);

        sql = "select month(数据日期) as 月份, avg(访问次数) as 平均访问次数 from 内容库产品 where"
                + " datediff('month', 数据日期, '2023-09-02') <= 6 group by MONTH(数据日期)";
        replaceSql = SqlReplaceHelper.replaceFunction(sql, functionMap);

        Assert.assertEquals(
                "SELECT toMonth(数据日期) AS 月份, avg(访问次数) AS 平均访问次数 FROM 内容库产品 WHERE"
                        + " datediff('month', 数据日期, '2023-09-02') <= 6 GROUP BY toMonth(数据日期)",
                replaceSql);

        sql = "select month(数据日期) as 月份, avg(访问次数) as 平均访问次数 from 内容库产品 where"
                + " (datediff('month', 数据日期, '2023-09-02') <= 6) and 数据日期 = '2023-10-10' group by MONTH(数据日期)";
        replaceSql = SqlReplaceHelper.replaceFunction(sql, functionMap);

        Assert.assertEquals("SELECT toMonth(数据日期) AS 月份, avg(访问次数) AS 平均访问次数 FROM 内容库产品 WHERE"
                + " (datediff('month', 数据日期, '2023-09-02') <= 6) AND "
                + "数据日期 = '2023-10-10' GROUP BY toMonth(数据日期)", replaceSql);
    }

    @Test
    void testReplaceAlias() {
        String sql = "select 部门, sum(访问次数) as 总访问次数 from 超音数 where "
                + "datediff('day', 数据日期, '2023-09-05') <= 3 group by 部门 order by 总访问次数 desc limit 10";
        String replaceSql = SqlReplaceHelper.replaceAlias(sql);
        System.out.println(replaceSql);
        Assert.assertEquals("SELECT 部门, sum(访问次数) FROM 超音数 WHERE "
                + "datediff('day', 数据日期, '2023-09-05') <= 3 GROUP BY 部门 ORDER BY sum(访问次数) DESC LIMIT 10",
                replaceSql);

        sql = "select 部门, sum(访问次数) as 总访问次数 from 超音数 where "
                + "(datediff('day', 数据日期, '2023-09-05') <= 3) and 数据日期 = '2023-10-10' "
                + "group by 部门 order by 总访问次数 desc limit 10";
        replaceSql = SqlReplaceHelper.replaceAlias(sql);
        System.out.println(replaceSql);
        Assert.assertEquals("SELECT 部门, sum(访问次数) FROM 超音数 WHERE "
                + "(datediff('day', 数据日期, '2023-09-05') <= 3) AND 数据日期 = '2023-10-10' "
                + "GROUP BY 部门 ORDER BY sum(访问次数) DESC LIMIT 10", replaceSql);

        sql = "select 部门, sum(访问次数) as 访问次数 from 超音数 where "
                + "(datediff('day', 数据日期, '2023-09-05') <= 3) and 数据日期 = '2023-10-10' "
                + "group by 部门 order by 访问次数 desc limit 10";
        replaceSql = SqlReplaceHelper.replaceAlias(sql);
        System.out.println(replaceSql);
        Assert.assertEquals("SELECT 部门, sum(访问次数) AS 访问次数 FROM 超音数 WHERE (datediff('day', 数据日期, "
                + "'2023-09-05') <= 3) AND 数据日期 = '2023-10-10' GROUP BY 部门 ORDER BY 访问次数 DESC LIMIT 10",
                replaceSql);
    }

    @Test
    void testReplaceAliasWithBackticks() {
        String sql = "SELECT 部门, SUM(访问次数) AS 总访问次数 FROM 超音数 WHERE "
                + "datediff('day', 数据日期, '2023-09-05') <= 3 GROUP BY 部门 ORDER BY 总访问次数 DESC LIMIT 10";
        String replaceSql = SqlReplaceHelper.replaceAliasWithBackticks(sql);
        System.out.println(replaceSql);
        Assert.assertEquals("SELECT 部门, SUM(访问次数) AS `总访问次数` FROM 超音数 WHERE "
                + "datediff('day', 数据日期, '2023-09-05') <= 3 GROUP BY 部门 ORDER BY `总访问次数` DESC LIMIT 10",
                replaceSql);

        sql = "select 部门, sum(访问次数) as 访问次数 from 超音数 where "
                + "(datediff('day', 数据日期, '2023-09-05') <= 3) and 数据日期 = '2023-10-10' "
                + "group by 部门 order by 访问次数 desc limit 10";
        replaceSql = SqlReplaceHelper.replaceAliasWithBackticks(sql);
        System.out.println(replaceSql);
        Assert.assertEquals("SELECT 部门, sum(访问次数) AS `访问次数` FROM 超音数 WHERE (datediff('day', 数据日期, "
                + "'2023-09-05') <= 3) AND 数据日期 = '2023-10-10' GROUP BY 部门 ORDER BY `访问次数` DESC LIMIT 10",
                replaceSql);

        sql = "select 部门, sum(访问次数) as 访问次数, count(部门) as 部门数, count(部门) as 部门数2, 访问次数 from 超音数 where "
                + "(datediff('day', 数据日期, '2023-09-05') <= 3) and 数据日期 = '2023-10-10' "
                + "group by 部门, 部门数, 部门数2 having 访问次数 > 1 AND 部门数2 > 2 AND 部门数 > 1 AND 访问次数 > 1 order by 访问次数 desc  limit 10";
        replaceSql = SqlReplaceHelper.replaceAliasWithBackticks(sql);
        System.out.println(replaceSql);
        Assert.assertEquals(
                "SELECT 部门, sum(访问次数) AS `访问次数`, count(部门) AS `部门数`, count(部门) AS `部门数2`, `访问次数` FROM 超音数 WHERE (datediff('day', 数据日期, "
                        + "'2023-09-05') <= 3) AND 数据日期 = '2023-10-10' GROUP BY 部门, `部门数`, `部门数2` HAVING `访问次数` > 1 AND `部门数2` > 2 AND `部门数` > 1 AND `访问次数` > 1 ORDER BY `访问次数` DESC LIMIT 10",
                replaceSql);

    }

    @Test
    void testReplaceAliasFieldName() {
        Map<String, String> map = new HashMap<>();
        map.put("总访问次数", "\"总访问次数\"");
        map.put("访问次数", "\"访问次数\"");
        String sql = "select 部门, sum(访问次数) as 总访问次数 from 超音数 where "
                + "datediff('day', 数据日期, '2023-09-05') <= 3 group by 部门 order by 总访问次数 desc limit 10";
        String replaceSql = SqlReplaceHelper.replaceAliasFieldName(sql, map);
        System.out.println(replaceSql);
        Assert.assertEquals("SELECT 部门, sum(访问次数) AS \"总访问次数\" FROM 超音数 WHERE "
                + "datediff('day', 数据日期, '2023-09-05') <= 3 GROUP BY 部门 ORDER BY \"总访问次数\" DESC LIMIT 10",
                replaceSql);

        sql = "select 部门, sum(访问次数) as 总访问次数 from 超音数 where "
                + "(datediff('day', 数据日期, '2023-09-05') <= 3) and 数据日期 = '2023-10-10' "
                + "group by 部门 order by 总访问次数 desc limit 10";
        replaceSql = SqlReplaceHelper.replaceAliasFieldName(sql, map);
        System.out.println(replaceSql);
        Assert.assertEquals("SELECT 部门, sum(访问次数) AS \"总访问次数\" FROM 超音数 WHERE "
                + "(datediff('day', 数据日期, '2023-09-05') <= 3) AND 数据日期 = '2023-10-10' "
                + "GROUP BY 部门 ORDER BY \"总访问次数\" DESC LIMIT 10", replaceSql);

        sql = "select 部门, sum(访问次数) as 访问次数 from 超音数 where "
                + "(datediff('day', 数据日期, '2023-09-05') <= 3) and 数据日期 = '2023-10-10' "
                + "group by 部门 order by 访问次数 desc limit 10";
        replaceSql = SqlReplaceHelper.replaceAliasFieldName(sql, map);
        System.out.println(replaceSql);
        Assert.assertEquals(
                "SELECT 部门, sum(\"访问次数\") AS \"访问次数\" FROM 超音数 WHERE (datediff('day', 数据日期, "
                        + "'2023-09-05') <= 3) AND 数据日期 = '2023-10-10' GROUP BY 部门 ORDER BY \"访问次数\" DESC LIMIT 10",
                replaceSql);
    }

    @Test
    void testReplaceAggAliasOrderbyField() {
        String sql = "SELECT SUM(访问次数) AS top10总播放量 FROM (SELECT 部门, SUM(访问次数) AS 访问次数 FROM 超音数  "
                + "GROUP BY 部门 ORDER BY SUM(访问次数) DESC LIMIT 10) AS top10";
        String replaceSql = SqlReplaceHelper.replaceAggAliasOrderbyField(sql);
        Assert.assertEquals(
                "SELECT SUM(访问次数) AS top10总播放量 FROM (SELECT 部门, SUM(访问次数) AS 访问次数 FROM 超音数 "
                        + "GROUP BY 部门 ORDER BY 2 DESC LIMIT 10) AS top10",
                replaceSql);
    }

    protected Map<String, String> initParams() {
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
        fieldToBizName.put("访问次数", "pv");
        fieldToBizName.put("粉丝数", "fans_cnt");
        return fieldToBizName;
    }

}
