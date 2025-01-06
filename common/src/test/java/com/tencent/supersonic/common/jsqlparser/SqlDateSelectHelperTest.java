package com.tencent.supersonic.common.jsqlparser;

import com.tencent.supersonic.common.jsqlparser.DateVisitor.DateBoundInfo;
import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class SqlDateSelectHelperTest {

    @Test
    void testGetDateBoundInfo() {

        String sql = "SELECT 维度1,sum(播放量) FROM 数据库 "
                + "WHERE (歌手名 = '张三') AND 数据日期 >= '2023-11-17' GROUP BY 维度1";
        DateBoundInfo dateBoundInfo = SqlDateSelectHelper.getDateBoundInfo(sql, "数据日期");
        Assert.assertEquals(dateBoundInfo.getLowerBound(), ">=");
        Assert.assertEquals(dateBoundInfo.getLowerDate(), "2023-11-17");

        sql = "SELECT 维度1,sum(播放量) FROM 数据库 "
                + "WHERE (歌手名 = '张三') AND 数据日期 > '2023-11-17' GROUP BY 维度1";
        dateBoundInfo = SqlDateSelectHelper.getDateBoundInfo(sql, "数据日期");
        Assert.assertEquals(dateBoundInfo.getLowerBound(), ">");
        Assert.assertEquals(dateBoundInfo.getLowerDate(), "2023-11-17");

        sql = "SELECT 维度1,sum(播放量) FROM 数据库 "
                + "WHERE (歌手名 = '张三') AND 数据日期 <= '2023-11-17' GROUP BY 维度1";
        dateBoundInfo = SqlDateSelectHelper.getDateBoundInfo(sql, "数据日期");
        Assert.assertEquals(dateBoundInfo.getUpperBound(), "<=");
        Assert.assertEquals(dateBoundInfo.getUpperDate(), "2023-11-17");

        sql = "SELECT 维度1,sum(播放量) FROM 数据库 "
                + "WHERE (歌手名 = '张三') AND 数据日期 < '2023-11-17' GROUP BY 维度1";
        dateBoundInfo = SqlDateSelectHelper.getDateBoundInfo(sql, "数据日期");
        Assert.assertEquals(dateBoundInfo.getUpperBound(), "<");
        Assert.assertEquals(dateBoundInfo.getUpperDate(), "2023-11-17");

        sql = "SELECT 维度1,sum(播放量) FROM 数据库 " + "WHERE (歌手名 = '张三') AND 数据日期 >= '2023-10-17' "
                + "AND 数据日期 <= '2023-11-17' GROUP BY 维度1";
        dateBoundInfo = SqlDateSelectHelper.getDateBoundInfo(sql, "数据日期");
        Assert.assertEquals(dateBoundInfo.getUpperBound(), "<=");
        Assert.assertEquals(dateBoundInfo.getUpperDate(), "2023-11-17");
        Assert.assertEquals(dateBoundInfo.getLowerBound(), ">=");
        Assert.assertEquals(dateBoundInfo.getLowerDate(), "2023-10-17");
    }
}
