package com.tencent.supersonic.common.calcite;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * SqlParseUtils Test
 *
 * @date 2023/7/12 12:00
 */
@Slf4j
class SqlParseUtilsTest {

    @Test
    void addAliasToSql() throws SqlParseException {

        String addAliasToSql = SqlParseUtils.addAliasToSql(
                "select sum(pv) from  ( select * from  t_1 "
                        + "where sys_imp_date >= '2023-07-07' and  sys_imp_date <= '2023-07-07' ) as  t_sub_1");

        Assert.assertTrue(addAliasToSql.toLowerCase().contains("as pv"));
    }

    @Test
    void addFieldToSql() throws SqlParseException {

        String addFieldToSql = SqlParseUtils.addFieldsToSql(
                "select pv from  ( select * from  t_1 "
                        + "where sys_imp_date >= '2023-07-07' and  sys_imp_date <= '2023-07-07' ) as  t_sub_1",
                Collections.singletonList("uv"));

        Assert.assertTrue(addFieldToSql.toLowerCase().contains("uv"));

        addFieldToSql = SqlParseUtils.addFieldsToSql(
                "select uv from  ( select * from  t_1 "
                        + "where sys_imp_date >= '2023-07-07' and  sys_imp_date <= '2023-07-07' ) as  t_sub_1  "
                        + "order by play_count desc limit 10",
                Collections.singletonList("pv"));
        Assert.assertTrue(addFieldToSql.toLowerCase().contains("pv"));

        addFieldToSql = SqlParseUtils.addFieldsToSql(
                "select uv from  "
                        + "( select * from  t_1 where sys_imp_date >= '2023-07-07' "
                        + "  and  sys_imp_date <= '2023-07-07' "
                        + ") as  t_sub_1 "
                        + "where user_id = '张三' order by play_count desc limit 10",
                Collections.singletonList("pv"));
        Assert.assertTrue(addFieldToSql.toLowerCase().contains("pv"));
    }

    @Test
    void getSqlParseInfo() {

        SqlParserInfo sqlParserInfo = SqlParseUtils.getSqlParseInfo(
                "select pv from  "
                        + "( select * from  t_1 where sys_imp_date >= '2023-07-07' and  sys_imp_date <= '2023-07-07' )"
                        + " as  t_sub_1 ");

        Assert.assertTrue(sqlParserInfo.getTableName().equalsIgnoreCase("t_1"));

        List<String> collect = sqlParserInfo.getAllFields().stream().map(field -> field.toLowerCase())
                .collect(Collectors.toList());

        Assert.assertTrue(collect.contains("pv"));
        Assert.assertTrue(!collect.contains("uv"));

        List<String> selectFields = sqlParserInfo.getSelectFields().stream().map(field -> field.toLowerCase())
                .collect(Collectors.toList());
        Assert.assertTrue(selectFields.contains("pv"));
        Assert.assertTrue(!selectFields.contains("uv"));

        sqlParserInfo = SqlParseUtils.getSqlParseInfo(
                "select uv from  t_1  order by play_count desc limit 10");

        Assert.assertTrue(sqlParserInfo.getTableName().equalsIgnoreCase("t_1"));
        collect = sqlParserInfo.getAllFields().stream().map(field -> field.toLowerCase())
                .collect(Collectors.toList());
        Assert.assertTrue(collect.contains("uv"));
        Assert.assertTrue(collect.contains("play_count"));
        Assert.assertTrue(!collect.contains("pv"));

        selectFields = sqlParserInfo.getSelectFields().stream().map(field -> field.toLowerCase())
                .collect(Collectors.toList());
        Assert.assertTrue(selectFields.contains("uv"));
        Assert.assertTrue(!selectFields.contains("pv"));
        Assert.assertTrue(!selectFields.contains("play_count"));

        sqlParserInfo = SqlParseUtils.getSqlParseInfo(
                "select uv from  "
                        + "( "
                        + "   select * from t_1 where sys_imp_date >= '2023-07-07' and  sys_imp_date <= '2023-07-07' "
                        + ") as  t_sub_1 "
                        + "where user_id = '1' order by play_count desc limit 10"
        );

        Assert.assertTrue(sqlParserInfo.getTableName().equalsIgnoreCase("t_1"));
        collect = sqlParserInfo.getAllFields().stream().map(field -> field.toLowerCase())
                .collect(Collectors.toList());
        Assert.assertTrue(collect.contains("uv"));
        Assert.assertTrue(collect.contains("play_count"));
        Assert.assertTrue(collect.contains("user_id"));
        Assert.assertTrue(!collect.contains("pv"));

        selectFields = sqlParserInfo.getSelectFields().stream().map(field -> field.toLowerCase())
                .collect(Collectors.toList());
        Assert.assertTrue(selectFields.contains("uv"));
        Assert.assertTrue(!selectFields.contains("pv"));
        Assert.assertTrue(!selectFields.contains("user_id"));
        Assert.assertTrue(!selectFields.contains("play_count"));
    }

    @Test
    void getWhereFieldTest() {
        SqlParserInfo sqlParserInfo = SqlParseUtils.getSqlParseInfo(
                "select uv from "
                        + " ( "
                        + " select * from t_1 where sys_imp_date >= '2023-07-07' and  "
                        + "sys_imp_date <= '2023-07-07' and user_id = 22 "
                        + " ) as  t_sub_1 "
                        + " where user_name_元 = 'zhangsan' order by play_count desc limit 10"
        );
        List<String> collect = sqlParserInfo.getAllFields().stream().map(field -> field.toLowerCase())
                .collect(Collectors.toList());
        Assert.assertTrue(collect.contains("user_id"));
    }
}
