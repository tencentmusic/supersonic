package com.tencent.supersonic.common.util.jsqlparser;

import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * SqlParser Remove Helper Test
 */
class SqlParserRemoveHelperTest {

    @Test
    void removeWhereHavingCondition() {
        String sql = "select 歌曲名 from 歌曲库 where sum(粉丝数) > 20000 and  2>1 and "
                + "sum(播放量) > 20000 and 1=1  HAVING sum(播放量) > 20000 and 3>1";
        sql = SqlParserRemoveHelper.removeNumberCondition(sql);
        System.out.println(sql);
        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE sum(粉丝数) > 20000 AND sum(播放量) > 20000 HAVING sum(播放量) > 20000",
                sql);
        sql = "SELECT 歌曲,sum(播放量) FROM 歌曲库\n"
                + "WHERE (歌手名 = '张三' AND 2 > 1) AND 数据日期 = '2023-11-07'\n"
                + "GROUP BY 歌曲名 HAVING sum(播放量) > 100000";
        sql = SqlParserRemoveHelper.removeNumberCondition(sql);
        System.out.println(sql);
        Assert.assertEquals(
                "SELECT 歌曲, sum(播放量) FROM 歌曲库 WHERE (歌手名 = '张三') "
                        + "AND 数据日期 = '2023-11-07' GROUP BY 歌曲名 HAVING sum(播放量) > 100000",
                sql);
        sql = "SELECT 歌曲名,sum(播放量) FROM 歌曲库 WHERE (1 = 1 AND 1 = 1 AND 2 > 1 )"
                + "AND 1 = 1 AND 歌曲类型 IN ('类型一', '类型二') AND 歌手名 IN ('林俊杰', '周杰伦')"
                + "AND 数据日期 = '2023-11-07' GROUP BY 歌曲名 HAVING 2 > 1 AND SUM(播放量) >= 1000";
        sql = SqlParserRemoveHelper.removeNumberCondition(sql);
        System.out.println(sql);
        Assert.assertEquals(
                "SELECT 歌曲名, sum(播放量) FROM 歌曲库 WHERE 歌曲类型 IN ('类型一', '类型二') "
                        + "AND 歌手名 IN ('林俊杰', '周杰伦') AND 数据日期 = '2023-11-07' "
                        + "GROUP BY 歌曲名 HAVING SUM(播放量) >= 1000",
                sql);

        sql = "SELECT 品牌名称,法人 FROM 互联网企业 WHERE (2 > 1 AND 1 = 1) AND 数据日期 = '2023-10-31'"
                + "GROUP BY 品牌名称, 法人 HAVING 2 > 1 AND sum(注册资本) > 100000000 AND sum(营收占比) = 0.5 and 1 = 1";
        sql = SqlParserRemoveHelper.removeNumberCondition(sql);
        System.out.println(sql);
        Assert.assertEquals(
                "SELECT 品牌名称, 法人 FROM 互联网企业 WHERE 数据日期 = '2023-10-31' GROUP BY "
                        + "品牌名称, 法人 HAVING sum(注册资本) > 100000000 AND sum(营收占比) = 0.5",
                sql);
    }

    @Test
    void removeHavingCondition() {
        String sql = "select 歌曲名 from 歌曲库 where 歌手名 = '周杰伦'   HAVING sum(播放量) > 20000";
        Set<String> removeFieldNames = new HashSet<>();
        removeFieldNames.add("播放量");
        String replaceSql = SqlParserRemoveHelper.removeHavingCondition(sql, removeFieldNames);
        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE 歌手名 = '周杰伦'",
                replaceSql);

    }

    @Test
    void removeWhereCondition() {
        String sql = "select 歌曲名 from 歌曲库 where datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌曲名 = '邓紫棋' and 数据日期 = '2023-08-09' and 歌曲发布时 = '2023-08-01'"
                + " order by 播放量 desc limit 11";

        Set<String> removeFieldNames = new HashSet<>();
        removeFieldNames.add("歌曲名");

        String replaceSql = SqlParserRemoveHelper.removeWhereCondition(sql, removeFieldNames);

        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE datediff('day', 发布日期, '2023-08-09') <= 1 "
                        + "AND 数据日期 = '2023-08-09' AND 歌曲发布时 = '2023-08-01' "
                        + "ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);

        sql = "select 歌曲名 from 歌曲库 where datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌曲名 in ('邓紫棋','周杰伦') and 歌曲名 in ('邓紫棋') and 数据日期 = '2023-08-09' and 歌曲发布时 = '2023-08-01'"
                + " order by 播放量 desc limit 11";
        replaceSql = SqlParserRemoveHelper.removeWhereCondition(sql, removeFieldNames);
        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE datediff('day', 发布日期, '2023-08-09') <= 1 "
                        + "AND 数据日期 = '2023-08-09' AND "
                        + "歌曲发布时 = '2023-08-01' ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);

        sql = "select 歌曲名 from 歌曲库 where (datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌曲名 in ('邓紫棋','周杰伦') and 歌曲名 in ('邓紫棋')) and 数据日期 = '2023-08-09' "
                + " order by 播放量 desc limit 11";
        replaceSql = SqlParserRemoveHelper.removeWhereCondition(sql, removeFieldNames);
        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE (datediff('day', 发布日期, '2023-08-09') <= 1) "
                        + "AND 数据日期 = '2023-08-09' ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);
    }

}
