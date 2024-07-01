package com.tencent.supersonic.common.jsqlparser;

import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
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
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.collections.CollectionUtils;

public class FieldAndValueAcquireVisitor extends ExpressionVisitorAdapter {

    private Set<FieldExpression> fieldExpressions;

    public FieldAndValueAcquireVisitor(Set<FieldExpression> fieldExpressions) {
        this.fieldExpressions = fieldExpressions;
    }

    public void visit(LikeExpression expr) {
        Expression leftExpression = expr.getLeftExpression();
        Expression rightExpression = expr.getRightExpression();

        FieldExpression fieldExpression = new FieldExpression();
        String columnName = null;
        if (leftExpression instanceof Column) {
            Column column = (Column) leftExpression;
            columnName = column.getColumnName();
            fieldExpression.setFieldName(columnName);
        }
        fieldExpression.setFieldValue(getFieldValue(rightExpression));
        fieldExpression.setOperator(expr.getStringExpression());
        fieldExpressions.add(fieldExpression);
    }

    public void visit(InExpression expr) {
        FieldExpression fieldExpression = new FieldExpression();
        Expression leftExpression = expr.getLeftExpression();
        if (!(leftExpression instanceof Column)) {
            return;
        }
        fieldExpression.setFieldName(((Column) leftExpression).getColumnName());
        fieldExpression.setOperator(JsqlConstants.IN);
        Expression rightItemsList = expr.getRightExpression();
        fieldExpression.setFieldValue(rightItemsList);
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
        fieldExpression.setFieldValue(result);
        fieldExpressions.add(fieldExpression);
    }

    @Override
    public void visit(MinorThan expr) {
        FieldExpression fieldExpression = getFilterExpression(expr);
        fieldExpressions.add(fieldExpression);
    }

    @Override
    public void visit(EqualsTo expr) {
        FieldExpression fieldExpression = getFilterExpression(expr);
        fieldExpressions.add(fieldExpression);
    }

    @Override
    public void visit(MinorThanEquals expr) {
        FieldExpression fieldExpression = getFilterExpression(expr);
        fieldExpressions.add(fieldExpression);
    }

    @Override
    public void visit(GreaterThan expr) {
        FieldExpression fieldExpression = getFilterExpression(expr);
        fieldExpressions.add(fieldExpression);
    }

    @Override
    public void visit(GreaterThanEquals expr) {
        FieldExpression fieldExpression = getFilterExpression(expr);
        fieldExpressions.add(fieldExpression);
    }

    private FieldExpression getFilterExpression(ComparisonOperator expr) {
        Expression leftExpression = expr.getLeftExpression();
        Expression rightExpression = expr.getRightExpression();

        FieldExpression fieldExpression = new FieldExpression();
        String columnName = null;
        if (leftExpression instanceof Column) {
            Column column = (Column) leftExpression;
            columnName = column.getColumnName();
            fieldExpression.setFieldName(columnName);
        }
        if (leftExpression instanceof Function) {
            Function leftExpressionFunction = (Function) leftExpression;
            Column field = getColumn(leftExpressionFunction);
            if (Objects.isNull(field)) {
                return fieldExpression;
            }
            String functionName = leftExpressionFunction.getName().toUpperCase();
            fieldExpression.setFieldName(field.getColumnName());
            fieldExpression.setFunction(functionName);
            fieldExpression.setOperator(expr.getStringExpression());
            //deal with DAY/WEEK function
            List<DatePeriodEnum> collect = Arrays.stream(DatePeriodEnum.values()).collect(Collectors.toList());
            DatePeriodEnum periodEnum = DatePeriodEnum.get(functionName);
            if (Objects.nonNull(periodEnum) && collect.contains(periodEnum)) {
                fieldExpression.setFieldValue(getFieldValue(rightExpression) + periodEnum.getChName());
                return fieldExpression;
            } else {
                //deal with aggregate function
                fieldExpression.setFieldValue(getFieldValue(rightExpression));
                return fieldExpression;
            }
        }
        fieldExpression.setFieldValue(getFieldValue(rightExpression));
        fieldExpression.setOperator(expr.getStringExpression());
        return fieldExpression;
    }

    private Column getColumn(Function leftExpressionFunction) {
        //List<Expression> leftExpressions = leftExpressionFunction.getParameters().getExpressions();
        ExpressionList<?> leftExpressions = leftExpressionFunction.getParameters();
        if (CollectionUtils.isEmpty(leftExpressions)) {
            return null;
        }
        if (!(leftExpressions.get(0) instanceof Column)) {
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
