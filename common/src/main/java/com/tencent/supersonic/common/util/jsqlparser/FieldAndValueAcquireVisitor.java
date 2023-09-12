package com.tencent.supersonic.common.util.jsqlparser;

import com.tencent.supersonic.common.util.DatePeriodEnum;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.collections.CollectionUtils;

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
        if (leftExpression instanceof Function) {
            Function leftExpressionFunction = (Function) leftExpression;
            String dateFunction = leftExpressionFunction.getName().toUpperCase();
            List<DatePeriodEnum> collect = Arrays.stream(DatePeriodEnum.values()).collect(Collectors.toList());
            DatePeriodEnum periodEnum = DatePeriodEnum.get(dateFunction);
            if (Objects.nonNull(periodEnum) && collect.contains(periodEnum)) {
                List<Expression> leftExpressions = leftExpressionFunction.getParameters().getExpressions();
                if (CollectionUtils.isEmpty(leftExpressions) || leftExpressions.size() < 1) {
                    return filterExpression;
                }
                Column field = (Column) leftExpressions.get(0);
                filterExpression.setFieldName(field.getColumnName());
                filterExpression.setFieldValue(getFieldValue(rightExpression) + periodEnum.getChName());
                filterExpression.setOperator(expr.getStringExpression());
                return filterExpression;
            }
        }

        filterExpression.setFieldValue(getFieldValue(rightExpression));
        filterExpression.setOperator(expr.getStringExpression());
        return filterExpression;
    }

    private Object getFieldValue(Expression rightExpression) {
        if (rightExpression instanceof StringValue) {
            StringValue stringValue = (StringValue) rightExpression;
            return stringValue.getValue();
        }
        if (rightExpression instanceof DoubleValue) {
            DoubleValue doubleValue = (DoubleValue) rightExpression;
            return doubleValue.getValue();
        }
        if (rightExpression instanceof LongValue) {
            LongValue longValue = (LongValue) rightExpression;
            return longValue.getValue();
        }
        return null;
    }
}