package com.tencent.supersonic.common.util.jsqlparser;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import org.springframework.util.CollectionUtils;

public class FiledNameReplaceVisitor extends ExpressionVisitorAdapter {

    private Map<String, Set<String>> fieldValueToFieldNames;

    public FiledNameReplaceVisitor(Map<String, Set<String>> fieldValueToFieldNames) {
        this.fieldValueToFieldNames = fieldValueToFieldNames;
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
        if (CollectionUtils.isEmpty(fieldValueToFieldNames)) {
            return;
        }
        if (Objects.isNull(rightExpression) || Objects.isNull(leftExpression)) {
            return;
        }
        Column leftColumnName = (Column) leftExpression;
        StringValue rightStringValue = (StringValue) rightExpression;

        Set<String> fieldNames = fieldValueToFieldNames.get(rightStringValue.getValue());
        if (!CollectionUtils.isEmpty(fieldNames) && !fieldNames.contains(leftColumnName.getColumnName())) {
            leftColumnName.setColumnName(fieldNames.stream().findFirst().get());
        }
    }

}