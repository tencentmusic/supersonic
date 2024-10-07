package com.tencent.supersonic.common.jsqlparser;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

import java.util.Set;

public class CustomExpressionDeParser extends ExpressionDeParser {

    private Set<String> removeFieldNames;
    private boolean dealNull;
    private boolean dealNotNull;

    public CustomExpressionDeParser(Set<String> removeFieldNames, boolean dealNull,
            boolean dealNotNull) {
        this.removeFieldNames = removeFieldNames;
        this.dealNull = dealNull;
        this.dealNotNull = dealNotNull;
    }

    @Override
    public void visit(AndExpression andExpression) {
        processBinaryExpression(andExpression, " AND ");
    }

    @Override
    public void visit(OrExpression orExpression) {
        processBinaryExpression(orExpression, " OR ");
    }

    @Override
    public void visit(IsNullExpression isNullExpression) {
        if (shouldSkip(isNullExpression)) {
            // Skip this expression
        } else {
            super.visit(isNullExpression);
        }
    }

    private void processBinaryExpression(Expression binaryExpression, String operator) {
        Expression leftExpression = ((AndExpression) binaryExpression).getLeftExpression();
        Expression rightExpression = ((AndExpression) binaryExpression).getRightExpression();

        boolean leftIsNull = leftExpression instanceof IsNullExpression
                && shouldSkip((IsNullExpression) leftExpression);
        boolean rightIsNull = rightExpression instanceof IsNullExpression
                && shouldSkip((IsNullExpression) rightExpression);

        if (leftIsNull && rightIsNull) {
            // Skip both expressions
        } else if (leftIsNull) {
            rightExpression.accept(this);
        } else if (rightIsNull) {
            leftExpression.accept(this);
        } else {
            leftExpression.accept(this);
            buffer.append(operator);
            rightExpression.accept(this);
        }
    }

    private boolean shouldSkip(IsNullExpression isNullExpression) {
        if (isNullExpression.getLeftExpression() instanceof Column) {
            Column column = (Column) isNullExpression.getLeftExpression();
            String columnName = column.getColumnName();
            // Add your target column names here
            if (removeFieldNames.contains(columnName)) {
                if (isNullExpression.isNot() && dealNotNull) {
                    return true;
                } else if (!isNullExpression.isNot() && dealNull) {
                    return true;
                }
            }
        }
        return false;
    }
}
