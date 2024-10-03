package com.tencent.supersonic.common.calcite;

import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.fun.SqlCase;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.util.SqlString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** sql parse utils */
public class SqlParseUtils {

    /**
     * get sql parseInfo
     *
     * @param sql
     * @return
     */
    public static SqlParserInfo getSqlParseInfo(String sql) {
        try {
            SqlParser parser = SqlParser.create(sql);
            SqlNode sqlNode = parser.parseQuery();
            SqlParserInfo sqlParserInfo = new SqlParserInfo();

            handlerSQL(sqlNode, sqlParserInfo);

            sqlParserInfo.setAllFields(
                    sqlParserInfo.getAllFields().stream().distinct().collect(Collectors.toList()));

            sqlParserInfo.setSelectFields(sqlParserInfo.getSelectFields().stream().distinct()
                    .collect(Collectors.toList()));

            return sqlParserInfo;
        } catch (SqlParseException e) {
            throw new RuntimeException("getSqlParseInfo", e);
        }
    }

    /**
     * hanlder sql
     *
     * @param sqlNode
     * @param sqlParserInfo
     */
    public static void handlerSQL(SqlNode sqlNode, SqlParserInfo sqlParserInfo) {
        SqlKind kind = sqlNode.getKind();

        switch (kind) {
            case SELECT:
                handlerSelect(sqlNode, sqlParserInfo);
                break;
            case ORDER_BY:
                handlerOrderBy(sqlNode, sqlParserInfo);
                break;
            default:
                break;
        }
    }

    /**
     * hanlder order by
     *
     * @param node
     * @param sqlParserInfo
     */
    private static void handlerOrderBy(SqlNode node, SqlParserInfo sqlParserInfo) {
        SqlOrderBy sqlOrderBy = (SqlOrderBy) node;

        SqlNode query = sqlOrderBy.query;

        handlerSQL(query, sqlParserInfo);

        SqlNodeList orderList = sqlOrderBy.orderList;

        Set<String> orderFields = handlerField(orderList);

        sqlParserInfo.getAllFields().addAll(orderFields);
    }

    /**
     * hanlder select
     *
     * @param select
     * @param sqlParserInfo
     */
    private static void handlerSelect(SqlNode select, SqlParserInfo sqlParserInfo) {
        List<String> allFields = sqlParserInfo.getAllFields();
        SqlSelect sqlSelect = (SqlSelect) select;
        SqlNodeList selectList = sqlSelect.getSelectList();

        selectList.getList().forEach(list -> {
            Set<String> selectFields = handlerField(list);
            sqlParserInfo.getSelectFields().addAll(selectFields);
        });
        String tableName = handlerFrom(sqlSelect.getFrom());
        sqlParserInfo.setTableName(tableName);

        Set<String> selectFields = handlerSelectField(sqlSelect);
        allFields.addAll(selectFields);
    }

    private static Set<String> handlerSelectField(SqlSelect sqlSelect) {
        Set<String> results = new HashSet<>();
        if (sqlSelect.getFrom() instanceof SqlBasicCall) {
            Set<String> formFields = handlerField(sqlSelect.getFrom());
            results.addAll(formFields);
        }

        sqlSelect.getSelectList().getList().forEach(list -> {
            Set<String> selectFields = handlerField(list);
            results.addAll(selectFields);
        });

        if (sqlSelect.hasWhere()) {
            Set<String> whereFields = handlerField(sqlSelect.getWhere());
            results.addAll(whereFields);
        }
        if (sqlSelect.hasOrderBy()) {
            Set<String> orderByFields = handlerField(sqlSelect.getOrderList());
            results.addAll(orderByFields);
        }
        SqlNodeList group = sqlSelect.getGroup();
        if (group != null) {
            group.forEach(groupField -> {
                Set<String> groupByFields = handlerField(groupField);
                results.addAll(groupByFields);
            });
        }
        return results;
    }

