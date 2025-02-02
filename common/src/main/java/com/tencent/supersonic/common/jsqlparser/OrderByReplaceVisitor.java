package com.tencent.supersonic.common.jsqlparser;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.OrderByVisitorAdapter;

import java.util.Map;

public class OrderByReplaceVisitor extends OrderByVisitorAdapter {

    private final double replaceMatchThreshold;
    private Map<String, String> fieldNameMap;

    public OrderByReplaceVisitor(Map<String, String> fieldNameMap, boolean exactReplace) {
        this.fieldNameMap = fieldNameMap;
        if (exactReplace) {
            this.replaceMatchThreshold = 1.0;
        } else {
            this.replaceMatchThreshold = 0.4;
        }
    }

    @Override
    public void visit(OrderByElement orderBy) {
        Expression expression = orderBy.getExpression();
        if (expression instanceof Column) {
            SqlReplaceHelper.replaceColumn((Column) expression, fieldNameMap,
                    replaceMatchThreshold);
        }
        if (expression instanceof Function) {
            SqlReplaceHelper.replaceFunction((Function) expression, fieldNameMap,
                    replaceMatchThreshold);
        }
        super.visit(orderBy);
    }

}
