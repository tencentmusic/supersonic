package com.tencent.supersonic.common.util.jsqlparser;

import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
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
            AndExpression andExpression = (AndExpression) whereExpression;
            Expression leftExpression = andExpression.getLeftExpression();
            Expression rightExpression = andExpression.getRightExpression();

            removeWhereExpression(leftExpression, removeFieldNames);
            removeWhereExpression(rightExpression, removeFieldNames);
        } else if (whereExpression instanceof Parenthesis) {
            removeWhereExpression(((Parenthesis) whereExpression).getExpression(), removeFieldNames);
        } else {
            removeExpressionWithConstant(whereExpression, removeFieldNames);
        }
    }

    private static void removeExpressionWithConstant(Expression expression, Set<String> removeFieldNames) {
        if (expression instanceof EqualsTo) {
            ComparisonOperator comparisonOperator = (ComparisonOperator) expression;
            String columnName = SqlParserSelectHelper.getColumnName(comparisonOperator.getLeftExpression(),
                    comparisonOperator.getRightExpression());
            if (!removeFieldNames.contains(columnName)) {
                return;
            }
            try {
                ComparisonOperator constantExpression = (ComparisonOperator) CCJSqlParserUtil.parseCondExpression(
                        JsqlConstants.EQUAL_CONSTANT);
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

