package com.tencent.supersonic.common.jsqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class FieldValueReplaceVisitor extends ExpressionVisitorAdapter {

    private final boolean exactReplace;

    private final Map<String, Map<String, String>> filedNameToValueMap;

    public FieldValueReplaceVisitor(boolean exactReplace,
            Map<String, Map<String, String>> filedNameToValueMap) {
        this.filedNameToValueMap = filedNameToValueMap;
        this.exactReplace = exactReplace;
    }

    @Override
    public void visit(EqualsTo expr) {
        replaceComparisonExpression(expr);
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

    public void visit(InExpression inExpression) {
        if (!(inExpression.getLeftExpression() instanceof Column)) {
            return;
        }
        Column column = (Column) inExpression.getLeftExpression();
        Map<String, String> valueMap = filedNameToValueMap.get(column.getColumnName());
        if (!(inExpression.getRightExpression() instanceof ExpressionList)) {
            return;
        }
        ExpressionList rightItemsList = (ExpressionList) inExpression.getRightExpression();
        List<Expression> expressions = rightItemsList.getExpressions();
        List<String> values = new ArrayList<>();
        expressions.stream().forEach(o -> {
            if (o instanceof StringValue) {
                values.add(((StringValue) o).getValue());
            }
        });
        if (valueMap == null || CollectionUtils.isEmpty(values)) {
            return;
        }
        List<Expression> newExpressions = new ArrayList<>();
        values.stream().forEach(o -> {
            String replaceValue = valueMap.getOrDefault(o, o);
            StringValue stringValue = new StringValue(replaceValue);
            newExpressions.add(stringValue);
        });
        rightItemsList.setExpressions(newExpressions);
        inExpression.setRightExpression(rightItemsList);
    }

    public <T extends Expression> void replaceComparisonExpression(T expression) {
        Expression leftExpression = ((ComparisonOperator) expression).getLeftExpression();
        Expression rightExpression = ((ComparisonOperator) expression).getRightExpression();

        if (!(leftExpression instanceof Column || leftExpression instanceof Function)) {
            return;
        }
        if (CollectionUtils.isEmpty(filedNameToValueMap)) {
            return;
        }
        if (Objects.isNull(rightExpression) || Objects.isNull(leftExpression)) {
            return;
        }
        String columnName = SqlSelectHelper.getColumnName(leftExpression);
        if (StringUtils.isEmpty(columnName)) {
            return;
        }
        Map<String, String> valueMap = filedNameToValueMap.get(columnName);
        if (Objects.isNull(valueMap) || valueMap.isEmpty()) {
            return;
        }
        if (rightExpression instanceof LongValue) {
            LongValue rightStringValue = (LongValue) rightExpression;
            String replaceValue =
                    getReplaceValue(valueMap, String.valueOf(rightStringValue.getValue()));
            if (StringUtils.isNotEmpty(replaceValue)) {
                rightStringValue.setValue(Long.parseLong(replaceValue));
            }
        }
        if (rightExpression instanceof DoubleValue) {
            DoubleValue rightStringValue = (DoubleValue) rightExpression;
            String replaceValue =
                    getReplaceValue(valueMap, String.valueOf(rightStringValue.getValue()));
            if (StringUtils.isNotEmpty(replaceValue)) {
                rightStringValue.setValue(Double.parseDouble(replaceValue));
            }
        }
        if (rightExpression instanceof StringValue) {
            StringValue rightStringValue = (StringValue) rightExpression;
            String replaceValue =
                    getReplaceValue(valueMap, String.valueOf(rightStringValue.getValue()));
            if (StringUtils.isNotEmpty(replaceValue)) {
                rightStringValue.setValue(replaceValue);
            }
        }
    }

    private String getReplaceValue(Map<String, String> valueMap, String beforeValue) {
        String afterValue = valueMap.get(String.valueOf(beforeValue));
        if (StringUtils.isEmpty(afterValue)) {
            return SqlReplaceHelper.getReplaceValue(beforeValue, valueMap, exactReplace);
        }
        return afterValue;
    }
}
