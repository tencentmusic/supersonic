package com.tencent.supersonic.common.util.jsqlparser;

import java.util.Set;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.schema.Column;

public class FieldAndValueAcquireVisitor extends ExpressionVisitorAdapter {

    private Set<FilterExpression> filterExpressions;

    public FieldAndValueAcquireVisitor(Set<FilterExpression> filterExpressions) {
        this.filterExpressions = filterExpressions;
    }


    @Override
    public void visit(MinorThan expr) {
        FilterExpression filterExpression = getFilterExpression(expr);
        filterExpressions.add(filterExpression);
    }

    @Override
    public void visit(EqualsTo expr) {
        FilterExpression filterExpression = getFilterExpression(expr);
        filterExpressions.add(filterExpression);
    }


    @Override
    public void visit(MinorThanEquals expr) {
        FilterExpression filterExpression = getFilterExpression(expr);
        filterExpressions.add(filterExpression);
    }


    @Override
    public void visit(GreaterThan expr) {
        FilterExpression filterExpression = getFilterExpression(expr);
        filterExpressions.add(filterExpression);
    }

    @Override
    public void visit(GreaterThanEquals expr) {
        FilterExpression filterExpression = getFilterExpression(expr);
        filterExpressions.add(filterExpression);
    }

    private FilterExpression getFilterExpression(ComparisonOperator expr) {
        Expression leftExpression = expr.getLeftExpression();
        Expression rightExpression = expr.getRightExpression();

        FilterExpression filterExpression = new FilterExpression();
        String columnName = null;
        if (leftExpression instanceof Column) {
            Column column = (Column) leftExpression;
            columnName = column.getColumnName();
            filterExpression.setFieldName(columnName);
        }
        if (rightExpression instanceof StringValue) {
            StringValue stringValue = (StringValue) rightExpression;
            filterExpression.setFieldValue(stringValue.getValue());
        }
        if (rightExpression instanceof DoubleValue) {
            DoubleValue doubleValue = (DoubleValue) rightExpression;
            filterExpression.setFieldValue(doubleValue.getValue());
        }
        if (rightExpression instanceof LongValue) {
            LongValue longValue = (LongValue) rightExpression;
            filterExpression.setFieldValue(longValue.getValue());
        }
        filterExpression.setOperator(expr.getStringExpression());
        return filterExpression;
    }
}