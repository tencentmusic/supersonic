package com.tencent.supersonic.common.util.jsqlparser;

import com.tencent.supersonic.common.pojo.Constants;
import java.util.Set;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.OrderByVisitorAdapter;

public class OrderByAcquireVisitor extends OrderByVisitorAdapter {

    private Set<FieldExpression> fields;

    public OrderByAcquireVisitor(Set<FieldExpression> fields) {
        this.fields = fields;
    }

    @Override
    public void visit(OrderByElement orderBy) {
        Expression expression = orderBy.getExpression();
        FieldExpression fieldExpression = new FieldExpression();
        if (expression instanceof Column) {
            fieldExpression.setFieldName(((Column) expression).getColumnName());
        }
        if (expression instanceof Function) {
            Function function = (Function) expression;
            //List<Expression> expressions = function.getParameters().getExpressions();
            ExpressionList<?> expressions = function.getParameters();
            for (Expression column : expressions) {
                if (column instanceof Column) {
                    fieldExpression.setFieldName(((Column) column).getColumnName());
                }
            }
        }
        String operator = Constants.ASC_UPPER;
        if (!orderBy.isAsc()) {
            operator = Constants.DESC_UPPER;
        }
        fieldExpression.setOperator(operator);
        fields.add(fieldExpression);
        super.visit(orderBy);
    }
}