package com.tencent.supersonic.common.util.jsqlparser;


import cn.hutool.core.lang.Assert;
import org.junit.jupiter.api.Test;

/**
 * @author lex luo
 * @date 2023/11/15 15:04
 */
class SqlParserEqualHelperTest {

    @Test
    void testEquals() {
        String sql1 = "SELECT * FROM table1 WHERE column1 = 1 AND column2 = 2";
        String sql2 = "SELECT * FROM table1 WHERE column2 = 2 AND column1 = 1";
        Assert.equals(SqlParserEqualHelper.equals(sql1, sql2), true);

        sql1 = "SELECT a,b,c,d FROM table1 WHERE column1 = 1 AND column2 = 2 order by a";
        sql2 = "SELECT d,c,b,a FROM table1 WHERE column2 = 2 AND column1 = 1 order by a";
        Assert.equals(SqlParserEqualHelper.equals(sql1, sql2), true);

        sql1 = "SELECT a,sum(b),sum(c),sum(d) FROM table1 WHERE column1 = 1 AND column2 = 2 group by a order by a";

        sql2 = "SELECT sum(d),sum(c),sum(b),a FROM table1 WHERE column2 = 2 AND column1 = 1 group by a order by a";

        Assert.equals(SqlParserEqualHelper.equals(sql1, sql2), true);

        sql1 = "SELECT a,sum(b),sum(c),sum(d) FROM table1 WHERE column1 = 1 AND column2 = 2 group by a order by a";

        sql2 = "SELECT sum(d),sum(c),sum(b),a FROM table1 WHERE column2 = 2 AND column1 = 1 group by a order by a";

        Assert.equals(SqlParserEqualHelper.equals(sql1, sql2), true);

        sql1 = "SELECT a,b,c,d FROM table1 WHERE column1 = 1 AND column2 = 2 order by a";
        sql2 = "SELECT d,c,b,f FROM table1 WHERE column2 = 2 AND column1 = 1 order by a";
        Assert.equals(SqlParserEqualHelper.equals(sql1, sql2), false);


    }
}