package com.tencent.supersonic.common.jsqlparser;


import org.junit.Assert;
import org.junit.jupiter.api.Test;

class SqlValidHelperTest {

    @Test
    void testEquals() {
        String sql1 = "SELECT * FROM table1 WHERE column1 = 1 AND column2 = 2";
        String sql2 = "SELECT * FROM table1 WHERE column2 = 2 AND column1 = 1";
        Assert.assertEquals(SqlValidHelper.equals(sql1, sql2), true);

        sql1 = "SELECT a,b,c,d FROM table1 WHERE column1 = 1 AND column2 = 2 order by a";
        sql2 = "SELECT d,c,b,a FROM table1 WHERE column2 = 2 AND column1 = 1 order by a";
        Assert.assertEquals(SqlValidHelper.equals(sql1, sql2), true);

        sql1 = "SELECT a,sum(b),sum(c),sum(d) FROM table1 WHERE column1 = 1 AND column2 = 2 group by a order by a";

        sql2 = "SELECT sum(d),sum(c),sum(b),a FROM table1 WHERE column2 = 2 AND column1 = 1 group by a order by a";

        Assert.assertEquals(SqlValidHelper.equals(sql1, sql2), true);

        sql1 = "SELECT a,sum(b),sum(c),sum(d) FROM table1 WHERE column1 = 1 AND column2 = 2 group by a order by a";

        sql2 = "SELECT sum(d),sum(c),sum(b),a FROM table1 WHERE column2 = 2 AND column1 = 1 group by a order by a";

        Assert.assertEquals(SqlValidHelper.equals(sql1, sql2), true);

        sql1 = "SELECT a,b,c,d FROM table1 WHERE column1 = 1 AND column2 = 2 order by a";
        sql2 = "SELECT d,c,b,f FROM table1 WHERE column2 = 2 AND column1 = 1 order by a";
        Assert.assertEquals(SqlValidHelper.equals(sql1, sql2), false);

        sql1 = "SELECT\n"
                + "页面,\n"
                + "SUM(访问次数)\n"
                + "FROM\n"
                + "超音数\n"
                + "WHERE\n"
                + "数据日期 >= '2023-10-26'\n"
                + "AND 数据日期 <= '2023-11-09'\n"
                + "AND department = \"HR\"\n"
                + "GROUP BY\n"
                + "页面\n"
                + "LIMIT\n"
                + "365";

        sql2 = "SELECT\n"
                + "页面,\n"
                + "SUM(访问次数)\n"
                + "FROM\n"
                + "超音数\n"
                + "WHERE\n"
                + "department = \"HR\"\n"
                + "AND 数据日期 >= '2023-10-26'\n"
                + "AND 数据日期 <= '2023-11-09'\n"
                + "GROUP BY\n"
                + "页面\n"
                + "LIMIT\n"
                + "365";
        Assert.assertEquals(SqlValidHelper.equals(sql1, sql2), true);
    }

    @Test
    void testIsValidSQL() {
        String sql1 = "SELECT * FROM table1 WHERE column1 = 1 AND column2 = 2";
        Assert.assertEquals(SqlValidHelper.isValidSQL(sql1), true);

        sql1 = "SELECT sum(b),sum(c),sum(d) FROM table1 WHERE column1 = 1 AND column2 = 2";

        Assert.assertEquals(SqlValidHelper.isValidSQL(sql1), true);

        sql1 = "SELECT a,b,c, FROM table1 WHERE column1 = 1 AND column2 = 2 order by a";
        Assert.assertEquals(SqlValidHelper.isValidSQL(sql1), false);

        sql1 = "SELECTa,b,c,d FROM table1";

        Assert.assertEquals(SqlValidHelper.isValidSQL(sql1), false);

        sql1 = "SELECT sum(b),sum(c),sum(d) FROM table1 WHERE";

        Assert.assertEquals(SqlValidHelper.isValidSQL(sql1), false);
    }
}