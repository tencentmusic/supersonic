package com.tencent.supersonic.common.util.jsqlparser;

import java.util.Map;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.OrderByVisitorAdapter;

public class OrderByReplaceVisitor extends OrderByVisitorAdapter {

    ParseVisitorHelper parseVisitorHelper = new ParseVisitorHelper();
    private Map<String, String> fieldToBizName;

    public OrderByReplaceVisitor(Map<String, String> fieldToBizName) {
        this.fieldToBizName = fieldToBizName;
    }

    @Override
    public void visit(OrderByElement orderBy) {
        Expression expression = orderBy.getExpression();
        if (expression instanceof Column) {
            parseVisitorHelper.replaceColumn((Column) expression, fieldToBizName);
        }
        super.visit(orderBy);
    }
}