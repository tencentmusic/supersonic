package com.tencent.supersonic.common.util.jsqlparser;

import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;

/**
 * Sql Parser remove Helper
 */
@Slf4j
public class SqlParserRemoveHelper {

    public static String removeWhereCondition(String sql, Set<String> removeFieldNames) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        selectBody.accept(new SelectVisitorAdapter() {
            @Override
            public void visit(PlainSelect plainSelect) {
                removeWhereCondition(plainSelect.getWhere(), removeFieldNames);
            }
        });
        return selectStatement.toString();
    }

    private static void removeWhereCondition(Expression whereExpression, Set<String> removeFieldNames) {
        if (whereExpression == null) {
            return;
        }
        removeWhereExpression(whereExpression, removeFieldNames);
    }

    private static void removeWhereExpression(Expression whereExpression, Set<String> removeFieldNames) {
        if (SqlParserSelectHelper.isLogicExpression(whereExpression)) {
            BinaryExpression binaryExpression = (BinaryExpression) whereExpression;
            Expression leftExpression = binaryExpression.getLeftExpression();
            Expression rightExpression = binaryExpression.getRightExpression();

            removeWhereExpression(leftExpression, removeFieldNames);
            removeWhereExpression(rightExpression, removeFieldNames);
        } else if (whereExpression instanceof Parenthesis) {
            removeWhereExpression(((Parenthesis) whereExpression).getExpression(), removeFieldNames);
        } else {
            removeExpressionWithConstant(whereExpression, removeFieldNames);
        }
    }

    public static String getConstant(Expression expression) {
        String constant = JsqlConstants.EQUAL_CONSTANT;
        if (expression instanceof GreaterThanEquals) {
            constant = JsqlConstants.GREATER_THAN_EQUALS_CONSTANT;
        } else if (expression instanceof MinorThanEquals) {
            constant = JsqlConstants.MINOR_THAN_EQUALS_CONSTANT;
        } else if (expression instanceof GreaterThan) {
            constant = JsqlConstants.GREATER_THAN_CONSTANT;
        } else if (expression instanceof MinorThan) {
            constant = JsqlConstants.MINOR_THAN_CONSTANT;
        }
        return constant;
    }

    private static void removeExpressionWithConstant(Expression expression, Set<String> removeFieldNames) {
        if (expression instanceof EqualsTo
                || expression instanceof GreaterThanEquals
                || expression instanceof GreaterThan
                || expression instanceof MinorThanEquals
                || expression instanceof MinorThan) {
            ComparisonOperator comparisonOperator = (ComparisonOperator) expression;
            String columnName = SqlParserSelectHelper.getColumnName(comparisonOperator.getLeftExpression(),
                    comparisonOperator.getRightExpression());
            if (!removeFieldNames.contains(columnName)) {
                return;
            }
            String constant = getConstant(expression);
            try {
                ComparisonOperator constantExpression = (ComparisonOperator) CCJSqlParserUtil.parseCondExpression(
                        constant);
                comparisonOperator.setLeftExpression(constantExpression.getLeftExpression());
                comparisonOperator.setRightExpression(constantExpression.getRightExpression());
                comparisonOperator.setASTNode(constantExpression.getASTNode());
            } catch (JSQLParserException e) {
                log.error("JSQLParserException", e);
            }
        }
        if (expression instanceof InExpression) {
            InExpression inExpression = (InExpression) expression;
            String columnName = SqlParserSelectHelper.getColumnName(inExpression.getLeftExpression(),
                    inExpression.getRightExpression());
            if (!removeFieldNames.contains(columnName)) {
                return;
            }
            try {
                InExpression constantExpression = (InExpression) CCJSqlParserUtil.parseCondExpression(
                        JsqlConstants.IN_CONSTANT);
                inExpression.setLeftExpression(constantExpression.getLeftExpression());
                inExpression.setRightItemsList(constantExpression.getRightItemsList());
                inExpression.setASTNode(constantExpression.getASTNode());
            } catch (JSQLParserException e) {
                log.error("JSQLParserException", e);
            }
        }
        if (expression instanceof LikeExpression) {
            LikeExpression likeExpression = (LikeExpression) expression;
            String columnName = SqlParserSelectHelper.getColumnName(likeExpression.getLeftExpression(),
                    likeExpression.getRightExpression());
            if (!removeFieldNames.contains(columnName)) {
                return;
            }
            try {
                LikeExpression constantExpression = (LikeExpression) CCJSqlParserUtil.parseCondExpression(
                        JsqlConstants.LIKE_CONSTANT);
                likeExpression.setLeftExpression(constantExpression.getLeftExpression());
                likeExpression.setRightExpression(constantExpression.getRightExpression());
            } catch (JSQLParserException e) {
                log.error("JSQLParserException", e);
            }
        }
    }

    public static String removeHavingCondition(String sql, Set<String> removeFieldNames) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        selectBody.accept(new SelectVisitorAdapter() {
            @Override
            public void visit(PlainSelect plainSelect) {
                removeWhereCondition(plainSelect.getHaving(), removeFieldNames);
            }
        });
        return selectStatement.toString();
    }

    public static String removeWhere(String sql, List<String> fields) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        Expression where = plainSelect.getWhere();

        if (where == null) {
            return sql;
        } else {
            where.accept(new FilterRemoveVisitor(fields));
            plainSelect.setWhere(where);
        }
        return selectStatement.toString();
    }

}

