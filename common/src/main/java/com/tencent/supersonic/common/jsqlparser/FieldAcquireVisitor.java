package com.tencent.supersonic.common.jsqlparser;

import com.google.common.collect.Sets;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SelectItem;

import java.util.Set;

public class FieldAcquireVisitor extends ExpressionVisitorAdapter {

    private Set<String> fields;
    private Set<String> aliases = Sets.newHashSet();

    public FieldAcquireVisitor(Set<String> fields) {
        this.fields = fields;
    }

    @Override
    public void visit(Column column) {
        String columnName = column.getColumnName();
        fields.add(columnName);
    }

    @Override
    public void visit(SelectItem selectItem) {
        Alias alias = selectItem.getAlias();
        if (alias != null) {
            aliases.add(alias.getName());
        }

        Expression expression = selectItem.getExpression();
        if (expression != null) {
            expression.accept(this);
        }
    }
}
