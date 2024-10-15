package com.tencent.supersonic.common.calcite;

import com.tencent.supersonic.common.pojo.enums.EngineType;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.Collections;

@Slf4j
class SqlWithMergerTest {

    @Test
    void test1() throws SqlParseException {
        String sql1 = "WITH DepartmentVisits AS (\n" + "    SELECT department, SUM(pv) AS 总访问次数\n"
                + "    FROM t_1\n"
                + "    WHERE sys_imp_date >= '2024-09-01' AND sys_imp_date <= '2024-09-29'\n"
                + "    GROUP BY department\n" + ")\n"
                + "SELECT COUNT(*) FROM DepartmentVisits WHERE 总访问次数 > 100";

        String sql2 =
                "SELECT `t3`.`sys_imp_date`, `t2`.`department`, `t3`.`s2_pv_uv_statis_pv` AS `pv`\n"
                        + "FROM (SELECT `user_name`, `department` FROM `s2_user_department`) AS `t2`\n"
                        + "LEFT JOIN (SELECT 1 AS `s2_pv_uv_statis_pv`, `imp_date` AS `sys_imp_date`, `user_name`\n"
                        + "FROM `s2_pv_uv_statis`) AS `t3` ON `t2`.`user_name` = `t3`.`user_name`";

        String mergeSql = SqlMergeWithUtils.mergeWith(EngineType.MYSQL, sql1,
                Collections.singletonList(sql2), Collections.singletonList("t_1"));

        Assert.assertEquals(format(mergeSql),
                "WITH t_1 AS (SELECT `t3`.`sys_imp_date`, `t2`.`department`, `t3`.`s2_pv_uv_statis_pv` AS `pv` "
                        + "FROM (SELECT `user_name`, `department` FROM `s2_user_department`) AS `t2` LEFT JOIN "
                        + "(SELECT 1 AS `s2_pv_uv_statis_pv`, `imp_date` AS `sys_imp_date`, `user_name` FROM `s2_pv_uv_statis`) "
                        + "AS `t3` ON `t2`.`user_name` = `t3`.`user_name`), DepartmentVisits AS (SELECT department, SUM(pv) AS 总访问次数 "
                        + "FROM t_1 WHERE sys_imp_date >= '2024-09-01' AND sys_imp_date <= '2024-09-29' GROUP BY department) "
                        + "SELECT COUNT(*) FROM DepartmentVisits WHERE 总访问次数 > 100");
    }

    @Test
    void test2() throws SqlParseException {

        String sql1 =
                "WITH DepartmentVisits AS (SELECT department, SUM(pv) AS 总访问次数 FROM t_1 WHERE sys_imp_date >= '2024-08-28' "
                        + "AND sys_imp_date <= '2024-09-28' GROUP BY department) SELECT COUNT(*) FROM DepartmentVisits WHERE 总访问次数 > 100 LIMIT 1000";

        String sql2 =
                "SELECT `t3`.`sys_imp_date`, `t2`.`department`, `t3`.`s2_pv_uv_statis_pv` AS `pv`\n"
                        + "FROM\n" + "(SELECT `user_name`, `department`\n" + "FROM\n"
                        + "`s2_user_department`) AS `t2`\n"
                        + "LEFT JOIN (SELECT 1 AS `s2_pv_uv_statis_pv`, `imp_date` AS `sys_imp_date`, `user_name`\n"
                        + "FROM\n"
                        + "`s2_pv_uv_statis`) AS `t3` ON `t2`.`user_name` = `t3`.`user_name`";

        String mergeSql = SqlMergeWithUtils.mergeWith(EngineType.MYSQL, sql1,
                Collections.singletonList(sql2), Collections.singletonList("t_1"));

        Assert.assertEquals(format(mergeSql),
                "WITH t_1 AS (SELECT `t3`.`sys_imp_date`, `t2`.`department`, `t3`.`s2_pv_uv_statis_pv` AS `pv` "
                        + "FROM (SELECT `user_name`, `department` FROM `s2_user_department`) AS `t2` LEFT JOIN "
                        + "(SELECT 1 AS `s2_pv_uv_statis_pv`, `imp_date` AS `sys_imp_date`, `user_name` FROM `s2_pv_uv_statis`) AS `t3` ON "
                        + "`t2`.`user_name` = `t3`.`user_name`), DepartmentVisits AS (SELECT department, SUM(pv) AS 总访问次数 FROM t_1 "
                        + "WHERE sys_imp_date >= '2024-08-28' AND sys_imp_date <= '2024-09-28' GROUP BY department) SELECT COUNT(*) "
                        + "FROM DepartmentVisits WHERE 总访问次数 > 100 LIMIT 1000");
    }

