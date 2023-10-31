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
    void removeHavingCondition() {
        String sql = "select 歌曲名 from 歌曲库 where 歌手名 = '周杰伦'   HAVING sum(播放量) > 20000";
        Set<String> removeFieldNames = new HashSet<>();
        removeFieldNames.add("播放量");
        String replaceSql = SqlParserRemoveHelper.removeHavingCondition(sql, removeFieldNames);
        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE 歌手名 = '周杰伦' HAVING 2 > 1",
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
                        + "AND 1 = 1 AND 数据日期 = '2023-08-09' AND 歌曲发布时 = '2023-08-01' "
                        + "ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);

        sql = "select 歌曲名 from 歌曲库 where datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌曲名 in ('邓紫棋','周杰伦') and 歌曲名 in ('邓紫棋') and 数据日期 = '2023-08-09' and 歌曲发布时 = '2023-08-01'"
                + " order by 播放量 desc limit 11";
        replaceSql = SqlParserRemoveHelper.removeWhereCondition(sql, removeFieldNames);
        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE datediff('day', 发布日期, '2023-08-09') <= 1 "
                        + "AND 1 IN (1) AND 1 IN (1) AND 数据日期 = '2023-08-09' AND "
                        + "歌曲发布时 = '2023-08-01' ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);

        sql = "select 歌曲名 from 歌曲库 where (datediff('day', 发布日期, '2023-08-09') <= 1 "
                + "and 歌曲名 in ('邓紫棋','周杰伦') and 歌曲名 in ('邓紫棋')) and 数据日期 = '2023-08-09' "
                + " order by 播放量 desc limit 11";
        replaceSql = SqlParserRemoveHelper.removeWhereCondition(sql, removeFieldNames);
        Assert.assertEquals(
                "SELECT 歌曲名 FROM 歌曲库 WHERE (datediff('day', 发布日期, '2023-08-09') <= 1 "
                        + "AND 1 IN (1) AND 1 IN (1)) AND 数据日期 = '2023-08-09' ORDER BY 播放量 DESC LIMIT 11",
                replaceSql);
    }

}
