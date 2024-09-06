package com.tencent.supersonic.headless.core.translator.calcite.sql.node;

import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;

import java.util.Set;

public class FilterNode extends SemanticNode {

    public static void getFilterField(SqlNode sqlNode, Set<String> fields) {
        if (sqlNode instanceof SqlIdentifier) {
            SqlIdentifier sqlIdentifier = (SqlIdentifier) sqlNode;
            fields.add(sqlIdentifier.names.get(0).toLowerCase());
            return;
        }
        if (sqlNode instanceof SqlBasicCall) {
            SqlBasicCall sqlBasicCall = (SqlBasicCall) sqlNode;
            for (SqlNode operand : sqlBasicCall.getOperandList()) {
                getFilterField(operand, fields);
            }
        }
    }

    public static boolean isMatchDataSource(Set<String> measures) {
        return false;
    }
}