    @Test
    void test3() throws SqlParseException {

        String sql1 = "SELECT COUNT(*) FROM DepartmentVisits WHERE 总访问次数 > 100 LIMIT 1000";

        String sql2 =
                "SELECT `t3`.`sys_imp_date`, `t2`.`department`, `t3`.`s2_pv_uv_statis_pv` AS `pv`\n"
                        + "FROM\n" + "(SELECT `user_name`, `department`\n" + "FROM\n"
                        + "`s2_user_department`) AS `t2`\n"
                        + "LEFT JOIN (SELECT 1 AS `s2_pv_uv_statis_pv`, `imp_date` AS `sys_imp_date`, `user_name`\n"
                        + "FROM\n"
                        + "`s2_pv_uv_statis`) AS `t3` ON `t2`.`user_name` = `t3`.`user_name`";

        String mergeSql = SqlMergeWithUtils.mergeWith(EngineType.MYSQL, sql1,
                Collections.singletonList(sql2), Collections.singletonList("t_1"));

        Assert.assertEquals(format(mergeSql),
                "WITH t_1 AS (SELECT `t3`.`sys_imp_date`, `t2`.`department`, `t3`.`s2_pv_uv_statis_pv` AS `pv` "
                        + "FROM (SELECT `user_name`, `department` FROM `s2_user_department`) AS `t2` LEFT JOIN "
                        + "(SELECT 1 AS `s2_pv_uv_statis_pv`, `imp_date` AS `sys_imp_date`, `user_name` FROM `s2_pv_uv_statis`) "
                        + "AS `t3` ON `t2`.`user_name` = `t3`.`user_name`) SELECT COUNT(*) FROM DepartmentVisits WHERE 总访问次数 > 100 "
                        + "LIMIT 1000");
    }

    @Test
    void test4() throws SqlParseException {
        String sql1 = "SELECT COUNT(*) FROM DepartmentVisits WHERE 总访问次数 > 100";

        String sql2 =
                "SELECT `t3`.`sys_imp_date`, `t2`.`department`, `t3`.`s2_pv_uv_statis_pv` AS `pv`\n"
                        + "FROM\n" + "(SELECT `user_name`, `department`\n" + "FROM\n"
                        + "`s2_user_department`) AS `t2`\n"
                        + "LEFT JOIN (SELECT 1 AS `s2_pv_uv_statis_pv`, `imp_date` AS `sys_imp_date`, `user_name`\n"
                        + "FROM\n"
                        + "`s2_pv_uv_statis`) AS `t3` ON `t2`.`user_name` = `t3`.`user_name`";

        String mergeSql = SqlMergeWithUtils.mergeWith(EngineType.MYSQL, sql1,
                Collections.singletonList(sql2), Collections.singletonList("t_1"));

        Assert.assertEquals(format(mergeSql),
                "WITH t_1 AS (SELECT `t3`.`sys_imp_date`, `t2`.`department`, `t3`.`s2_pv_uv_statis_pv` AS `pv` "
                        + "FROM (SELECT `user_name`, `department` FROM `s2_user_department`) AS `t2` "
                        + "LEFT JOIN (SELECT 1 AS `s2_pv_uv_statis_pv`, `imp_date` AS `sys_imp_date`, `user_name` FROM `s2_pv_uv_statis`) "
                        + "AS `t3` ON `t2`.`user_name` = `t3`.`user_name`) SELECT COUNT(*) FROM DepartmentVisits WHERE 总访问次数 > 100");

    }

