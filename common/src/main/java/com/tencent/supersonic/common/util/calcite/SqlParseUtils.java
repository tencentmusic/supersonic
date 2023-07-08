package com.tencent.supersonic.common.util.calcite;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;

/**
 * sql parse utils
 */
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

            List<String> collect = sqlParserInfo.getAllFields().stream().distinct().collect(Collectors.toList());

            sqlParserInfo.setAllFields(collect);
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
        handlerField(orderList, sqlParserInfo);
    }

    /**
     * hanlder select
     *
     * @param select
     * @param sqlParserInfo
     */
    private static void handlerSelect(SqlNode select, SqlParserInfo sqlParserInfo) {
        SqlSelect sqlSelect = (SqlSelect) select;
        SqlNodeList selectList = sqlSelect.getSelectList();

        selectList.getList().forEach(list -> {
            handlerField(list, sqlParserInfo);
        });
        String tableName = handlerFrom(sqlSelect.getFrom());
        sqlParserInfo.setTableName(tableName);

        if (sqlSelect.hasWhere()) {
            handlerField(sqlSelect.getWhere(), sqlParserInfo);
        }
        if (sqlSelect.hasOrderBy()) {
            handlerField(sqlSelect.getOrderList(), sqlParserInfo);
        }
        SqlNodeList group = sqlSelect.getGroup();
        if (group != null) {
            group.forEach(groupField -> {
                handlerField(groupField, sqlParserInfo);
            });
        }
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
                SqlBasicCall sqlBasicCall =  (SqlBasicCall) from;
                SqlNode sqlNode = sqlBasicCall.getOperandList().get(0);
                SqlSelect sqlSelect = (SqlSelect) sqlNode;
                return handlerFrom(sqlSelect.getFrom());
        }
        return "";
    }

    /**
     * handler field
     *
     * @param field
     * @param sqlParserInfo
     */
    private static void handlerField(SqlNode field, SqlParserInfo sqlParserInfo) {
        SqlKind kind = field.getKind();
        switch (kind) {
            case AS:
                List<SqlNode> operandList1 = ((SqlBasicCall) field).getOperandList();
                SqlNode left_as = operandList1.get(0);
                handlerField(left_as, sqlParserInfo);
                break;
            case IDENTIFIER:
                SqlIdentifier sqlIdentifier = (SqlIdentifier) field;
                sqlParserInfo.getAllFields().add(sqlIdentifier.getSimple());
                break;
            default:
                if (field instanceof SqlBasicCall) {
                    List<SqlNode> operandList2 = ((SqlBasicCall) field).getOperandList();
                    for (int i = 0; i < operandList2.size(); i++) {
                        handlerField(operandList2.get(i), sqlParserInfo);
                    }
                }
                if (field instanceof SqlNodeList) {
                    ((SqlNodeList) field).getList().forEach(node -> {
                        handlerField(node, sqlParserInfo);
                    });
                }
                break;
        }
    }
}