    /**
     * hander from
     *
     * @param from
     * @return
     */
    private static String handlerFrom(SqlNode from) {
        SqlKind kind = from.getKind();
        switch (kind) {
            case IDENTIFIER:
                SqlIdentifier sqlIdentifier = (SqlIdentifier) from;
                return sqlIdentifier.getSimple();
            case AS:
                SqlBasicCall sqlBasicCall = (SqlBasicCall) from;
                SqlNode sqlNode = sqlBasicCall.getOperandList().get(0);
                SqlSelect sqlSelect = (SqlSelect) sqlNode;
                return handlerFrom(sqlSelect.getFrom());
            default:
                break;
        }
        return "";
    }

    /**
     * handler field
     *
     * @param field
     */
    private static Set<String> handlerField(SqlNode field) {
        Set<String> fields = new HashSet<>();
        SqlKind kind = field.getKind();
        switch (kind) {
            case AS:
                List<SqlNode> operandList1 = ((SqlBasicCall) field).getOperandList();
                SqlNode leftAs = operandList1.get(0);
                fields.addAll(handlerField(leftAs));
                break;
            case IDENTIFIER:
                SqlIdentifier sqlIdentifier = (SqlIdentifier) field;
                String simpleName = sqlIdentifier.getSimple();
                if (StringUtils.isNotEmpty(simpleName)) {
                    fields.add(simpleName);
                }
                break;
            case SELECT:
                SqlSelect sqlSelect = (SqlSelect) field;
                fields.addAll(handlerSelectField(sqlSelect));
                break;
            default:
                if (field instanceof SqlBasicCall) {
                    List<SqlNode> operandList2 = ((SqlBasicCall) field).getOperandList();
                    for (int i = 0; i < operandList2.size(); i++) {
                        fields.addAll(handlerField(operandList2.get(i)));
                    }
                }
                if (field instanceof SqlNodeList) {
                    ((SqlNodeList) field).getList().forEach(node -> {
                        fields.addAll(handlerField(node));
                    });
                }
                break;
        }
        return fields;
    }

    public static String addAliasToSql(String sql) throws SqlParseException {
        SqlParser parser = SqlParser.create(sql);
        SqlNode sqlNode = parser.parseStmt();

        if (!(sqlNode instanceof SqlSelect)) {
            return sql;
        }

        SqlNodeList selectList = ((SqlSelect) sqlNode).getSelectList();
        for (SqlNode node : selectList) {
            if (node instanceof SqlBasicCall) {
                SqlBasicCall sqlBasicCall = (SqlBasicCall) node;

                List<SqlNode> operandList = sqlBasicCall.getOperandList();
                if (CollectionUtils.isNotEmpty(operandList) && operandList.size() == 1) {
                    SqlIdentifier sqlIdentifier = (SqlIdentifier) operandList.get(0);
                    String simple = sqlIdentifier.getSimple();
                    SqlBasicCall aliasedNode =
                            new SqlBasicCall(SqlStdOperatorTable.AS,
                                    new SqlNode[] {sqlBasicCall, new SqlIdentifier(
                                            simple.toLowerCase(), SqlParserPos.ZERO)},
                                    SqlParserPos.ZERO);
                    selectList.set(selectList.indexOf(node), aliasedNode);
                }
            }
        }
        SqlDialect dialect = new S2MysqlSqlDialect(S2MysqlSqlDialect.DEFAULT_CONTEXT);
        SqlString newSql = sqlNode.toSqlString(dialect);
        return newSql.getSql().replaceAll("`", "");
    }

