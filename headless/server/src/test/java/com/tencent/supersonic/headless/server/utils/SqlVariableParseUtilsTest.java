package com.tencent.supersonic.headless.server.utils;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.Param;
import com.tencent.supersonic.headless.api.pojo.SqlVariable;
import com.tencent.supersonic.headless.api.pojo.enums.VariableValueType;
import com.tencent.supersonic.headless.core.utils.SqlVariableParseUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SqlVariableParseUtilsTest {

    @Test
    void testParseSql_defaultVariableValue() {
        String sql = "select * from t_$interval$ where id = $id$ and name = $name$";
        List<SqlVariable> variables = Lists.newArrayList(mockNumSqlVariable(),
                mockExprSqlVariable(), mockStrSqlVariable());
        String actualSql = SqlVariableParseUtils.parse(sql, variables, Lists.newArrayList());
        String expectedSql = "select * from t_d where id = 1 and name = 'tom'";
        Assertions.assertEquals(expectedSql, actualSql);
    }

    @Test
    void testParseSql() {
        String sql = "select * from t_$interval$ where id = $id$ and name = $name$";
        List<SqlVariable> variables = Lists.newArrayList(mockNumSqlVariable(),
                mockExprSqlVariable(), mockStrSqlVariable());
        List<Param> params =
                Lists.newArrayList(mockIdParam(), mockNameParam(), mockIntervalParam());
        String actualSql = SqlVariableParseUtils.parse(sql, variables, params);
        String expectedSql = "select * from t_wk where id = 2 and name = 'alice'";
        Assertions.assertEquals(expectedSql, actualSql);
    }

    private SqlVariable mockNumSqlVariable() {
        return mockSqlVariable("id", VariableValueType.NUMBER, 1);
    }

    private SqlVariable mockStrSqlVariable() {
        return mockSqlVariable("name", VariableValueType.STRING, "tom");
    }

    private SqlVariable mockExprSqlVariable() {
        return mockSqlVariable("interval", VariableValueType.EXPR, "d");
    }

    private SqlVariable mockSqlVariable(String name, VariableValueType variableValueType,
            Object value) {
        SqlVariable sqlVariable = new SqlVariable();
        sqlVariable.setName(name);
        sqlVariable.setValueType(variableValueType);
        sqlVariable.setDefaultValues(Lists.newArrayList(value));
        return sqlVariable;
    }

    private Param mockIdParam() {
        return mockParam("id", "2");
    }

    private Param mockNameParam() {
        return mockParam("name", "alice");
    }

    private Param mockIntervalParam() {
        return mockParam("interval", "wk");
    }

    private Param mockParam(String name, String value) {
        return new Param(name, value);
    }
}
