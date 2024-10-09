package com.tencent.supersonic.common.jsqlparser;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.schema.Column;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class FiledNameReplaceVisitor extends ExpressionVisitorAdapter {
    private Map<String, Set<String>> fieldValueToFieldNames;

    public FiledNameReplaceVisitor(Map<String, Set<String>> fieldValueToFieldNames) {
        this.fieldValueToFieldNames = fieldValueToFieldNames;
    }

    @Override
    public void visit(EqualsTo expr) {
        replaceFieldNameByFieldValue(expr);
    }

    @Override
    public void visit(LikeExpression expr) {
        replaceFieldNameByFieldValue(expr);
    }

    private void replaceFieldNameByFieldValue(BinaryExpression expr) {
        Expression leftExpression = expr.getLeftExpression();
        Expression rightExpression = expr.getRightExpression();

        if (!(rightExpression instanceof StringValue) || !(leftExpression instanceof Column)
                || CollectionUtils.isEmpty(fieldValueToFieldNames)
                || Objects.isNull(rightExpression) || Objects.isNull(leftExpression)) {
            return;
        }

        Column leftColumn = (Column) leftExpression;
        StringValue rightStringValue = (StringValue) rightExpression;

        Set<String> fieldNames = fieldValueToFieldNames.get(rightStringValue.getValue());
        if (!CollectionUtils.isEmpty(fieldNames)
                && !fieldNames.contains(leftColumn.getColumnName())) {
            leftColumn.setColumnName(fieldNames.stream().findFirst().get());
        }
    }
}