    public static String addFieldsToSql(String sql, List<String> addFields)
            throws SqlParseException {
        if (CollectionUtils.isEmpty(addFields)) {
            return sql;
        }
        SqlParser parser = SqlParser.create(sql);
        SqlNode sqlNode = parser.parseStmt();
        SqlNodeList selectList = getSelectList(sqlNode);

        // agg to field not allow to add field
        if (Objects.isNull(selectList)) {
            return sql;
        }
        for (SqlNode node : selectList) {
            if (node instanceof SqlBasicCall) {
                return sql;
            }
        }
        Set<String> existFields = new HashSet<>();
        for (SqlNode node : selectList.getList()) {
            if (node instanceof SqlIdentifier) {
                String fieldName = ((SqlIdentifier) node).getSimple();
                existFields.add(fieldName.toLowerCase());
            }
        }

        for (String addField : addFields) {
            if (existFields.contains(addField.toLowerCase())) {
                continue;
            }
            SqlIdentifier newField = new SqlIdentifier(addField, SqlParserPos.ZERO);
            selectList.add(newField);
            existFields.add(addField.toLowerCase());
        }
        SqlDialect dialect = new S2MysqlSqlDialect(S2MysqlSqlDialect.DEFAULT_CONTEXT);
        SqlString newSql = sqlNode.toSqlString(dialect);

        return newSql.getSql().replaceAll("`", "");
    }

    private static SqlNodeList getSelectList(SqlNode sqlNode) {
        SqlKind kind = sqlNode.getKind();

        switch (kind) {
            case SELECT:
                SqlSelect sqlSelect = (SqlSelect) sqlNode;
                return sqlSelect.getSelectList();
            case ORDER_BY:
                SqlOrderBy sqlOrderBy = (SqlOrderBy) sqlNode;
                SqlSelect query = (SqlSelect) sqlOrderBy.query;
                return query.getSelectList();
            default:
                break;
        }
        return null;
    }

    public static Set<String> getFilterField(String where) {
        Set<String> result = new HashSet<>();
        try {

            SqlParser parser = SqlParser.create(where);
            SqlNode sqlNode = parser.parseExpression();
            getFieldByExpression(sqlNode, result);
            return result;
        } catch (SqlParseException e) {
            throw new RuntimeException("getSqlParseInfo", e);
        }
    }

    public static void getFieldByExpression(SqlNode sqlNode, Set<String> fields) {
        if (sqlNode instanceof SqlIdentifier) {
            SqlIdentifier sqlIdentifier = (SqlIdentifier) sqlNode;
            fields.add(sqlIdentifier.names.get(0).toLowerCase());
            return;
        }
        if (sqlNode instanceof SqlBasicCall) {
            SqlBasicCall sqlBasicCall = (SqlBasicCall) sqlNode;
            for (SqlNode operand : sqlBasicCall.getOperandList()) {
                getFieldByExpression(operand, fields);
            }
        }
    }

    public static Map getCaseExprFields(String expr) {
        SqlParser parser = SqlParser.create(expr);
        Map<String, String> ret = new HashMap();
        try {
            SqlNode sqlNodeCase = parser.parseExpression();
            if (sqlNodeCase instanceof SqlCase) {
                SqlCase sqlCase = (SqlCase) sqlNodeCase;
                if (CollectionUtils.isEmpty(sqlCase.getThenOperands())
                        || CollectionUtils.isEmpty(sqlCase.getWhenOperands())) {
                    return ret;
                }
                SqlDialect dialect = new S2MysqlSqlDialect(S2MysqlSqlDialect.DEFAULT_CONTEXT);
                int i = 0;
                for (SqlNode sqlNode : sqlCase.getWhenOperands().getList()) {
                    if (sqlNode instanceof SqlBasicCall) {
                        SqlBasicCall when = (SqlBasicCall) sqlNode;
                        if (!org.springframework.util.CollectionUtils.isEmpty(when.getOperandList())
                                && when.getOperandList().size() > 1) {
                            String value =
                                    when.getOperandList().get(1).toSqlString(dialect).getSql();
                            if (sqlCase.getThenOperands().get(i) != null) {
                                if (sqlCase.getThenOperands().get(i) instanceof SqlIdentifier) {
                                    SqlIdentifier sqlIdentifier =
                                            (SqlIdentifier) sqlCase.getThenOperands().get(i);
                                    String field = sqlIdentifier.getSimple();
                                    ret.put(value, field);
                                }
                            }
                        }
                    }
                    i++;
                }
            }
        } catch (SqlParseException e) {
            throw new RuntimeException("getSqlParseInfo", e);
        }
        return ret;
    }
}
