package com.tencent.supersonic.common.util.jsqlparser;

import java.util.Map;
import java.util.Objects;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;

import net.sf.jsqlparser.schema.Column;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

public class FieldlValueReplaceVisitor extends ExpressionVisitorAdapter {

    private Map<String, Map<String, String>> filedNameToValueMap;

    public FieldlValueReplaceVisitor(Map<String, Map<String, String>> filedNameToValueMap) {
        this.filedNameToValueMap = filedNameToValueMap;
    }

    @Override
    public void visit(EqualsTo expr) {
        Expression leftExpression = expr.getLeftExpression();
        Expression rightExpression = expr.getRightExpression();
        if (!(rightExpression instanceof StringValue)) {
            return;
        }
        if (!(leftExpression instanceof Column)) {
            return;
        }
        if (CollectionUtils.isEmpty(filedNameToValueMap)) {
            return;
        }
        if (Objects.isNull(rightExpression) || Objects.isNull(leftExpression)) {
            return;
        }
        Column leftColumnName = (Column) leftExpression;
        StringValue rightStringValue = (StringValue) rightExpression;

        String columnName = leftColumnName.getColumnName();
        if (StringUtils.isEmpty(columnName)) {
            return;
        }
        Map<String, String> valueMap = filedNameToValueMap.get(columnName);
        if (Objects.isNull(valueMap) || valueMap.isEmpty()) {
            return;
        }
        String replaceValue = valueMap.get(rightStringValue.getValue());
        if (StringUtils.isNotEmpty(replaceValue)) {
            rightStringValue.setValue(replaceValue);
        }
    }

    public void visit(GreaterThan expr) {
        replaceComparisonExpression(expr);
    }

    public void visit(GreaterThanEquals expr) {
        replaceComparisonExpression(expr);
    }

    public void visit(MinorThanEquals expr) {
        replaceComparisonExpression(expr);
    }

    public void visit(MinorThan expr) {
        replaceComparisonExpression(expr);
    }

    public <T extends Expression> void replaceComparisonExpression(T expression) {
        if ((expression instanceof GreaterThanEquals) || (expression instanceof GreaterThan)
                || (expression instanceof MinorThanEquals) || (expression instanceof MinorThan)) {
            Expression leftExpression = ((ComparisonOperator) expression).getLeftExpression();
            Expression rightExpression = ((ComparisonOperator) expression).getRightExpression();
            if (!(leftExpression instanceof Column)) {
                return;
            }
            if (CollectionUtils.isEmpty(filedNameToValueMap)) {
                return;
            }
            if (Objects.isNull(rightExpression) || Objects.isNull(leftExpression)) {
                return;
            }
            Column leftColumnName = (Column) leftExpression;

            String columnName = leftColumnName.getColumnName();
            if (StringUtils.isEmpty(columnName)) {
                return;
            }
            Map<String, String> valueMap = filedNameToValueMap.get(columnName);
            if (Objects.isNull(valueMap) || valueMap.isEmpty()) {
                return;
            }
            for (String oriValue : valueMap.keySet()) {
                String replaceValue = valueMap.get(oriValue);
                if (StringUtils.isNotEmpty(replaceValue)) {
                    if (rightExpression instanceof LongValue) {
                        LongValue rightStringValue = (LongValue) rightExpression;
                        rightStringValue.setValue(Long.parseLong(replaceValue));
                    }
                    if (rightExpression instanceof DoubleValue) {
                        DoubleValue rightStringValue = (DoubleValue) rightExpression;
                        rightStringValue.setValue(Double.parseDouble(replaceValue));
                    }
                    if (rightExpression instanceof StringValue) {
                        StringValue rightStringValue = (StringValue) rightExpression;
                        rightStringValue.setValue(replaceValue);
                    }
                }
            }

        }
    }
}
