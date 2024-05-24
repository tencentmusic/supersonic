package com.tencent.supersonic.common.util.jsqlparser;

import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * SqlParserReplaceHelperTest
 */
class SqlReplaceHelperTest {

    @Test
    void testReplaceSelectField() {

        String sql = "SELECT 维度1,sum(播放量) FROM 数据库 "
                + "WHERE (歌手名 = '张三') AND 数据日期 = '2023-11-17' GROUP BY 维度1";
        Map<String, String> fieldMap = new HashMap<>();
        fieldMap.put("播放量", "播放量1");
        sql = SqlReplaceHelper.replaceSelectFields(sql, fieldMap);
        System.out.println(sql);
        Assert.assertEquals("SELECT 维度1, sum(播放量1) FROM 数据库 "
                + "WHERE (歌手名 = '张三') AND 数据日期 = '2023-11-17' GROUP BY 维度1", sql);

        sql = "SELECT 维度1,播放量 FROM 数据库 "
                + "WHERE (歌手名 = '张三') AND 数据日期 = '2023-11-17' GROUP BY 维度1";
        fieldMap = new HashMap<>();
        fieldMap.put("播放量", "播放量1");
        sql = SqlReplaceHelper.replaceSelectFields(sql, fieldMap);
        System.out.println(sql);
        Assert.assertEquals("SELECT 维度1, 播放量1 FROM 数据库 WHERE (歌手名 = '张三') AND 数据日期 = '2023-11-17' GROUP BY 维度1", sql);
    }

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
                        + "ORDER BY 播放量 DESC LIMIT 11", replaceSql);

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
                        + "AND 歌手名 = '林俊杰' AND 歌手名 = '陈奕迅' AND 数据日期 = '2023-08-09' AND "
                        + "歌曲发布时 = '2023-08-01' ORDER BY 播放量 DESC LIMIT 11", replaceSql);

        replaceSql = "select 歌曲名 from 歌曲库 where (datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌手名 = '周杰' and 歌手名 = '林俊' and 歌手名 = '陈' and 歌曲发布时 = '2023-08-01') and 数据日期 = '2023-08-09' "
                + " order by 播放量 desc limit 11";

        replaceSql = SqlReplaceHelper.replaceValue(replaceSql, filedNameToValueMap2, false);

        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE (datediff('day', 发布日期, '2023-08-09') <= 1 AND 歌手名 = '周杰伦' "
                        + "AND 歌手名 = '林俊杰' AND 歌手名 = '陈奕迅' AND 歌曲发布时 = '2023-08-01') "
                        + "AND 数据日期 = '2023-08-09' ORDER BY 播放量 DESC LIMIT 11", replaceSql);

        replaceSql = "select 歌曲名 from 歌曲库 where (datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌手名 = '周杰' and 歌手名 = '林俊' and 歌手名 = '陈' and 歌曲发布时 = '2023-08-01' and 播放量 < ("
                + "select min(播放量) from 歌曲库 where 语种 = '英文' "
                + ") ) and 数据日期 = '2023-08-09' "
                + " order by 播放量 desc limit 11";

        replaceSql = SqlReplaceHelper.replaceValue(replaceSql, filedNameToValueMap2, false);

        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE (datediff('day', 发布日期, '2023-08-09') <= 1 AND "
                        + "歌手名 = '周杰伦' AND 歌手名 = '林俊杰' AND 歌手名 = '陈奕迅' AND 歌曲发布时 = '2023-08-01' "
                        + "AND 播放量 < (SELECT min(播放量) FROM 歌曲库 WHERE 语种 = '英文')) AND 数据日期 = '2023-08-09' "
                        + "ORDER BY 播放量 DESC LIMIT 11", replaceSql);
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
                        + "ORDER BY 播放量 DESC LIMIT 11", replaceSql);

        replaceSql = "select 歌曲名 from 歌曲库 where datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌曲名 like '%邓紫棋%' and 数据日期 = '2023-08-09' and 歌曲发布时 = '2023-08-01'"
                + " order by 播放量 desc limit 11";

        replaceSql = SqlReplaceHelper.replaceFieldNameByValue(replaceSql, fieldValueToFieldNames);

        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE datediff('day', 发布日期, '2023-08-09') <= 1 "
                        + "AND 歌手名 LIKE '邓紫棋' AND 数据日期 = '2023-08-09' AND 歌曲发布时 = "
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

        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE datediff('day', 发布日期, '2023-08-09') <= 1 "
                        + "AND 歌手名 = '林俊杰' AND 数据日期 = '2023-08-09' AND 歌曲发布时 = "
                        + "'2023-08-01' ORDER BY 播放量 DESC LIMIT 11", replaceSql);

        replaceSql = "select 歌曲名 from 歌曲库 where (datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌手名 = '林俊杰' and 歌曲发布时 = '2023-08-01') and 数据日期 = '2023-08-09'"
                + " order by 播放量 desc limit 11";

        replaceSql = SqlReplaceHelper.replaceFieldNameByValue(replaceSql, fieldValueToFieldNames);

        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE (datediff('day', 发布日期, '2023-08-09') <= 1 AND "
                        + "歌手名 = '林俊杰' AND 歌曲发布时 = '2023-08-01') AND 数据日期 = '2023-08-09' "
                        + "ORDER BY 播放量 DESC LIMIT 11", replaceSql);

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
                        + "UNION SELECT * FROM internet ORDER BY annual_turnover DESC LIMIT 5", replaceSql);
    }

    @Test
    void testReplaceFields() {

        Map<String, String> fieldToBizName = initParams();
        String replaceSql = "select 歌曲名 from 歌曲库 where datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌手名 = '邓紫棋' and 数据日期 = '2023-08-09' and 歌曲发布时 = '2023-08-01'"
                + " order by 播放量 desc limit 11";

        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);
        Assert.assertEquals(
                "SELECT song_name FROM 歌曲库 WHERE (publish_date >= '2023-08-08' AND publish_date <= '2023-08-09')"
                        + " AND singer_name = '邓紫棋' AND sys_imp_date = '2023-08-09' AND song_publis_date = '2023-08-01'"
                        + " ORDER BY play_count DESC LIMIT 11", replaceSql);

        replaceSql = "select 品牌名称 from 互联网企业 where datediff('year', 品牌成立时间, '2023-11-04') > 17 and 注册资本 = 50000000";
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);
        replaceSql = SqlRemoveHelper.removeNumberFilter(replaceSql);
        Assert.assertEquals(
                "SELECT 品牌名称 FROM 互联网企业 WHERE 品牌成立时间 < '2006-11-04' AND 注册资本 = 50000000", replaceSql);

        replaceSql = "select MONTH(数据日期), sum(访问次数) from 内容库产品 "
                + "where datediff('year', 数据日期, '2023-09-03') <= 0.5 "
                + "group by MONTH(数据日期) order by sum(访问次数) desc limit 1";

        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT MONTH(sys_imp_date), sum(pv) FROM 内容库产品 WHERE (sys_imp_date >= '2023-03-03' "
                        + "AND sys_imp_date <= '2023-09-03')"
                        + " GROUP BY MONTH(sys_imp_date) ORDER BY sum(pv) DESC LIMIT 1", replaceSql);

        replaceSql = "select MONTH(数据日期), sum(访问次数) from 内容库产品 "
                + "where datediff('year', 数据日期, '2023-09-03') <= 0.5 "
                + "group by MONTH(数据日期) HAVING sum(访问次数) > 1000";

        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT MONTH(sys_imp_date), sum(pv) FROM 内容库产品 WHERE (sys_imp_date >= '2023-03-03' AND"
                        + " sys_imp_date <= '2023-09-03') GROUP BY MONTH(sys_imp_date) HAVING sum(pv) > 1000",
                replaceSql);

        replaceSql = "select YEAR(发行日期), count(歌曲名) from 歌曲库 where YEAR(发行日期) "
                + "in (2022, 2023) and 数据日期 = '2023-08-14' group by YEAR(发行日期)";

        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT YEAR(publish_date), count(song_name) FROM 歌曲库 "
                        + "WHERE YEAR(publish_date) IN (2022, 2023) AND sys_imp_date = '2023-08-14' "
                        + "GROUP BY YEAR(publish_date)",
                replaceSql);

        replaceSql = "select YEAR(发行日期), count(歌曲名) from 歌曲库 "
                + "where YEAR(发行日期) in (2022, 2023) and 数据日期 = '2023-08-14' "
                + "group by 发行日期";

        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT YEAR(publish_date), count(song_name) FROM 歌曲库 "
                        + "WHERE YEAR(publish_date) IN (2022, 2023) AND sys_imp_date = '2023-08-14'"
                        + " GROUP BY publish_date",
                replaceSql);

        replaceSql = SqlReplaceHelper.replaceFields(
                "select 歌曲名 from 歌曲库 where datediff('year', 发布日期, '2023-08-11') <= 1 "
                        + "and 结算播放量 > 1000000 and datediff('day', 数据日期, '2023-08-11') <= 30",
                fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT song_name FROM 歌曲库 WHERE (publish_date >= '2022-08-11' "
                        + "AND publish_date <= '2023-08-11') AND play_count > 1000000 AND "
                        + "(sys_imp_date >= '2023-07-12' AND sys_imp_date <= '2023-08-11')",
                replaceSql);

        replaceSql = SqlReplaceHelper.replaceFields(
                "select 歌曲名 from 歌曲库 where datediff('day', 发布日期, '2023-08-09') <= 1 "
                        + "and 歌手名 = '邓紫棋' and 数据日期 = '2023-08-09' order by 播放量 desc limit 11",
                fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT song_name FROM 歌曲库 WHERE (publish_date >= '2023-08-08' AND publish_date <= '2023-08-09')"
                        + " AND singer_name = '邓紫棋' AND sys_imp_date = '2023-08-09' ORDER BY play_count DESC LIMIT 11",
                replaceSql);

        replaceSql = SqlReplaceHelper.replaceFields(
                "select 歌曲名 from 歌曲库 where datediff('year', 发布日期, '2023-08-09') = 0 "
                        + "and 歌手名 = '邓紫棋' and 数据日期 = '2023-08-09' order by 播放量 desc limit 11", fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT song_name FROM 歌曲库 WHERE (publish_date >= '2023-01-01' AND publish_date <= '2023-08-09')"
                        + " AND singer_name = '邓紫棋' AND sys_imp_date = '2023-08-09' ORDER BY play_count DESC LIMIT 11",
                replaceSql);

        replaceSql = SqlReplaceHelper.replaceFields(
                "select 歌曲名 from 歌曲库 where datediff('year', 发布日期, '2023-08-09') <= 0.5 "
                        + "and 歌手名 = '邓紫棋' and 数据日期 = '2023-08-09' order by 播放量 desc limit 11", fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT song_name FROM 歌曲库 WHERE (publish_date >= '2023-02-09' AND publish_date <= '2023-08-09')"
                        + " AND singer_name = '邓紫棋' AND sys_imp_date = '2023-08-09' ORDER BY play_count DESC LIMIT 11",
                replaceSql);

        replaceSql = SqlReplaceHelper.replaceFields(
                "select 歌曲名 from 歌曲库 where datediff('year', 发布日期, '2023-08-09') >= 0.5 "
                        + "and 歌手名 = '邓紫棋' and 数据日期 = '2023-08-09' order by 播放量 desc limit 11", fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);
        replaceSql = SqlRemoveHelper.removeNumberFilter(replaceSql);
        Assert.assertEquals(
                "SELECT song_name FROM 歌曲库 WHERE publish_date <= '2023-02-09' AND"
                        + " singer_name = '邓紫棋' AND sys_imp_date = '2023-08-09'"
                        + " ORDER BY play_count DESC LIMIT 11", replaceSql);

        replaceSql = SqlReplaceHelper.replaceFields(
                "select 部门,用户 from 超音数 where 数据日期 = '2023-08-08' and 用户 ='alice'"
                        + " and 发布日期 ='11' order by 访问次数 desc limit 1", fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT department, user_id FROM 超音数 WHERE sys_imp_date = '2023-08-08'"
                        + " AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1", replaceSql);

        replaceSql = SqlReplaceHelper.replaceTable(replaceSql, "s2");

        replaceSql = SqlAddHelper.addFieldsToSelect(replaceSql, Collections.singletonList("field_a"));

        replaceSql = SqlReplaceHelper.replaceFields(
                "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                        + "and 用户 ='alice' and 发布日期 ='11' group by 部门 limit 1", fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT department, sum(pv) FROM 超音数 WHERE sys_imp_date = '2023-08-08'"
                        + " AND user_id = 'alice' AND publish_date = '11' GROUP BY department LIMIT 1", replaceSql);

        replaceSql = "select sum(访问次数) from 超音数 where 数据日期 >= '2023-08-06' "
                + "and 数据日期 <= '2023-08-06' and 部门 = 'hr'";
        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT sum(pv) FROM 超音数 WHERE sys_imp_date >= '2023-08-06' "
                        + "AND sys_imp_date <= '2023-08-06' AND department = 'hr'", replaceSql);

        replaceSql = "SELECT 歌曲名称, sum(评分) FROM CSpider WHERE(1 < 2) AND 数据日期 = '2023-10-15' "
                + "GROUP BY 歌曲名称 HAVING sum(评分) < ( SELECT min(评分) FROM CSpider WHERE 语种 = '英文')";
        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT song_name, sum(user_id) FROM CSpider WHERE (1 < 2) AND "
                        + "sys_imp_date = '2023-10-15' GROUP BY song_name HAVING "
                        + "sum(user_id) < (SELECT min(user_id) FROM CSpider WHERE user_id = '英文')", replaceSql);

        replaceSql = "SELECT sum(评分)/ (SELECT sum(评分) FROM CSpider WHERE  数据日期 = '2023-10-15')"
                + " FROM CSpider WHERE  数据日期 = '2023-10-15' "
                + "GROUP BY 歌曲名称 HAVING sum(评分) < ( SELECT min(评分) FROM CSpider WHERE 语种 = '英文')";
        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT sum(user_id) / (SELECT sum(user_id) FROM CSpider WHERE sys_imp_date = '2023-10-15') "
                        + "FROM CSpider WHERE sys_imp_date = '2023-10-15' GROUP BY song_name HAVING "
                        + "sum(user_id) < (SELECT min(user_id) FROM CSpider WHERE user_id = '英文')", replaceSql);
    }

    @Test
    void testReplaceTable() {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        String replaceSql = SqlReplaceHelper.replaceTable(sql, "s2");

        Assert.assertEquals(
                "SELECT 部门, sum(访问次数) FROM s2 WHERE 数据日期 = '2023-08-08' "
                        + "AND 用户 = alice AND 发布日期 = '11' GROUP BY 部门 LIMIT 1", replaceSql);

        sql = "select * from 互联网企业 order by 公司成立时间 desc limit 3 union select * from 互联网企业 order by 年营业额 desc limit 5";
        replaceSql = SqlReplaceHelper.replaceTable(sql, "internet");
        Assert.assertEquals(
                "SELECT * FROM internet ORDER BY 公司成立时间 DESC LIMIT 3 "
                        + "UNION SELECT * FROM internet ORDER BY 年营业额 DESC LIMIT 5", replaceSql);

        sql = "SELECT * FROM CSpider音乐 WHERE (评分 < (SELECT min(评分) "
                + "FROM CSpider音乐 WHERE 语种 = '英文')) AND 数据日期 = '2023-10-11'";
        replaceSql = SqlReplaceHelper.replaceTable(sql, "cspider");

        Assert.assertEquals(
                "SELECT * FROM cspider WHERE (评分 < (SELECT min(评分) FROM "
                        + "cspider WHERE 语种 = '英文')) AND 数据日期 = '2023-10-11'", replaceSql);

        sql = "SELECT 歌曲名称, sum(评分) FROM CSpider音乐 WHERE(1 < 2) AND 数据日期 = '2023-10-15' "
                + "GROUP BY 歌曲名称 HAVING sum(评分) < ( SELECT min(评分) FROM CSpider音乐 WHERE 语种 = '英文')";

        replaceSql = SqlReplaceHelper.replaceTable(sql, "cspider");

        Assert.assertEquals(
                "SELECT 歌曲名称, sum(评分) FROM cspider WHERE (1 < 2) AND 数据日期 = "
                        + "'2023-10-15' GROUP BY 歌曲名称 HAVING sum(评分) < (SELECT min(评分) "
                        + "FROM cspider WHERE 语种 = '英文')",
                replaceSql);
    }

    @Test
    void testReplaceFunctionName() {

        String sql = "select 公司名称,平均(注册资本),总部地点 from 互联网企业 where\n"
                + "年营业额 >= 28800000000 and 最大(注册资本)>10000 \n"
                + "  group by  公司名称 having 平均(注册资本)>10000  order by \n"
                + "平均(注册资本) desc limit 5";
        Map<String, String> map = new HashMap<>();
        map.put("平均", "avg");
        map.put("最大", "max");
        sql = SqlReplaceHelper.replaceFunction(sql, map);
        System.out.println(sql);
        Assert.assertEquals("SELECT 公司名称, avg(注册资本), 总部地点 FROM 互联网企业 WHERE 年营业额 >= 28800000000 AND "
                + "max(注册资本) > 10000 GROUP BY 公司名称 HAVING avg(注册资本) > 10000 ORDER BY avg(注册资本) DESC LIMIT 5", sql);

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

        Assert.assertEquals(
                "SELECT toMonth(数据日期) AS 月份, avg(访问次数) AS 平均访问次数 FROM 内容库产品 WHERE"
                        + " (datediff('month', 数据日期, '2023-09-02') <= 6) AND "
                        + "数据日期 = '2023-10-10' GROUP BY toMonth(数据日期)",
                replaceSql);
    }

    @Test
    void testReplaceAlias() {
        String sql = "select 部门, sum(访问次数) as 总访问次数 from 超音数 where "
                + "datediff('day', 数据日期, '2023-09-05') <= 3 group by 部门 order by 总访问次数 desc limit 10";
        String replaceSql = SqlReplaceHelper.replaceAlias(sql);
        System.out.println(replaceSql);
        Assert.assertEquals(
                "SELECT 部门, sum(访问次数) FROM 超音数 WHERE "
                        + "datediff('day', 数据日期, '2023-09-05') <= 3 GROUP BY 部门 ORDER BY sum(访问次数) DESC LIMIT 10",
                replaceSql);

        sql = "select 部门, sum(访问次数) as 总访问次数 from 超音数 where "
                + "(datediff('day', 数据日期, '2023-09-05') <= 3) and 数据日期 = '2023-10-10' "
                + "group by 部门 order by 总访问次数 desc limit 10";
        replaceSql = SqlReplaceHelper.replaceAlias(sql);
        System.out.println(replaceSql);
        Assert.assertEquals(
                "SELECT 部门, sum(访问次数) FROM 超音数 WHERE "
                        + "(datediff('day', 数据日期, '2023-09-05') <= 3) AND 数据日期 = '2023-10-10' "
                        + "GROUP BY 部门 ORDER BY sum(访问次数) DESC LIMIT 10",
                replaceSql);

        sql = "select 部门, sum(访问次数) as 访问次数 from 超音数 where "
                + "(datediff('day', 数据日期, '2023-09-05') <= 3) and 数据日期 = '2023-10-10' "
                + "group by 部门 order by 访问次数 desc limit 10";
        replaceSql = SqlReplaceHelper.replaceAlias(sql);
        System.out.println(replaceSql);
        Assert.assertEquals(
                "SELECT 部门, sum(访问次数) AS 访问次数 FROM 超音数 WHERE (datediff('day', 数据日期, "
                        + "'2023-09-05') <= 3) AND 数据日期 = '2023-10-10' GROUP BY 部门 ORDER BY 访问次数 DESC LIMIT 10",
                replaceSql);

    }

    @Test
    void testReplaceAggAliasOrderItem() {
        String sql = "SELECT SUM(访问次数) AS top10总播放量 FROM (SELECT 部门, SUM(访问次数) AS 访问次数 FROM 超音数  "
                + "GROUP BY 部门 ORDER BY SUM(访问次数) DESC LIMIT 10) AS top10";
        String replaceSql = SqlReplaceHelper.replaceAggAliasOrderItem(sql);
        Assert.assertEquals(
                "SELECT SUM(访问次数) AS top10总播放量 FROM (SELECT 部门, SUM(访问次数) AS 访问次数 FROM 超音数 "
                        + "GROUP BY 部门 ORDER BY 2 DESC LIMIT 10) AS top10",
                replaceSql);
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
        fieldToBizName.put("访问次数", "pv");
        return fieldToBizName;
    }
}
