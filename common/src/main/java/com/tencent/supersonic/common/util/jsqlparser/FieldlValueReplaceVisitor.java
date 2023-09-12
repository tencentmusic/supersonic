package com.tencent.supersonic.common.util.jsqlparser;

import java.util.Map;
import java.util.Objects;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.StringValue;
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

}