    @Test
    void test5() throws SqlParseException {

        String sql1 = "SELECT COUNT(*) FROM Department join Visits WHERE 总访问次数 > 100";

        String sql2 =
                "SELECT `t3`.`sys_imp_date`, `t2`.`department`, `t3`.`s2_pv_uv_statis_pv` AS `pv`\n"
                        + "FROM\n" + "(SELECT `user_name`, `department`\n" + "FROM\n"
                        + "`s2_user_department`) AS `t2`\n"
                        + "LEFT JOIN (SELECT 1 AS `s2_pv_uv_statis_pv`, `imp_date` AS `sys_imp_date`, `user_name`\n"
                        + "FROM\n"
                        + "`s2_pv_uv_statis`) AS `t3` ON `t2`.`user_name` = `t3`.`user_name`";

        String mergeSql = SqlMergeWithUtils.mergeWith(EngineType.MYSQL, sql1,
                Collections.singletonList(sql2), Collections.singletonList("t_1"));


        Assert.assertEquals(format(mergeSql),
                "WITH t_1 AS (SELECT `t3`.`sys_imp_date`, `t2`.`department`, `t3`.`s2_pv_uv_statis_pv` AS `pv` "
                        + "FROM (SELECT `user_name`, `department` FROM `s2_user_department`) AS `t2` LEFT JOIN "
                        + "(SELECT 1 AS `s2_pv_uv_statis_pv`, `imp_date` AS `sys_imp_date`, `user_name` FROM `s2_pv_uv_statis`) "
                        + "AS `t3` ON `t2`.`user_name` = `t3`.`user_name`) SELECT COUNT(*) FROM Department INNER JOIN Visits "
                        + "WHERE 总访问次数 > 100");
    }


    @Test
    void test6() throws SqlParseException {

        String sql1 =
                "SELECT COUNT(*) FROM Department join Visits WHERE 总访问次数 > 100 ORDER BY 总访问次数 LIMIT 10";

        String sql2 =
                "SELECT `t3`.`sys_imp_date`, `t2`.`department`, `t3`.`s2_pv_uv_statis_pv` AS `pv`\n"
                        + "FROM\n" + "(SELECT `user_name`, `department`\n" + "FROM\n"
                        + "`s2_user_department`) AS `t2`\n"
                        + "LEFT JOIN (SELECT 1 AS `s2_pv_uv_statis_pv`, `imp_date` AS `sys_imp_date`, `user_name`\n"
                        + "FROM\n"
                        + "`s2_pv_uv_statis`) AS `t3` ON `t2`.`user_name` = `t3`.`user_name`";

        String mergeSql = SqlMergeWithUtils.mergeWith(EngineType.MYSQL, sql1,
                Collections.singletonList(sql2), Collections.singletonList("t_1"));


        Assert.assertEquals(format(mergeSql),
                "WITH t_1 AS (SELECT `t3`.`sys_imp_date`, `t2`.`department`, `t3`.`s2_pv_uv_statis_pv` AS `pv` FROM "
                        + "(SELECT `user_name`, `department` FROM `s2_user_department`) AS `t2` LEFT JOIN (SELECT 1 AS `s2_pv_uv_statis_pv`,"
                        + " `imp_date` AS `sys_imp_date`, `user_name` FROM `s2_pv_uv_statis`) AS `t3` ON `t2`.`user_name` = `t3`.`user_name`) "
                        + "SELECT COUNT(*) FROM Department INNER JOIN Visits WHERE 总访问次数 > 100 ORDER BY 总访问次数 LIMIT 10");
    }

    private static String format(String mergeSql) {
        mergeSql = mergeSql.replace("\r\n", "\n");
        // Remove extra spaces and newlines
        return mergeSql.replaceAll("\\s+", " ").trim();
    }
}
