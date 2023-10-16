package com.tencent.supersonic.common.util.jsqlparser;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.util.SelectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

/**
 * Sql Parser add Helper
 */
@Slf4j
public class SqlParserAddHelper {

    public static String addFieldsToSelect(String sql, List<String> fields) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        // add fields to select
        for (String field : fields) {
            SelectUtils.addExpression(selectStatement, new Column(field));
        }
        return selectStatement.toString();
    }

    public static String addFunctionToSelect(String sql, Expression expression) {
        PlainSelect plainSelect = SqlParserSelectHelper.getPlainSelect(sql);
        if (Objects.isNull(plainSelect)) {
            return sql;
        }
        List<SelectItem> selectItems = plainSelect.getSelectItems();
        if (CollectionUtils.isEmpty(selectItems)) {
            return sql;
        }
        boolean existFunction = false;
        for (SelectItem selectItem : selectItems) {
            SelectExpressionItem expressionItem = (SelectExpressionItem) selectItem;
            if (expressionItem.getExpression() instanceof Function) {
                Function expressionFunction = (Function) expressionItem.getExpression();
                if (expression.toString().equalsIgnoreCase(expressionFunction.toString())) {
                    existFunction = true;
                    break;
                }
            }
        }
        if (!existFunction) {
            SelectExpressionItem sumExpressionItem = new SelectExpressionItem(expression);
            selectItems.add(sumExpressionItem);
        }
        return plainSelect.toString();
    }

    public static String addWhere(String sql, String column, Object value) {
        if (StringUtils.isEmpty(column) || Objects.isNull(value)) {
            return sql;
        }
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        Expression where = plainSelect.getWhere();

        Expression right = new StringValue(value.toString());
        if (value instanceof Integer || value instanceof Long) {
            right = new LongValue(value.toString());
        }

        if (where == null) {
            plainSelect.setWhere(new EqualsTo(new Column(column), right));
        } else {
            plainSelect.setWhere(new AndExpression(where, new EqualsTo(new Column(column), right)));
        }
        return selectStatement.toString();
    }


    public static String addWhere(String sql, Expression expression) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        Expression where = plainSelect.getWhere();

        if (where == null) {
            plainSelect.setWhere(expression);
        } else {
            plainSelect.setWhere(new AndExpression(where, expression));
        }
        return selectStatement.toString();
    }

    public static String addWhere(String sql, List<Expression> expressionList) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        if (CollectionUtils.isEmpty(expressionList)) {
            return sql;
        }
        Expression expression = expressionList.get(0);
        for (int i = 1; i < expressionList.size(); i++) {
            expression = new AndExpression(expression, expressionList.get(i));
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        Expression where = plainSelect.getWhere();

        if (where == null) {
            plainSelect.setWhere(expression);
        } else {
            plainSelect.setWhere(new AndExpression(where, expression));
        }
        return selectStatement.toString();
    }

    public static String addAggregateToField(String sql, Map<String, String> fieldNameToAggregate) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        selectBody.accept(new SelectVisitorAdapter() {
            @Override
            public void visit(PlainSelect plainSelect) {
                addAggregateToSelectItems(plainSelect.getSelectItems(), fieldNameToAggregate);
                addAggregateToOrderByItems(plainSelect.getOrderByElements(), fieldNameToAggregate);
                addAggregateToGroupByItems(plainSelect.getGroupBy(), fieldNameToAggregate);
                addAggregateToWhereItems(plainSelect.getWhere(), fieldNameToAggregate);
            }
        });
        return selectStatement.toString();
    }

    public static String addGroupBy(String sql, Set<String> groupByFields) {
        if (CollectionUtils.isEmpty(groupByFields)) {
            return sql;
        }
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }

        PlainSelect plainSelect = (PlainSelect) selectBody;
        GroupByElement groupByElement = new GroupByElement();
        List<String> originalGroupByFields = SqlParserSelectHelper.getGroupByFields(sql);
        if (!CollectionUtils.isEmpty(originalGroupByFields)) {
            groupByFields.addAll(originalGroupByFields);
        }
        for (String groupByField : groupByFields) {
            groupByElement.addGroupByExpression(new Column(groupByField));
        }
        plainSelect.setGroupByElement(groupByElement);
        return selectStatement.toString();
    }

    private static void addAggregateToSelectItems(List<SelectItem> selectItems,
            Map<String, String> fieldNameToAggregate) {
        for (SelectItem selectItem : selectItems) {
            if (selectItem instanceof SelectExpressionItem) {
                SelectExpressionItem selectExpressionItem = (SelectExpressionItem) selectItem;
                Expression expression = selectExpressionItem.getExpression();
                Function function = SqlParserSelectFunctionHelper.getFunction(expression, fieldNameToAggregate);
                if (function == null) {
                    continue;
                }
                selectExpressionItem.setExpression(function);
            }
        }
    }

    private static void addAggregateToOrderByItems(List<OrderByElement> orderByElements,
            Map<String, String> fieldNameToAggregate) {
        if (orderByElements == null) {
            return;
        }
        for (OrderByElement orderByElement : orderByElements) {
            Expression expression = orderByElement.getExpression();
            Function function = SqlParserSelectFunctionHelper.getFunction(expression, fieldNameToAggregate);
            if (function == null) {
                continue;
            }
            orderByElement.setExpression(function);
        }
    }

    private static void addAggregateToGroupByItems(GroupByElement groupByElement,
            Map<String, String> fieldNameToAggregate) {
        if (groupByElement == null) {
            return;
        }
        for (Expression expression : groupByElement.getGroupByExpressions()) {
            Function function = SqlParserSelectFunctionHelper.getFunction(expression, fieldNameToAggregate);
            if (function == null) {
                continue;
            }
            groupByElement.addGroupByExpression(function);
        }
    }

    private static void addAggregateToWhereItems(Expression whereExpression, Map<String, String> fieldNameToAggregate) {
        if (whereExpression == null) {
            return;
        }
        modifyWhereExpression(whereExpression, fieldNameToAggregate);
    }

    private static void modifyWhereExpression(Expression whereExpression,
            Map<String, String> fieldNameToAggregate) {
        if (SqlParserSelectHelper.isLogicExpression(whereExpression)) {
            AndExpression andExpression = (AndExpression) whereExpression;
            Expression leftExpression = andExpression.getLeftExpression();
            Expression rightExpression = andExpression.getRightExpression();
            modifyWhereExpression(leftExpression, fieldNameToAggregate);
            modifyWhereExpression(rightExpression, fieldNameToAggregate);
        } else if (whereExpression instanceof Parenthesis) {
            modifyWhereExpression(((Parenthesis) whereExpression).getExpression(), fieldNameToAggregate);
        } else {
            setAggToFunction(whereExpression, fieldNameToAggregate);
        }
    }

    private static void setAggToFunction(Expression expression, Map<String, String> fieldNameToAggregate) {
        if (!(expression instanceof ComparisonOperator)) {
            return;
        }
        ComparisonOperator comparisonOperator = (ComparisonOperator) expression;
        if (comparisonOperator.getRightExpression() instanceof Column) {
            String columnName = ((Column) (comparisonOperator).getRightExpression()).getColumnName();
            Function function = SqlParserSelectFunctionHelper.getFunction(comparisonOperator.getRightExpression(),
                    fieldNameToAggregate.get(columnName));
            if (Objects.nonNull(function)) {
                comparisonOperator.setRightExpression(function);
            }
        }
        if (comparisonOperator.getLeftExpression() instanceof Column) {
            String columnName = ((Column) (comparisonOperator).getLeftExpression()).getColumnName();
            Function function = SqlParserSelectFunctionHelper.getFunction(comparisonOperator.getLeftExpression(),
                    fieldNameToAggregate.get(columnName));
            if (Objects.nonNull(function)) {
                comparisonOperator.setLeftExpression(function);
            }
        }
    }

    public static String addHaving(String sql, Set<String> fieldNames) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }

        PlainSelect plainSelect = (PlainSelect) selectBody;
        //replace metric to 1 and 1 and add having metric
        Expression where = plainSelect.getWhere();
        FiledFilterReplaceVisitor visitor = new FiledFilterReplaceVisitor(fieldNames);
        if (Objects.nonNull(where)) {
            where.accept(visitor);
        }
        List<Expression> waitingForAdds = visitor.getWaitingForAdds();
        if (!CollectionUtils.isEmpty(waitingForAdds)) {
            for (Expression waitingForAdd : waitingForAdds) {
                Expression having = plainSelect.getHaving();
                if (Objects.isNull(having)) {
                    plainSelect.setHaving(waitingForAdd);
                } else {
                    plainSelect.setHaving(new AndExpression(having, waitingForAdd));
                }
            }
        }
        return selectStatement.toString();
    }

    public static String addHaving(String sql, List<Expression> expressionList) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        if (CollectionUtils.isEmpty(expressionList)) {
            return sql;
        }
        Expression expression = expressionList.get(0);
        for (int i = 1; i < expressionList.size(); i++) {
            expression = new AndExpression(expression, expressionList.get(i));
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        Expression having = plainSelect.getHaving();

        if (having == null) {
            plainSelect.setHaving(expression);
        } else {
            plainSelect.setHaving(new AndExpression(having, expression));
        }
        return selectStatement.toString();
    }

    public static String addParenthesisToWhere(String sql) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        Expression where = plainSelect.getWhere();
        if (Objects.nonNull(where)) {
            Parenthesis parenthesis = new Parenthesis(where);
            plainSelect.setWhere(parenthesis);
        }
        return selectStatement.toString();
    }
}

