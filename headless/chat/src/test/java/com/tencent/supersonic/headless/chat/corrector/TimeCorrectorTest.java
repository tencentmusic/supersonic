package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class TimeCorrectorTest {

    @Test
    void testDoCorrect() {
        TimeCorrector corrector = new TimeCorrector();
        ChatQueryContext chatQueryContext = new ChatQueryContext();
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        SqlInfo sqlInfo = new SqlInfo();
        // 1.数据日期 <=
        String sql = "SELECT 维度1, SUM(播放量) FROM 数据库 "
                + "WHERE (歌手名 = '张三') AND 数据日期 <= '2023-11-17' GROUP BY 维度1";
        sqlInfo.setCorrectedS2SQL(sql);
        semanticParseInfo.setSqlInfo(sqlInfo);
        corrector.doCorrect(chatQueryContext, semanticParseInfo);

        Assert.assertEquals(
                "SELECT 维度1, SUM(播放量) FROM 数据库 WHERE ((歌手名 = '张三') AND 数据日期 <= '2023-11-17') "
                        + "AND 数据日期 >= '2023-11-17' GROUP BY 维度1",
                sqlInfo.getCorrectedS2SQL());

        // 2.数据日期 <
        sql = "SELECT 维度1, SUM(播放量) FROM 数据库 "
                + "WHERE (歌手名 = '张三') AND 数据日期 < '2023-11-17' GROUP BY 维度1";
        sqlInfo.setCorrectedS2SQL(sql);
        corrector.doCorrect(chatQueryContext, semanticParseInfo);

        Assert.assertEquals(
                "SELECT 维度1, SUM(播放量) FROM 数据库 WHERE ((歌手名 = '张三') AND 数据日期 < '2023-11-17') "
                        + "AND 数据日期 >= '2023-11-17' GROUP BY 维度1",
                sqlInfo.getCorrectedS2SQL());

        // 3.数据日期 >=
        sql = "SELECT 维度1, SUM(播放量) FROM 数据库 "
                + "WHERE (歌手名 = '张三') AND 数据日期 >= '2023-11-17' GROUP BY 维度1";
        sqlInfo.setCorrectedS2SQL(sql);
        corrector.doCorrect(chatQueryContext, semanticParseInfo);

        Assert.assertEquals(
                "SELECT 维度1, SUM(播放量) FROM 数据库 "
                        + "WHERE (歌手名 = '张三') AND 数据日期 >= '2023-11-17' GROUP BY 维度1",
                sqlInfo.getCorrectedS2SQL());

        // 4.数据日期 >
        sql = "SELECT 维度1, SUM(播放量) FROM 数据库 "
                + "WHERE (歌手名 = '张三') AND 数据日期 > '2023-11-17' GROUP BY 维度1";
        sqlInfo.setCorrectedS2SQL(sql);
        corrector.doCorrect(chatQueryContext, semanticParseInfo);

        Assert.assertEquals(
                "SELECT 维度1, SUM(播放量) FROM 数据库 "
                        + "WHERE (歌手名 = '张三') AND 数据日期 > '2023-11-17' GROUP BY 维度1",
                sqlInfo.getCorrectedS2SQL());

        // 5.no 数据日期
        sql = "SELECT 维度1, SUM(播放量) FROM 数据库 " + "WHERE 歌手名 = '张三' GROUP BY 维度1";
        sqlInfo.setCorrectedS2SQL(sql);
        corrector.doCorrect(chatQueryContext, semanticParseInfo);

        Assert.assertEquals("SELECT 维度1, SUM(播放量) FROM 数据库 WHERE 歌手名 = '张三' GROUP BY 维度1",
                sqlInfo.getCorrectedS2SQL());

        // 6. 数据日期-月 <=
        sql = "SELECT 维度1, SUM(播放量) FROM 数据库 "
                + "WHERE 歌手名 = '张三' AND 数据日期_月 <= '2024-01' GROUP BY 维度1";
        sqlInfo.setCorrectedS2SQL(sql);
        corrector.doCorrect(chatQueryContext, semanticParseInfo);

        Assert.assertEquals(
                "SELECT 维度1, SUM(播放量) FROM 数据库 WHERE (歌手名 = '张三' AND 数据日期_月 <= '2024-01') "
                        + "AND 数据日期_月 >= '2024-01' GROUP BY 维度1",
                sqlInfo.getCorrectedS2SQL());

        // 7. 数据日期-月 >
        sql = "SELECT 维度1, SUM(播放量) FROM 数据库 "
                + "WHERE 歌手名 = '张三' AND 数据日期_月 > '2024-01' GROUP BY 维度1";
        sqlInfo.setCorrectedS2SQL(sql);
        corrector.doCorrect(chatQueryContext, semanticParseInfo);

        Assert.assertEquals(
                "SELECT 维度1, SUM(播放量) FROM 数据库 "
                        + "WHERE 歌手名 = '张三' AND 数据日期_月 > '2024-01' GROUP BY 维度1",
                sqlInfo.getCorrectedS2SQL());

        // 8. no where
        sql = "SELECT COUNT(1) FROM 数据库";
        sqlInfo.setCorrectedS2SQL(sql);
        corrector.doCorrect(chatQueryContext, semanticParseInfo);
        Assert.assertEquals("SELECT COUNT(1) FROM 数据库", sqlInfo.getCorrectedS2SQL());
    }
}
