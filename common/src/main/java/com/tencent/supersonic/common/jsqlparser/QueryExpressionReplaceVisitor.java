package com.tencent.supersonic.common.jsqlparser;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;

public class QueryExpressionReplaceVisitor extends ExpressionVisitorAdapter {

    private Map<String, String> fieldExprMap;
    private String lastColumnName;

    public QueryExpressionReplaceVisitor(Map<String, String> fieldExprMap) {
        this.fieldExprMap = fieldExprMap;
    }

    protected void visitBinaryExpression(BinaryExpression expr) {
        expr.setLeftExpression(replace(expr.getLeftExpression(), fieldExprMap));
        expr.setRightExpression(replace(expr.getRightExpression(), fieldExprMap));
    }

    public void visit(SelectItem selectExpressionItem) {
        Expression expression = selectExpressionItem.getExpression();
        String toReplace = "";
        if (expression instanceof Function) {
            Function leftFunc = (Function) expression;
            if (Objects.nonNull(leftFunc.getParameters())
                    && leftFunc.getParameters().getExpressions().get(0) instanceof Column) {
                Column column = (Column) leftFunc.getParameters().getExpressions().get(0);
                lastColumnName = column.getColumnName();
                toReplace = getReplaceExpr(leftFunc, fieldExprMap);
            }
        } else if (expression instanceof Column) {
            Column column = (Column) expression;
            lastColumnName = column.getColumnName();
            toReplace = getReplaceExpr((Column) expression, fieldExprMap);
        } else if (expression instanceof BinaryExpression) {
            BinaryExpression expr = (BinaryExpression) expression;
            expr.setLeftExpression(replace(expr.getLeftExpression(), fieldExprMap));
            expr.setRightExpression(replace(expr.getRightExpression(), fieldExprMap));
        }

        if (expression instanceof BinaryExpression) {
            BinaryExpression binaryExpression = (BinaryExpression) expression;
            visitBinaryExpression(binaryExpression);
        }

        if (expression instanceof Parenthesis) {
            replace(expression, fieldExprMap);
        }

        if (!toReplace.isEmpty()) {
            Expression toReplaceExpr = getExpression(toReplace);
            if (Objects.nonNull(toReplaceExpr)) {
                selectExpressionItem.setExpression(toReplaceExpr);
                if (Objects.isNull(selectExpressionItem.getAlias())) {
                    selectExpressionItem.setAlias(new Alias(lastColumnName, true));
                }
            }
        }
    }

    public static Expression replace(Expression expression, Map<String, String> fieldExprMap) {
        String toReplace = "";
        if (expression instanceof Function) {
            Function function = (Function) expression;
            if (function.getParameters().getExpressions().get(0) instanceof Column) {
                toReplace = getReplaceExpr((Function) expression, fieldExprMap);
            }
        }
        if (expression instanceof Column) {
            toReplace = getReplaceExpr((Column) expression, fieldExprMap);
        }
        if (expression instanceof BinaryExpression) {
            BinaryExpression binaryExpression = (BinaryExpression) expression;
            binaryExpression
                    .setLeftExpression(replace(binaryExpression.getLeftExpression(), fieldExprMap));
            binaryExpression.setRightExpression(
                    replace(binaryExpression.getRightExpression(), fieldExprMap));
        }
        if (expression instanceof Parenthesis) {
            Parenthesis parenthesis = (Parenthesis) expression;
            parenthesis.setExpression(replace(parenthesis.getExpression(), fieldExprMap));
        }

        if (!toReplace.isEmpty()) {
            Expression replace = getExpression(toReplace);
            if (Objects.nonNull(replace)) {
                return replace;
            }
        }
        return expression;
    }

    public static Expression getExpression(String expr) {
        if (expr.isEmpty()) {
            return null;
        }
        try {
            Expression expression = CCJSqlParserUtil.parseExpression(expr);
            return expression;
        } catch (Exception e) {
            return null;
        }
    }

    public static String getReplaceExpr(Column column, Map<String, String> fieldExprMap) {
        return fieldExprMap.containsKey(column.getColumnName())
                ? fieldExprMap.get(column.getColumnName())
                : "";
    }

    public static String getReplaceExpr(Function function, Map<String, String> fieldExprMap) {
        Column column = (Column) function.getParameters().getExpressions().get(0);
        String expr = getReplaceExpr(column, fieldExprMap);
        // if metric expr itself has agg function then replace original function in the SQL
        if (StringUtils.isBlank(expr)) {
            return expr;
        } else if (!SqlSelectFunctionHelper.getAggregateFunctions(expr).isEmpty()) {
            return expr;
        } else {
            String col = getReplaceExpr(column, fieldExprMap);
            column.setColumnName(col);
            return function.toString();
        }
    }
}
