package com.tencent.supersonic.common.util.jsqlparser;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter;

public class FunctionAliasReplaceVisitor extends SelectItemVisitorAdapter {

    private Map<String, String> aliasToActualExpression = new HashMap<>();

    @Override
    public void visit(SelectExpressionItem selectExpressionItem) {
        if (selectExpressionItem.getExpression() instanceof Function) {
            Function function = (Function) selectExpressionItem.getExpression();
            if (Objects.nonNull(selectExpressionItem.getAlias())) {
                aliasToActualExpression.put(selectExpressionItem.getAlias().getName(), function.toString());
                selectExpressionItem.setAlias(null);
            }
        }
    }

    public Map<String, String> getAliasToActualExpression() {
        return aliasToActualExpression;
    }
}