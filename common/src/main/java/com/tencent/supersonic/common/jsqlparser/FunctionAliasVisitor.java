package com.tencent.supersonic.common.jsqlparser;

import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter;

import java.util.Objects;
import java.util.Set;

public class FunctionAliasVisitor extends SelectItemVisitorAdapter {

    private Set<String> fieldNames;

    public FunctionAliasVisitor(Set<String> fieldNames) {
        this.fieldNames = fieldNames;
    }

    @Override
    public void visit(SelectItem selectExpressionItem) {
        if (selectExpressionItem.getExpression() instanceof Function) {
            if (Objects.nonNull(selectExpressionItem.getAlias())) {
                fieldNames.add(selectExpressionItem.getAlias().getName());
            }
        }
    }
}
