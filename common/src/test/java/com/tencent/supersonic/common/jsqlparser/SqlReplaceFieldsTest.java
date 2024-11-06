package com.tencent.supersonic.common.jsqlparser;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.Map;

class SqlReplaceFieldsTest extends SqlReplaceHelperTest {
    Map<String, String> fieldToBizName = initParams();

    @Test
    void testReplaceFields1() {

        String replaceSql = "select 歌曲名 from 歌曲库 where datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌手名 = '邓紫棋' and 数据日期 = '2023-08-09' and 歌曲发布时 = '2023-08-01'"
                + " order by 播放量 desc limit 11";

        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);
        Assert.assertEquals(
                "SELECT song_name FROM 歌曲库 WHERE (publish_date >= '2023-08-08' AND publish_date "
                        + "<= '2023-08-09') AND singer_name = '邓紫棋' AND sys_imp_date = '2023-08-09' AND "
                        + "歌曲发布时 = '2023-08-01' ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);
    }

    @Test
    void testReplaceFields2() {
        String replaceSql =
                "select 品牌名称 from 互联网企业 where datediff('year', 品牌成立时间, '2023-11-04') > 17 and 注册资本 = 50000000";
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);
        replaceSql = SqlRemoveHelper.removeNumberFilter(replaceSql);
        Assert.assertEquals(
                "SELECT 品牌名称 FROM 互联网企业 WHERE 品牌成立时间 < '2006-11-04' AND 注册资本 = 50000000",
                replaceSql);
    }

    @Test
    void testReplaceFields3() {

        String replaceSql = "select MONTH(数据日期), sum(访问次数) from 内容库产品 "
                + "where datediff('year', 数据日期, '2023-09-03') <= 0.5 "
                + "group by MONTH(数据日期) order by sum(访问次数) desc limit 1";

        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT MONTH(sys_imp_date), sum(pv) FROM 内容库产品 WHERE (sys_imp_date >= '2023-03-03' "
                        + "AND sys_imp_date <= '2023-09-03')"
                        + " GROUP BY MONTH(sys_imp_date) ORDER BY sum(pv) DESC LIMIT 1",
                replaceSql);
    }

    @Test
    void testReplaceFields4() {

        String replaceSql = "select MONTH(数据日期), sum(访问次数) from 内容库产品 "
                + "where datediff('year', 数据日期, '2023-09-03') <= 0.5 "
                + "group by MONTH(数据日期) HAVING sum(访问次数) > 1000";

        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT MONTH(sys_imp_date), sum(pv) FROM 内容库产品 WHERE (sys_imp_date >= '2023-03-03' AND"
                        + " sys_imp_date <= '2023-09-03') GROUP BY MONTH(sys_imp_date) HAVING sum(pv) > 1000",
                replaceSql);
    }

    @Test
    void testReplaceFields5() {

        String replaceSql = "select YEAR(发行日期), count(歌曲名) from 歌曲库 where YEAR(发行日期) "
                + "in (2022, 2023) and 数据日期 = '2023-08-14' group by YEAR(发行日期)";

        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals("SELECT YEAR(发行日期), count(song_name) FROM 歌曲库 WHERE "
                + "YEAR(发行日期) IN (2022, 2023) AND sys_imp_date = '2023-08-14' GROUP BY YEAR(发行日期)",
                replaceSql);
    }

    @Test
    void testReplaceFields6() {

        String replaceSql = "select YEAR(发行日期), count(歌曲名) from 歌曲库 "
                + "where YEAR(发行日期) in (2022, 2023) and 数据日期 = '2023-08-14' " + "group by 发行日期";

        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT YEAR(发行日期), count(song_name) FROM 歌曲库 WHERE YEAR(发行日期) "
                        + "IN (2022, 2023) AND sys_imp_date = '2023-08-14' GROUP BY 发行日期",
                replaceSql);

    }

    @Test
    void testReplaceFields7() {

        String replaceSql = SqlReplaceHelper.replaceFields(
                "select 歌曲名 from 歌曲库 where datediff('year', 发布日期, '2023-08-11') <= 1 "
                        + "and 结算播放量 > 1000000 and datediff('day', 数据日期, '2023-08-11') <= 30",
                fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT song_name FROM 歌曲库 WHERE (publish_date >= '2022-08-11' AND publish_date <= '2023-08-11')"
                        + " AND 结算播放量 > 1000000 AND (sys_imp_date >= '2023-07-12' AND sys_imp_date <= '2023-08-11')",
                replaceSql);
    }

    @Test
    void testReplaceFields8() {

        String replaceSql = SqlReplaceHelper.replaceFields(
                "select 歌曲名 from 歌曲库 where datediff('day', 发布日期, '2023-08-09') <= 1 "
                        + "and 歌手名 = '邓紫棋' and 数据日期 = '2023-08-09' order by 播放量 desc limit 11",
                fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT song_name FROM 歌曲库 WHERE (publish_date >= '2023-08-08' AND publish_date "
                        + "<= '2023-08-09') AND singer_name = '邓紫棋' AND sys_imp_date = '2023-08-09' ORDER BY "
                        + "播放量 DESC LIMIT 11",
                replaceSql);
    }

    @Test
    void testReplaceFields9() {

        String replaceSql = SqlReplaceHelper.replaceFields(
                "select 歌曲名 from 歌曲库 where datediff('year', 发布日期, '2023-08-09') = 0 "
                        + "and 歌手名 = '邓紫棋' and 数据日期 = '2023-08-09' order by 播放量 desc limit 11",
                fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT song_name FROM 歌曲库 WHERE (publish_date >= '2023-01-01' AND publish_date "
                        + "<= '2023-08-09') AND singer_name = '邓紫棋' AND sys_imp_date = '2023-08-09' "
                        + "ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);
    }

    @Test
    void testReplaceFields10() {

        String replaceSql = SqlReplaceHelper.replaceFields(
                "select 歌曲名 from 歌曲库 where datediff('year', 发布日期, '2023-08-09') <= 0.5 "
                        + "and 歌手名 = '邓紫棋' and 数据日期 = '2023-08-09' order by 播放量 desc limit 11",
                fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT song_name FROM 歌曲库 WHERE (publish_date >= '2023-02-09' AND publish_date <="
                        + " '2023-08-09') AND singer_name = '邓紫棋' AND sys_imp_date = '2023-08-09' "
                        + "ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);
    }

    @Test
    void testReplaceField11() {

        String replaceSql = SqlReplaceHelper.replaceFields(
                "select 歌曲名 from 歌曲库 where datediff('year', 发布日期, '2023-08-09') >= 0.5 "
                        + "and 歌手名 = '邓紫棋' and 数据日期 = '2023-08-09' order by 播放量 desc limit 11",
                fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);
        replaceSql = SqlRemoveHelper.removeNumberFilter(replaceSql);
        Assert.assertEquals("SELECT song_name FROM 歌曲库 WHERE publish_date <= '2023-02-09' "
                + "AND singer_name = '邓紫棋' AND sys_imp_date = '2023-08-09' ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);
    }

    @Test
    void testReplaceFields12() {

        String replaceSql = SqlReplaceHelper
                .replaceFields("select 部门,用户 from 超音数 where 数据日期 = '2023-08-08' and 用户 ='alice'"
                        + " and 发布日期 ='11' order by 访问次数 desc limit 1", fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT department, user_id FROM 超音数 WHERE sys_imp_date = '2023-08-08'"
                        + " AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1",
                replaceSql);
    }

    @Test
    void testReplaceFields13() {
        String replaceSql =
                SqlReplaceHelper.replaceFields(
                        "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                                + "and 用户 ='alice' and 发布日期 ='11' group by 部门 limit 1",
                        fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals("SELECT department, sum(pv) FROM 超音数 WHERE sys_imp_date = '2023-08-08'"
                + " AND user_id = 'alice' AND publish_date = '11' GROUP BY department LIMIT 1",
                replaceSql);
    }

    @Test
    void testReplaceFields14() {

        String replaceSql = "select sum(访问次数) from 超音数 where 数据日期 >= '2023-08-06' "
                + "and 数据日期 <= '2023-08-06' and 部门 = 'hr'";
        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals("SELECT sum(pv) FROM 超音数 WHERE sys_imp_date >= '2023-08-06' "
                + "AND sys_imp_date <= '2023-08-06' AND department = 'hr'", replaceSql);
    }

    @Test
    void testReplaceFields15() {

        String replaceSql =
                "SELECT 歌曲名称, sum(评分) FROM CSpider WHERE(1 < 2) AND 数据日期 = '2023-10-15' "
                        + "GROUP BY 歌曲名称 HAVING sum(评分) < ( SELECT min(评分) FROM CSpider WHERE 语种 = '英文')";
        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT 歌曲名称, sum(评分) FROM CSpider WHERE (1 < 2) AND sys_imp_date = '2023-10-15' "
                        + "GROUP BY 歌曲名称 HAVING sum(评分) < (SELECT min(评分) FROM CSpider WHERE 语种 = '英文')",
                replaceSql);
    }

    @Test
    void testReplaceFields16() {

        String replaceSql =
                "SELECT sum(评分)/ (SELECT sum(评分) FROM CSpider WHERE  数据日期 = '2023-10-15')"
                        + " FROM CSpider WHERE  数据日期 = '2023-10-15' "
                        + "GROUP BY 歌曲名称 HAVING sum(评分) < ( SELECT min(评分) FROM CSpider WHERE 语种 = '英文')";
        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);
        replaceSql = SqlReplaceHelper.replaceFunction(replaceSql);

        Assert.assertEquals(
                "SELECT sum(评分) / (SELECT sum(评分) FROM CSpider WHERE sys_imp_date = '2023-10-15') FROM "
                        + "CSpider WHERE sys_imp_date = '2023-10-15' GROUP BY 歌曲名称 HAVING sum(评分) < (SELECT min(评分) "
                        + "FROM CSpider WHERE 语种 = '英文')",
                replaceSql);
    }

    @Test
    void testReplaceFields17() {

        String replaceSql = "WITH daily_visits AS (\n"
                + "\t\tSELECT DATE_FORMAT(数据日期, 'yyyy-MM-dd') AS 数据_日期, SUM(访问次数) AS 访问_次数\n"
                + "\t\tFROM 超音数数据集\n" + "\t\tWHERE 数据日期 >= DATE_SUB('2024-10-15', 30)\n"
                + "\t\tGROUP BY DATE_FORMAT(数据日期, 'yyyy-MM-dd')\n" + "\t)\n" + "SELECT *\n"
                + "FROM daily_visits";
        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);

        Assert.assertEquals("WITH daily_visits AS (SELECT DATE_FORMAT(sys_imp_date, 'yyyy-MM-dd') "
                + "AS 数据_日期, SUM(pv) AS 访问_次数 FROM 超音数数据集 WHERE sys_imp_date >= "
                + "DATE_SUB('2024-10-15', 30) GROUP BY DATE_FORMAT(sys_imp_date, 'yyyy-MM-dd')) "
                + "SELECT * FROM daily_visits", replaceSql);
    }

    @Test
    void testReplaceFields18() {

        String replaceSql = "WITH\n" + "  latest_data AS (\n" + "    SELECT\n" + "      粉丝数,\n"
                + "      ROW_NUMBER() OVER (\n" + "        ORDER BY\n" + "          数据日期 DESC\n"
                + "      ) AS __row_num__\n" + "    FROM\n" + "      问答艺人数据集\n" + "    WHERE\n"
                + "      (TME歌手ID = '1')\n" + "      AND (\n" + "        数据日期 >= '2024-10-22'\n"
                + "        AND 数据日期 <= '2024-10-29'\n" + "      )\n" + "  )\n" + "SELECT\n"
                + "  AVG(__粉丝数__)\n" + "FROM\n" + "  latest_data\n" + "WHERE\n"
                + "  __row_num__ = 1";
        replaceSql = SqlReplaceHelper.replaceFields(replaceSql, fieldToBizName);

        Assert.assertEquals("WITH latest_data AS (SELECT fans_cnt, ROW_NUMBER() OVER "
                + "(ORDER BY sys_imp_date DESC) AS __row_num__ FROM 问答艺人数据集 WHERE (TME歌手ID = '1') "
                + "AND (sys_imp_date >= '2024-10-22' AND sys_imp_date <= '2024-10-29')) SELECT AVG(__粉丝数__) "
                + "FROM latest_data WHERE __row_num__ = 1", replaceSql);
    }
}
