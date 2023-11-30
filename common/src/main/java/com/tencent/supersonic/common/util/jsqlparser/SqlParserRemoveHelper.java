package com.tencent.supersonic.common.util.jsqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Sql Parser remove Helper
 */
@Slf4j
public class SqlParserRemoveHelper {

    public static String removeSelect(String sql, Set<String> fields) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        if (selectStatement == null) {
            return sql;
        }
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        List<SelectItem> selectItems = ((PlainSelect) selectBody).getSelectItems();
        selectItems.removeIf(selectItem -> {
            if (selectItem instanceof SelectExpressionItem) {
                SelectExpressionItem selectExpressionItem = (SelectExpressionItem) selectItem;
                String columnName = SqlParserSelectHelper.getColumnName(selectExpressionItem.getExpression());
                return fields.contains(columnName);
            }
            return false;
        });
        return selectStatement.toString();
    }

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
        sql = removeNumberCondition(selectStatement.toString());
        return sql;
    }

    private static void removeWhereCondition(Expression whereExpression, Set<String> removeFieldNames) {
        if (whereExpression == null) {
            return;
        }
        removeWhereExpression(whereExpression, removeFieldNames);
    }

    public static String removeNumberCondition(String sql) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        if (selectStatement == null) {
            return sql;
        }
        SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        Expression where = ((PlainSelect) selectBody).getWhere();
        Expression having = ((PlainSelect) selectBody).getHaving();
        where = filteredWhereExpression(where);
        having = filteredWhereExpression(having);
        ((PlainSelect) selectBody).setWhere(where);
        ((PlainSelect) selectBody).setHaving(having);
        return selectStatement.toString();
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
        sql = removeNumberCondition(selectStatement.toString());
        return sql;
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

    public static String removeGroupBy(String sql, Set<String> fields) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        if (selectStatement == null) {
            return sql;
        }
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        GroupByElement groupByElement = ((PlainSelect) selectBody).getGroupBy();
        if (groupByElement == null) {
            return sql;
        }
        ExpressionList groupByExpressionList = groupByElement.getGroupByExpressionList();
        groupByExpressionList.getExpressions().removeIf(expression -> {
            if (expression instanceof Column) {
                Column column = (Column) expression;
                return fields.contains(column.getColumnName());
            }
            return false;
        });
        if (CollectionUtils.isEmpty(groupByExpressionList.getExpressions())) {
            ((PlainSelect) selectBody).setGroupByElement(null);
        }
        return selectStatement.toString();
    }

    private static Expression filteredWhereExpression(Expression where) {
        if (Objects.isNull(where)) {
            return null;
        }
        if (where instanceof Parenthesis) {
            Expression expression = filteredWhereExpression(((Parenthesis) where).getExpression());
            if (expression != null) {
                try {
                    Expression parseExpression = CCJSqlParserUtil.parseExpression("(" + expression + ")");
                    return parseExpression;
                } catch (JSQLParserException jsqlParserException) {
                    log.info("jsqlParser has an exception:{}", jsqlParserException.toString());
                }
            } else {
                return expression;
            }
        } else if (where instanceof AndExpression) {
            AndExpression andExpression = (AndExpression) where;
            return filteredNumberExpression(andExpression);
        } else if (where instanceof OrExpression) {
            OrExpression orExpression = (OrExpression) where;
            return filteredNumberExpression(orExpression);
        } else {
            return replaceComparisonOperatorFunction(where);
        }
        return where;
    }

    private static <T extends BinaryExpression> Expression filteredNumberExpression(T binaryExpression) {
        Expression leftExpression = filteredWhereExpression(binaryExpression.getLeftExpression());
        Expression rightExpression = filteredWhereExpression(binaryExpression.getRightExpression());
        if (leftExpression != null && rightExpression != null) {
            binaryExpression.setLeftExpression(leftExpression);
            binaryExpression.setRightExpression(rightExpression);
            return binaryExpression;
        } else if (leftExpression != null && rightExpression == null) {
            return leftExpression;
        } else if (leftExpression == null && rightExpression != null) {
            return rightExpression;
        } else {
            return null;
        }
    }

    private static Expression replaceComparisonOperatorFunction(Expression expression) {
        if (Objects.isNull(expression)) {
            return null;
        }
        if (expression instanceof GreaterThanEquals) {
            return removeSingleFilter((GreaterThanEquals) expression);
        } else if (expression instanceof GreaterThan) {
            return removeSingleFilter((GreaterThan) expression);
        } else if (expression instanceof MinorThan) {
            return removeSingleFilter((MinorThan) expression);
        } else if (expression instanceof MinorThanEquals) {
            return removeSingleFilter((MinorThanEquals) expression);
        } else if (expression instanceof EqualsTo) {
            return removeSingleFilter((EqualsTo) expression);
        } else if (expression instanceof NotEqualsTo) {
            return removeSingleFilter((NotEqualsTo) expression);
        } else if (expression instanceof InExpression) {
            InExpression inExpression = (InExpression) expression;
            Expression leftExpression = inExpression.getLeftExpression();
            return distinguishNumberCondition(leftExpression, expression);
        } else if (expression instanceof LikeExpression) {
            LikeExpression likeExpression = (LikeExpression) expression;
            Expression leftExpression = likeExpression.getLeftExpression();
            return distinguishNumberCondition(leftExpression, expression);
        }
        return expression;
    }

    private static <T extends ComparisonOperator> Expression removeSingleFilter(T comparisonExpression) {
        Expression leftExpression = comparisonExpression.getLeftExpression();
        return distinguishNumberCondition(leftExpression, comparisonExpression);
    }

    public static Expression distinguishNumberCondition(Expression leftExpression, Expression expression) {
        if (leftExpression instanceof LongValue) {
            return null;
        } else {
            return expression;
        }
    }

}

