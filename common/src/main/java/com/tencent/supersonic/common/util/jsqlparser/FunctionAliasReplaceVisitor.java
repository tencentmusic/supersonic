package com.tencent.supersonic.common.util.jsqlparser;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter;

public class FunctionAliasReplaceVisitor extends SelectItemVisitorAdapter {

    private Map<String, String> aliasToActualExpression = new HashMap<>();

    @Override
    public void visit(SelectItem selectExpressionItem) {
        if (selectExpressionItem.getExpression() instanceof Function) {
            Function function = (Function) selectExpressionItem.getExpression();
            String columnName = SqlSelectHelper.getColumnName(function);
            //1.exist alias. as
            //2.alias's fieldName not equal. "sum(pv) as pv" cannot be replaced.
            if (Objects.nonNull(selectExpressionItem.getAlias()) && !selectExpressionItem.getAlias().getName()
                    .equalsIgnoreCase(columnName)) {
                aliasToActualExpression.put(selectExpressionItem.getAlias().getName(), function.toString());
                selectExpressionItem.setAlias(null);
            }
        }
    }

    public Map<String, String> getAliasToActualExpression() {
        return aliasToActualExpression;
    }
}