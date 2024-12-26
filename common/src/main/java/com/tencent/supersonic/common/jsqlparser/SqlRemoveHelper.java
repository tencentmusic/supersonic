package com.tencent.supersonic.common.jsqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sql Parser remove Helper
 */
@Slf4j
public class SqlRemoveHelper {
    private static Pattern pattern =
            Pattern.compile("([\\s,\\t\\n]|\\b)_([^\\s,\\t\\n]+)_([\\s,\\t\\n]|\\b)");

    public static String removeUnderscores(String sql) {
        try {
            Matcher matcher = pattern.matcher(sql);

            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(result,
                        matcher.group(1) + matcher.group(2) + matcher.group(3));
            }
            matcher.appendTail(result);

            return result.toString();
        } catch (Exception e) {
            log.error("removeUnderscores error", e);
        }
        return sql;
    }

    public static String removeAsteriskAndAddFields(String sql, Set<String> needAddDefaultFields) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (isInvalidSelect(selectStatement)) {
            return sql;
        }
        List<SelectItem<?>> selectItems = ((PlainSelect) selectStatement).getSelectItems();
        if (selectItems.stream().anyMatch(item -> item.getExpression() instanceof AllColumns)) {
            selectItems.clear();
            List<SelectItem<Column>> columnSelectItems = new ArrayList<>();
            for (String fieldName : needAddDefaultFields) {
                SelectItem<Column> selectExpressionItem = new SelectItem(new Column(fieldName));
                columnSelectItems.add(selectExpressionItem);
            }
            selectItems.addAll(columnSelectItems);
        }
        return selectStatement.toString();
    }

    public static String removeSameFieldFromSelect(String sql) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (isInvalidSelect(selectStatement)) {
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

    private static void removeWhereCondition(Expression whereExpression,
            Set<String> removeFieldNames) {
        if (whereExpression == null) {
            return;
        }
        removeWhereExpression(whereExpression, removeFieldNames);
    }

    public static String removeNumberFilter(String sql) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (isInvalidSelect(selectStatement)) {
            return sql;
        }
        Expression where = ((PlainSelect) selectStatement).getWhere();
        Expression having = ((PlainSelect) selectStatement).getHaving();
        try {
            ((PlainSelect) selectStatement)
                    .setWhere(filteredExpression(where, SqlEditEnum.NUMBER_FILTER));
            ((PlainSelect) selectStatement)
                    .setHaving(filteredExpression(having, SqlEditEnum.NUMBER_FILTER));
        } catch (Exception e) {
            log.info("replaceFunction has an exception:{}", e.toString());
        }
        return selectStatement.toString();
    }

    private static void removeWhereExpression(Expression whereExpression,
            Set<String> removeFieldNames) {
        if (SqlSelectHelper.isLogicExpression(whereExpression)) {
            BinaryExpression binaryExpression = (BinaryExpression) whereExpression;
            Expression leftExpression = binaryExpression.getLeftExpression();
            Expression rightExpression = binaryExpression.getRightExpression();

            removeWhereExpression(leftExpression, removeFieldNames);
            removeWhereExpression(rightExpression, removeFieldNames);
        } else if (whereExpression instanceof Parenthesis) {
            removeWhereExpression(((Parenthesis) whereExpression).getExpression(),
                    removeFieldNames);
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

    private static void removeExpressionWithConstant(Expression expression,
            Set<String> removeFieldNames) {
        try {
            if (expression instanceof ComparisonOperator) {
                handleComparisonOperator((ComparisonOperator) expression, removeFieldNames);
            } else if (expression instanceof InExpression) {
                handleInExpression((InExpression) expression, removeFieldNames);
            } else if (expression instanceof LikeExpression) {
                handleLikeExpression((LikeExpression) expression, removeFieldNames);
            } else if (expression instanceof Between) {
                handleBetweenExpression((Between) expression, removeFieldNames);
            }
        } catch (JSQLParserException e) {
            log.error("JSQLParserException", e);
        }
    }

    private static void handleComparisonOperator(ComparisonOperator comparisonOperator,
            Set<String> removeFieldNames) throws JSQLParserException {
        String columnName = SqlSelectHelper.getColumnName(comparisonOperator.getLeftExpression(),
                comparisonOperator.getRightExpression());
        if (!removeFieldNames.contains(columnName)) {
            return;
        }
        String constant = getConstant(comparisonOperator);
        ComparisonOperator constantExpression =
                (ComparisonOperator) CCJSqlParserUtil.parseCondExpression(constant);
        updateComparisonOperator(comparisonOperator, constantExpression);
    }

    private static void handleInExpression(InExpression inExpression, Set<String> removeFieldNames)
            throws JSQLParserException {
        String columnName = SqlSelectHelper.getColumnName(inExpression.getLeftExpression(),
                inExpression.getRightExpression());
        if (!removeFieldNames.contains(columnName)) {
            return;
        }
        InExpression constantExpression =
                (InExpression) CCJSqlParserUtil.parseCondExpression(JsqlConstants.IN_CONSTANT);
        updateInExpression(inExpression, constantExpression);
    }

    private static void handleLikeExpression(LikeExpression likeExpression,
            Set<String> removeFieldNames) throws JSQLParserException {
        String columnName = SqlSelectHelper.getColumnName(likeExpression.getLeftExpression(),
                likeExpression.getRightExpression());
        if (!removeFieldNames.contains(columnName)) {
            return;
        }
        LikeExpression constantExpression =
                (LikeExpression) CCJSqlParserUtil.parseCondExpression(JsqlConstants.LIKE_CONSTANT);
        updateLikeExpression(likeExpression, constantExpression);
    }

    private static void handleBetweenExpression(Between between, Set<String> removeFieldNames)
            throws JSQLParserException {
        String columnName = SqlSelectHelper.getColumnName(between.getLeftExpression());
        if (!removeFieldNames.contains(columnName)) {
            return;
        }
        Between constantExpression =
                (Between) CCJSqlParserUtil.parseCondExpression(JsqlConstants.BETWEEN_AND_CONSTANT);
        updateBetweenExpression(between, constantExpression);
    }

    private static void updateComparisonOperator(ComparisonOperator original,
            ComparisonOperator constantExpression) {
        original.setLeftExpression(constantExpression.getLeftExpression());
        original.setRightExpression(constantExpression.getRightExpression());
        original.setASTNode(constantExpression.getASTNode());
    }

    private static void updateInExpression(InExpression original, InExpression constantExpression) {
        original.setLeftExpression(constantExpression.getLeftExpression());
        original.setRightExpression(constantExpression.getRightExpression());
        original.setASTNode(constantExpression.getASTNode());
    }

    private static void updateLikeExpression(LikeExpression original,
            LikeExpression constantExpression) {
        original.setLeftExpression(constantExpression.getLeftExpression());
        original.setRightExpression(constantExpression.getRightExpression());
    }

    private static void updateBetweenExpression(Between between, Between constantExpression) {
        between.setBetweenExpressionEnd(constantExpression.getBetweenExpressionEnd());
        between.setBetweenExpressionStart(constantExpression.getBetweenExpressionStart());
        between.setLeftExpression(constantExpression.getLeftExpression());
    }

    public static String removeHavingCondition(String sql, Set<String> removeFieldNames) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
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
        if (isInvalidSelect(selectStatement)) {
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

    public static String removeSelect(String sql, Set<String> fields) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (isInvalidSelect(selectStatement)) {
            return sql;
        }
        List<SelectItem<?>> selectItems = ((PlainSelect) selectStatement).getSelectItems();
        Iterator<SelectItem<?>> iterator = selectItems.iterator();
        while (iterator.hasNext()) {
            SelectItem selectItem = iterator.next();
            selectItem.accept(new SelectItemVisitorAdapter() {
                @Override
                public void visit(SelectItem item) {
                    if (fields.contains(item.getExpression().toString())) {
                        iterator.remove();
                    }
                }
            });
        }
        if (selectItems.isEmpty()) {
            selectItems.add(new SelectItem(new AllColumns()));
        }
        return selectStatement.toString();
    }

    public static Expression filteredExpression(Expression where, SqlEditEnum sqlEditEnum)
            throws Exception {
        if (Objects.isNull(where)) {
            return null;
        }
        if (where instanceof Parenthesis) {
            Expression expression =
                    filteredExpression(((Parenthesis) where).getExpression(), sqlEditEnum);
            if (expression != null) {
                try {
                    Expression parseExpression =
                            CCJSqlParserUtil.parseExpression("(" + expression + ")");
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
        Expression leftExpression =
                filteredExpression(binaryExpression.getLeftExpression(), sqlEditEnum);
        Expression rightExpression =
                filteredExpression(binaryExpression.getRightExpression(), sqlEditEnum);
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

    private static Expression dealComparisonOperatorFilter(Expression expression,
            SqlEditEnum sqlEditEnum) {
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
        } else if (expression instanceof Between) {
            Between between = (Between) expression;
            Expression leftExpression = between.getLeftExpression();
            return recursionBase(leftExpression, expression, sqlEditEnum);
        }
        return expression;
    }

    private static Expression removeSingleFilter(ComparisonOperator comparisonExpression,
            SqlEditEnum sqlEditEnum) {
        Expression leftExpression = comparisonExpression.getLeftExpression();
        return recursionBase(leftExpression, comparisonExpression, sqlEditEnum);
    }

    private static Expression recursionBase(Expression leftExpression, Expression expression,
            SqlEditEnum sqlEditEnum) {
        if (sqlEditEnum.equals(SqlEditEnum.NUMBER_FILTER)) {
            return distinguishNumberFilter(leftExpression, expression);
        }
        if (sqlEditEnum.equals(SqlEditEnum.DATEDIFF)) {
            return SqlReplaceHelper.distinguishDateDiffFilter(leftExpression, expression);
        }
        return expression;
    }

    private static Expression distinguishNumberFilter(Expression leftExpression,
            Expression expression) {
        if (leftExpression instanceof LongValue) {
            return null;
        } else {
            return expression;
        }
    }

    public static String removeIsNullInWhere(String sql, Set<String> removeFieldNames) {
        return removeIsNullOrNotNullInWhere(true, false, sql, removeFieldNames);
    }

    public static String removeNotNullInWhere(String sql, Set<String> removeFieldNames) {
        return removeIsNullOrNotNullInWhere(false, true, sql, removeFieldNames);
    }

    public static String removeIsNullOrNotNullInWhere(boolean dealNull, boolean dealNotNull,
            String sql, Set<String> removeFieldNames) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        // Create a custom ExpressionDeParser to remove specific IS NULL and IS NOT NULL conditions
        ExpressionDeParser expressionDeParser =
                new CustomExpressionDeParser(removeFieldNames, dealNull, dealNotNull);

        StringBuilder buffer = new StringBuilder();
        SelectDeParser selectDeParser = new SelectDeParser(expressionDeParser, buffer);
        expressionDeParser.setSelectVisitor(selectDeParser);
        expressionDeParser.setBuffer(buffer);
        PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
        if (plainSelect.getWhere() != null) {
            plainSelect.getWhere().accept(expressionDeParser);
        }
        // Parse the modified WHERE clause back to an Expression
        try {
            Expression newWhere = CCJSqlParserUtil.parseCondExpression(buffer.toString());
            plainSelect.setWhere(newWhere);
        } catch (Exception e) {
            log.error("parseCondExpression error:{}", buffer, e);
        }
        return selectStatement.toString();
    }

    private static boolean isInvalidSelect(Select selectStatement) {
        return Objects.isNull(selectStatement) || !(selectStatement instanceof PlainSelect);
    }
}
