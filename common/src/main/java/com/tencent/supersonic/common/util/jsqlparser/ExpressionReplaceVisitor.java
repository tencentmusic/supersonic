package com.tencent.supersonic.common.util.jsqlparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;

public class ExpressionReplaceVisitor extends ExpressionVisitorAdapter {

    private Map<String, String> fieldExprMap;


    public ExpressionReplaceVisitor(Map<String, String> fieldExprMap) {
        this.fieldExprMap = fieldExprMap;
    }

    protected void visitBinaryExpression(BinaryExpression expr) {
        Expression left = expr.getLeftExpression();
        Expression right = expr.getRightExpression();
        Boolean leftVisited = false;
        Boolean rightVisited = false;
        if (left instanceof Function) {
            Function leftFunc = (Function) left;
            if (visitFunction(leftFunc)) {
                leftVisited = true;
            }
        }
        if (right instanceof Function) {
            Function function = (Function) right;
            if (visitFunction(function)) {
                rightVisited = true;
            }
        }
        if (left instanceof Column) {
            Expression expression = QueryExpressionReplaceVisitor.getExpression(
                    QueryExpressionReplaceVisitor.getReplaceExpr((Column) left, fieldExprMap));
            if (Objects.nonNull(expression)) {
                expr.setLeftExpression(expression);
                leftVisited = true;
            }
        }
        if (right instanceof Column) {
            Expression expression = QueryExpressionReplaceVisitor.getExpression(
                    QueryExpressionReplaceVisitor.getReplaceExpr((Column) right, fieldExprMap));
            if (Objects.nonNull(expression)) {
                expr.setRightExpression(expression);
                rightVisited = true;
            }
        }
        if (!leftVisited) {
            expr.getLeftExpression().accept(this);
        }
        if (!rightVisited) {
            expr.getRightExpression().accept(this);
        }
    }

    private boolean visitFunction(Function function) {
        if (function.getParameters().getExpressions().get(0) instanceof Column) {
            Expression expression = QueryExpressionReplaceVisitor.getExpression(
                    QueryExpressionReplaceVisitor.getReplaceExpr(function, fieldExprMap));
            if (Objects.nonNull(expression)) {
                List<Expression> expressions = new ArrayList<>();
                expressions.add(expression);
                for (int i = 1; i < function.getParameters().getExpressions().size(); i++) {
                    expressions.add(function.getParameters().getExpressions().get(i));
                }
                function.getParameters().setExpressions(expressions);
                return true;
            }
        }
        return false;
    }
}
