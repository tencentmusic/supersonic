package com.tencent.supersonic.common.util.jsqlparser;

import com.tencent.supersonic.common.util.DatePeriodEnum;
import java.util.ArrayList;
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
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.collections.CollectionUtils;

public class FieldAndValueAcquireVisitor extends ExpressionVisitorAdapter {

    private Set<FilterExpression> filterExpressions;

    public FieldAndValueAcquireVisitor(Set<FilterExpression> filterExpressions) {
        this.filterExpressions = filterExpressions;
    }

    public void visit(LikeExpression expr) {
        Expression leftExpression = expr.getLeftExpression();
        Expression rightExpression = expr.getRightExpression();

        FilterExpression filterExpression = new FilterExpression();
        String columnName = null;
        if (leftExpression instanceof Column) {
            Column column = (Column) leftExpression;
            columnName = column.getColumnName();
            filterExpression.setFieldName(columnName);
        }
        filterExpression.setFieldValue(getFieldValue(rightExpression));
        filterExpression.setOperator(expr.getStringExpression());
        filterExpressions.add(filterExpression);
    }

    public void visit(InExpression expr) {
        FilterExpression filterExpression = new FilterExpression();
        Expression leftExpression = expr.getLeftExpression();
        if (!(leftExpression instanceof Column)) {
            return;
        }
        filterExpression.setFieldName(((Column) leftExpression).getColumnName());
        filterExpression.setOperator(JsqlConstants.IN);
        ItemsList rightItemsList = expr.getRightItemsList();
        filterExpression.setFieldValue(rightItemsList);
        List<Object> result = new ArrayList<>();
        if (rightItemsList instanceof ExpressionList) {
            ExpressionList rightExpressionList = (ExpressionList) rightItemsList;
            List<Expression> expressions = rightExpressionList.getExpressions();
            if (CollectionUtils.isNotEmpty(expressions)) {
                for (Expression expression : expressions) {
                    result.add(getFieldValue(expression));
                }
            }
        }
        filterExpression.setFieldValue(result);
        filterExpressions.add(filterExpression);
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
            Column field = getColumn(leftExpressionFunction);
            if (Objects.isNull(field)) {
                return filterExpression;
            }
            String functionName = leftExpressionFunction.getName().toUpperCase();
            filterExpression.setFieldName(field.getColumnName());
            filterExpression.setFunction(functionName);
            filterExpression.setOperator(expr.getStringExpression());
            //deal with DAY/WEEK function
            List<DatePeriodEnum> collect = Arrays.stream(DatePeriodEnum.values()).collect(Collectors.toList());
            DatePeriodEnum periodEnum = DatePeriodEnum.get(functionName);
            if (Objects.nonNull(periodEnum) && collect.contains(periodEnum)) {
                filterExpression.setFieldValue(getFieldValue(rightExpression) + periodEnum.getChName());
                return filterExpression;
            } else {
                //deal with aggregate function
                filterExpression.setFieldValue(getFieldValue(rightExpression));
                return filterExpression;
            }
        }
        filterExpression.setFieldValue(getFieldValue(rightExpression));
        filterExpression.setOperator(expr.getStringExpression());
        return filterExpression;
    }

    private Column getColumn(Function leftExpressionFunction) {
        List<Expression> leftExpressions = leftExpressionFunction.getParameters().getExpressions();
        if (CollectionUtils.isEmpty(leftExpressions) || leftExpressions.size() < 1) {
            return null;
        }
        return (Column) leftExpressions.get(0);
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
