package com.tencent.supersonic.common.jsqlparser;

import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.schema.Column;

import java.util.Set;

public class FieldAcquireVisitor extends ExpressionVisitorAdapter {

    private Set<String> fields;

    public FieldAcquireVisitor(Set<String> fields) {
        this.fields = fields;
    }

    @Override
    public void visit(Column column) {
        String columnName = column.getColumnName();
        fields.add(columnName);
    }
}
