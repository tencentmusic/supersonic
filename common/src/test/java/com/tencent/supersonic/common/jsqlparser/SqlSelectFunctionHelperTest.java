package com.tencent.supersonic.common.jsqlparser;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * SqlParserSelectHelper Test
 */
class SqlSelectFunctionHelperTest {

    @Test
    void testHasAggregateFunction1() {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        boolean hasAggregateFunction = SqlSelectFunctionHelper.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, true);
    }

    @Test
    void testHasAggregateFunction2() {
        String sql = "select 部门,count (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        boolean hasAggregateFunction = SqlSelectFunctionHelper.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, true);
    }

    @Test
    void testHasAggregateFunction3() {
        String sql =
                "SELECT count(1) FROM s2 WHERE sys_imp_date = '2023-08-08' AND user_id = 'alice'"
                        + " AND publish_date = '11' ORDER BY pv DESC LIMIT 1";
        boolean hasAggregateFunction = SqlSelectFunctionHelper.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, true);
    }

    @Test
    void testHasAggregateFunction4() {
        String sql =
                "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08' "
                        + "AND user_id = 'alice' AND publish_date = '11' ORDER BY pv DESC LIMIT 1";
        boolean hasAggregateFunction = SqlSelectFunctionHelper.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, false);

    }

    @Test
    void testHasAggregateFunction5() {
        String sql = "SELECT department, user_id, field_a FROM s2 WHERE sys_imp_date = '2023-08-08'"
                + " AND user_id = 'alice' AND publish_date = '11'";
        boolean hasAggregateFunction = SqlSelectFunctionHelper.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, false);
    }

    @Test
    void testHasAggregateFunction6() {
        String sql = "SELECT user_name, sum(pv) FROM t_34 WHERE sys_imp_date <= '2023-09-03' "
                + "AND sys_imp_date >= '2023-08-04' GROUP BY user_name ORDER BY sum(pv) DESC LIMIT 10";
        boolean hasAggregateFunction = SqlSelectFunctionHelper.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, true);
    }

    @Test
    void testHasAggregateFunction7() {
        String sql = "WITH\n" + "  date_range AS (\n" + "    SELECT\n"
                + "      DATEADD ('DAY', -7, CURRENT_DATE) AS start_date,\n"
                + "      DATEADD ('DAY', -1, CURRENT_DATE) AS end_date\n" + "  )\n" + "SELECT\n"
                + "  SUM(访问次数) AS 访问次数\n" + "FROM\n" + "  超音数数据集\n" + "WHERE\n" + "  数据日期 >= (\n"
                + "    SELECT\n" + "      start_date\n" + "    FROM\n" + "      date_range\n"
                + "  )\n" + "  AND 数据日期 <= (\n" + "    SELECT\n" + "      end_date\n" + "    FROM\n"
                + "      date_range\n" + "  )";
        boolean hasAggregateFunction = SqlSelectFunctionHelper.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, true);
    }

    @Test
    void testHasAggregateFunction8() {
        String sql = "WITH\n" + "  date_range AS (\n" + "    SELECT\n"
                + "      DATEADD ('DAY', -7, CURRENT_DATE) AS start_date,\n"
                + "      DATEADD ('DAY', -1, CURRENT_DATE) AS end_date\n" + "  )\n" + "SELECT\n"
                + "  SUM(访问次数) AS 访问次数\n" + "FROM\n" + "  超音数数据集\n" + "WHERE\n" + "  数据日期 >= (\n"
                + "    SELECT\n" + "      start_date\n" + "    FROM\n" + "      date_range\n"
                + "  )\n" + "  AND 数据日期 <= (\n" + "    SELECT\n" + "      end_date\n" + "    FROM\n"
                + "      date_range\n" + "  )";
        boolean hasAggregateFunction = SqlSelectFunctionHelper.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, true);
    }

    @Test
    void testHasAggregateFunction9() {
        String sql = "WITH `t_57` AS (\n"
                + "\t\tSELECT `sys_imp_date`, `empl_name`, `department`, `pv`\n" + "\t\tFROM (\n"
                + "\t\t\tSELECT `sys_imp_date`, `empl_name`, `department`, `pvuv_statis_pv` AS `pv`\n"
                + "\t\t\tFROM (\n"
                + "\t\t\t\tSELECT `pvuv_statis_pv` AS `pvuv_statis_pv`, `sys_imp_date`, `empl_name`, `department`\n"
                + "\t\t\t\tFROM (\n"
                + "\t\t\t\t\tSELECT `src1_pvuv_statis`.`sys_imp_date` AS `sys_imp_date`, `src1_pvuv_statis`.`pvuv_statis_pv` "
                + "AS `pvuv_statis_pv`, `src1_department`.`empl_name` AS `empl_name`, `src1_department`.`department` AS `department`\n"
                + "\t\t\t\t\tFROM (\n"
                + "\t\t\t\t\t\tSELECT `empl_name` AS `empl_name`, `department` AS `department`\n"
                + "\t\t\t\t\t\tFROM (\n" + "\t\t\t\t\t\t\tSELECT *\n"
                + "\t\t\t\t\t\t\tFROM `dw_ods`.`user_department`\n" + "\t\t\t\t\t\t) `department`\n"
                + "\t\t\t\t\t) `src1_department`\n" + "\t\t\t\t\t\tLEFT JOIN (\n"
                + "\t\t\t\t\t\t\tSELECT `pv` AS `pvuv_statis_pv`, `imp_date` AS `sys_imp_date`, `user_name`\n"
                + "\t\t\t\t\t\t\tFROM (\n"
                + "\t\t\t\t\t\t\t\tSELECT `event`, `accountId` AS `user_name`, `imp_date`, `ori_page_title`, `page_title`\n"
                + "\t\t\t\t\t\t\t\t\t, `pg_id`, `accountId` AS `uv`, 1 AS `pv`, `accountId`\n"
                + "\t\t\t\t\t\t\t\tFROM `dw_ods`.`s2_event_view` `t`\n"
                + "\t\t\t\t\t\t\t) `pvuv_statis`\n" + "\t\t\t\t\t\t) `src1_pvuv_statis`\n"
                + "\t\t\t\t\t\tON `src1_department`.`empl_name` = `src1_pvuv_statis`.`user_name`\n"
                + "\t\t\t\t) `src11_`\n" + "\t\t\t) `department_pvuv_statis_0`\n"
                + "\t\t) `department_pvuv_statis_1`\n" + "\t), \n" + "\t`department_visits` AS (\n"
                + "\t\tSELECT `department`, SUM(`pv`) AS `total_visits`\n" + "\t\tFROM `t_57`\n"
                + "\t\tWHERE `sys_imp_date` >= '2024-01-01'\n"
                + "\t\t\tAND `sys_imp_date` <= '2024-10-31'\n" + "\t\tGROUP BY `department`\n"
                + "\t\tORDER BY `total_visits` DESC\n" + "\t\tLIMIT 3\n" + "\t), \n"
                + "\t`user_visits` AS (\n"
                + "\t\tSELECT `department1`, `empl_name`, SUM(`pv`) AS `user_total_visits`\n"
                + "\t\tFROM `t_57`\n" + "\t\tWHERE `sys_imp_date` >= '2024-01-01'\n"
                + "\t\t\tAND `sys_imp_date` <= '2024-10-31'\n" + "\t\t\tAND `department` IN (\n"
                + "\t\t\t\tSELECT `department`\n" + "\t\t\t\tFROM `department_visits`\n"
                + "\t\t\t)\n" + "\t\tGROUP BY `department`, `empl_name`\n" + "\t)\n"
                + "SELECT `dv`.`department`, `uv`.`empl_name`, `uv`.`user_total_visits` AS `max_visits`\n"
                + "FROM `department_visits` `dv`\n"
                + "\tINNER JOIN `user_visits` `uv` ON `dv`.`department` = `uv`.`department1`\n"
                + "WHERE (`uv`.`department`, `uv`.`user_total_visits`) IN (\n"
                + "\tSELECT `department`, MAX(`user_total_visits`)\n" + "\tFROM `user_visits`\n"
                + "\tGROUP BY `department`\n" + ")\n" + "LIMIT 1000";
        boolean hasAggregateFunction = SqlSelectFunctionHelper.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, true);
    }

    @Test
    void testHasAggregateFunction10() {
        String sql = "WITH `temp_table` AS (\n" + "\t\tSELECT 用户, 部门, 数据日期, SUM(访问次数) AS _总访问次数_\n"
                + "\t\tFROM 超音数数据集\n" + "\t\tWHERE 数据日期 >= '2024-01-01'\n"
                + "\t\t\tAND 数据日期 <= '2024-10-31'\n" + "\t\tGROUP BY 用户, 部门, 数据日期\n" + "\t)\n"
                + "SELECT 用户, 部门\n" + "FROM `temp_table`\n" + "WHERE (_部门_, _总访问次数_) IN (\n"
                + "\tSELECT _部门_, MAX(_总访问次数_)\n" + "\tFROM `temp_table`\n" + "\tGROUP BY _部门_\n"
                + ")";
        boolean hasAggregateFunction = SqlSelectFunctionHelper.hasAggregateFunction(sql);
        Assert.assertEquals(hasAggregateFunction, true);
    }

    @Test
    void testHasFunction1() {

        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        boolean hasFunction = SqlSelectFunctionHelper.hasFunction(sql, "sum");
        Assert.assertEquals(hasFunction, true);
    }

    @Test
    void testHasFunction2() {
        String sql = "select 部门,count (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        boolean hasFunction = SqlSelectFunctionHelper.hasFunction(sql, "count");
        Assert.assertEquals(hasFunction, true);

    }

    @Test
    void testHasFunction3() {
        String sql = "select 部门,count (*) from 超音数 where 数据日期 = '2023-08-08' "
                + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        boolean hasFunction = SqlSelectFunctionHelper.hasFunction(sql, "count");
        Assert.assertEquals(hasFunction, true);
    }

    @Test
    void testHasFunction4() {
        String sql = "SELECT user_name, pv FROM t_34 WHERE sys_imp_date <= '2023-09-03' "
                + "AND sys_imp_date >= '2023-08-04' GROUP BY user_name ORDER BY sum(pv) DESC LIMIT 10";
        boolean hasFunction = SqlSelectFunctionHelper.hasFunction(sql, "sum");
        Assert.assertEquals(hasFunction, false);

    }

    @Test
    void testHasFunction5() {
        String sql = "select 部门,min (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";
        boolean hasFunction = SqlSelectFunctionHelper.hasFunction(sql, "min");

        Assert.assertEquals(hasFunction, true);
    }

    @Test
    void testHasAsterisk1() {
        String sql = "select 部门,sum (访问次数) from 超音数 where 数据日期 = '2023-08-08' "
                + "and 用户 =alice and 发布日期 ='11' group by 部门 limit 1";

        Assert.assertEquals(SqlSelectFunctionHelper.hasAsterisk(sql), false);
    }

    @Test
    void testHasAsterisk2() {
        String sql =
                "select * from 超音数 where 数据日期 = '2023-08-08' " + "and 用户 =alice and 发布日期 ='11'";
        Assert.assertEquals(SqlSelectFunctionHelper.hasAsterisk(sql), true);
    }
}
