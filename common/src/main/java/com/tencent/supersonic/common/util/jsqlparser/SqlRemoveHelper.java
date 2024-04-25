package com.tencent.supersonic.common.util.jsqlparser;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import org.springframework.util.CollectionUtils;

/**
 * Sql Parser remove Helper
 */
@Slf4j
public class SqlRemoveHelper {

    public static String removeSelect(String sql, Set<String> fields) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (selectStatement == null) {
            return sql;
        }
        //SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        List<SelectItem<?>> selectItems = ((PlainSelect) selectStatement).getSelectItems();
        selectItems.removeIf(selectItem -> {
            String columnName = SqlSelectHelper.getColumnName(selectItem.getExpression());
            return fields.contains(columnName);
        });
        return selectStatement.toString();
    }

    public static String removeSameFieldFromSelect(String sql) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (selectStatement == null) {
            return sql;
        }
        //SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        List<SelectItem<?>> selectItems = ((PlainSelect) selectStatement).getSelectItems();
        Set<String> fields = new HashSet<>();
        selectItems.removeIf(selectItem -> {
            String field = selectItem.getExpression().toString();
            if (fields.contains(field)) {
                return true;
            }
            fields.add(field);
            return false;
        });
        ((PlainSelect) selectStatement).setSelectItems(selectItems);
        return selectStatement.toString();
    }

    public static String removeWhereCondition(String sql, Set<String> removeFieldNames) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        //SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        selectStatement.accept(new SelectVisitorAdapter() {
            @Override
            public void visit(PlainSelect plainSelect) {
                removeWhereCondition(plainSelect.getWhere(), removeFieldNames);
            }
        });
        return removeNumberFilter(selectStatement.toString());
    }

    private static void removeWhereCondition(Expression whereExpression, Set<String> removeFieldNames) {
        if (whereExpression == null) {
            return;
        }
        removeWhereExpression(whereExpression, removeFieldNames);
    }

    public static String removeNumberFilter(String sql) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (selectStatement == null) {
            return sql;
        }
        //SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        Expression where = ((PlainSelect) selectStatement).getWhere();
        Expression having = ((PlainSelect) selectStatement).getHaving();
        try {
            ((PlainSelect) selectStatement).setWhere(filteredExpression(where, SqlEditEnum.NUMBER_FILTER));
            ((PlainSelect) selectStatement).setHaving(filteredExpression(having, SqlEditEnum.NUMBER_FILTER));
        } catch (Exception e) {
            log.info("replaceFunction has an exception:{}", e.toString());
        }
        return selectStatement.toString();
    }

    private static void removeWhereExpression(Expression whereExpression, Set<String> removeFieldNames) {
        if (SqlSelectHelper.isLogicExpression(whereExpression)) {
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
            String columnName = SqlSelectHelper.getColumnName(comparisonOperator.getLeftExpression(),
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
            String columnName = SqlSelectHelper.getColumnName(inExpression.getLeftExpression(),
                    inExpression.getRightExpression());
            if (!removeFieldNames.contains(columnName)) {
                return;
            }
            try {
                InExpression constantExpression = (InExpression) CCJSqlParserUtil.parseCondExpression(
                        JsqlConstants.IN_CONSTANT);
                inExpression.setLeftExpression(constantExpression.getLeftExpression());
                //inExpression.setRightItemsList(constantExpression.getRightItemsList());
                inExpression.setRightExpression(constantExpression.getRightExpression());
                inExpression.setASTNode(constantExpression.getASTNode());
            } catch (JSQLParserException e) {
                log.error("JSQLParserException", e);
            }
        }
        if (expression instanceof LikeExpression) {
            LikeExpression likeExpression = (LikeExpression) expression;
            String columnName = SqlSelectHelper.getColumnName(likeExpression.getLeftExpression(),
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
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        //SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        selectStatement.accept(new SelectVisitorAdapter() {
            @Override
            public void visit(PlainSelect plainSelect) {
                removeWhereCondition(plainSelect.getHaving(), removeFieldNames);
            }
        });
        return removeNumberFilter(selectStatement.toString());
    }

    public static String removeGroupBy(String sql, Set<String> fields) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (selectStatement == null) {
            return sql;
        }
        //SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        GroupByElement groupByElement = ((PlainSelect) selectStatement).getGroupBy();
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
            ((PlainSelect) selectStatement).setGroupByElement(null);
        }
        return selectStatement.toString();
    }

    public static Expression filteredExpression(Expression where, SqlEditEnum sqlEditEnum) throws Exception {
        if (Objects.isNull(where)) {
            return null;
        }
        if (where instanceof Parenthesis) {
            Expression expression = filteredExpression(((Parenthesis) where).getExpression(), sqlEditEnum);
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
            return filteredLogicExpression(andExpression, sqlEditEnum);
        } else if (where instanceof OrExpression) {
            OrExpression orExpression = (OrExpression) where;
            return filteredLogicExpression(orExpression, sqlEditEnum);
        } else {
            return dealComparisonOperatorFilter(where, sqlEditEnum);
        }
        return where;
    }

    private static <T extends BinaryExpression> Expression filteredLogicExpression(
            T binaryExpression, SqlEditEnum sqlEditEnum) throws Exception {
        Expression leftExpression = filteredExpression(binaryExpression.getLeftExpression(), sqlEditEnum);
        Expression rightExpression = filteredExpression(binaryExpression.getRightExpression(), sqlEditEnum);
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

    private static Expression dealComparisonOperatorFilter(Expression expression, SqlEditEnum sqlEditEnum) {
        if (Objects.isNull(expression)) {
            return null;
        }
        if (expression instanceof GreaterThanEquals || expression instanceof GreaterThan
                || expression instanceof MinorThan || expression instanceof MinorThanEquals
                || expression instanceof EqualsTo || expression instanceof NotEqualsTo) {
            return removeSingleFilter((ComparisonOperator) expression, sqlEditEnum);
        } else if (expression instanceof InExpression) {
            InExpression inExpression = (InExpression) expression;
            Expression leftExpression = inExpression.getLeftExpression();
            return recursionBase(leftExpression, expression, sqlEditEnum);
        } else if (expression instanceof LikeExpression) {
            LikeExpression likeExpression = (LikeExpression) expression;
            Expression leftExpression = likeExpression.getLeftExpression();
            return recursionBase(leftExpression, expression, sqlEditEnum);
        }
        return expression;
    }

    private static Expression removeSingleFilter(
            ComparisonOperator comparisonExpression, SqlEditEnum sqlEditEnum) {
        Expression leftExpression = comparisonExpression.getLeftExpression();
        return recursionBase(leftExpression, comparisonExpression, sqlEditEnum);
    }

    private static Expression recursionBase(Expression leftExpression, Expression expression, SqlEditEnum sqlEditEnum) {
        if (sqlEditEnum.equals(SqlEditEnum.NUMBER_FILTER)) {
            return distinguishNumberFilter(leftExpression, expression);
        }
        if (sqlEditEnum.equals(SqlEditEnum.DATEDIFF)) {
            return SqlReplaceHelper.distinguishDateDiffFilter(leftExpression, expression);
        }
        return expression;
    }

    private static Expression distinguishNumberFilter(Expression leftExpression, Expression expression) {
        if (leftExpression instanceof LongValue) {
            return null;
        } else {
            return expression;
        }
    }

}

