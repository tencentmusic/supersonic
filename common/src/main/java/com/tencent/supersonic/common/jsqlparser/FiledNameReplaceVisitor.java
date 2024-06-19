package com.tencent.supersonic.common.jsqlparser;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.schema.Column;
import org.springframework.util.CollectionUtils;

public class FiledNameReplaceVisitor extends ExpressionVisitorAdapter {

    public static final String PREFIX = "%";
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
        if (!(rightExpression instanceof StringValue)) {
            return;
        }
        if (!(leftExpression instanceof Column)) {
            return;
        }
        if (CollectionUtils.isEmpty(fieldValueToFieldNames)) {
            return;
        }
        if (Objects.isNull(rightExpression) || Objects.isNull(leftExpression)) {
            return;
        }
        Column leftColumnName = (Column) leftExpression;
        StringValue rightStringValue = (StringValue) rightExpression;

        if (expr instanceof LikeExpression) {
            String value = getValue(rightStringValue.getValue());
            rightStringValue.setValue(value);
        }

        Set<String> fieldNames = fieldValueToFieldNames.get(rightStringValue.getValue());
        if (!CollectionUtils.isEmpty(fieldNames) && !fieldNames.contains(leftColumnName.getColumnName())) {
            leftColumnName.setColumnName(fieldNames.stream().findFirst().get());
        }
    }

    private String getValue(String value) {
        if (value.startsWith(PREFIX)) {
            value = value.substring(1);
        }
        if (value.endsWith(PREFIX)) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

}
