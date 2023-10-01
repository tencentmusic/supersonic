package com.tencent.supersonic.common.util.jsqlparser;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
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
 * Sql Parser Update Helper
 */
@Slf4j
public class SqlParserUpdateHelper {

    public static String replaceValue(String sql, Map<String, Map<String, String>> filedNameToValueMap) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        Expression where = plainSelect.getWhere();
        FieldlValueReplaceVisitor visitor = new FieldlValueReplaceVisitor(filedNameToValueMap);
        if (Objects.nonNull(where)) {
            where.accept(visitor);
        }
        return selectStatement.toString();
    }

    public static String replaceFieldNameByValue(String sql, Map<String, Set<String>> fieldValueToFieldNames) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        Expression where = plainSelect.getWhere();
        FiledNameReplaceVisitor visitor = new FiledNameReplaceVisitor(fieldValueToFieldNames);
        if (Objects.nonNull(where)) {
            where.accept(visitor);
        }
        return selectStatement.toString();
    }

    public static String replaceFields(String sql, Map<String, String> fieldNameMap) {
        return replaceFields(sql, fieldNameMap, false);
    }

    public static String replaceFields(String sql, Map<String, String> fieldNameMap, boolean exactReplace) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        //1. replace where fields
        Expression where = plainSelect.getWhere();
        FieldReplaceVisitor visitor = new FieldReplaceVisitor(fieldNameMap, exactReplace);
        if (Objects.nonNull(where)) {
            where.accept(visitor);
        }

        //2. replace select fields
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            selectItem.accept(visitor);
        }

        //3. replace oder by fields
        List<OrderByElement> orderByElements = plainSelect.getOrderByElements();
        if (!CollectionUtils.isEmpty(orderByElements)) {
            for (OrderByElement orderByElement : orderByElements) {
                orderByElement.accept(new OrderByReplaceVisitor(fieldNameMap, exactReplace));
            }
        }

        //4. replace group by fields
        GroupByElement groupByElement = plainSelect.getGroupBy();
        if (Objects.nonNull(groupByElement)) {
            groupByElement.accept(new GroupByReplaceVisitor(fieldNameMap, exactReplace));
        }
        //5. replace having fields
        Expression having = plainSelect.getHaving();
        if (Objects.nonNull(having)) {
            having.accept(visitor);
        }
        return selectStatement.toString();
    }

    public static String replaceFunction(String sql, Map<String, String> functionMap) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        //1. replace where dataDiff function
        Expression where = plainSelect.getWhere();

        FunctionNameReplaceVisitor visitor = new FunctionNameReplaceVisitor(functionMap);
        if (Objects.nonNull(where)) {
            where.accept(visitor);
        }
        GroupByElement groupBy = plainSelect.getGroupBy();
        if (Objects.nonNull(groupBy)) {
            GroupByFunctionReplaceVisitor replaceVisitor = new GroupByFunctionReplaceVisitor(functionMap);
            groupBy.accept(replaceVisitor);
        }

        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            selectItem.accept(visitor);
        }
        return selectStatement.toString();
    }

    public static String replaceFunction(String sql) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        //1. replace where dataDiff function
        Expression where = plainSelect.getWhere();
        FunctionReplaceVisitor visitor = new FunctionReplaceVisitor();
        if (Objects.nonNull(where)) {
            where.accept(visitor);
        }
        //2. add Waiting Expression
        List<Expression> waitingForAdds = visitor.getWaitingForAdds();
        addWaitingExpression(plainSelect, where, waitingForAdds);
        return selectStatement.toString();
    }

    private static void addWaitingExpression(PlainSelect plainSelect, Expression where,
            List<Expression> waitingForAdds) {
        if (CollectionUtils.isEmpty(waitingForAdds)) {
            return;
        }
        for (Expression expression : waitingForAdds) {
            if (where == null) {
                plainSelect.setWhere(expression);
            } else {
                where = new AndExpression(where, expression);
            }
        }
        plainSelect.setWhere(where);
    }


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

    public static String replaceTable(String sql, String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            return sql;
        }
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        PlainSelect plainSelect = (PlainSelect) selectBody;
        // replace table name
        Table table = (Table) plainSelect.getFromItem();
        table.setName(tableName);
        return selectStatement.toString();
    }


    public static String replaceAlias(String sql) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        FunctionAliasReplaceVisitor visitor = new FunctionAliasReplaceVisitor();
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            selectItem.accept(visitor);
        }
        Map<String, String> aliasToActualExpression = visitor.getAliasToActualExpression();
        if (Objects.nonNull(aliasToActualExpression) && !aliasToActualExpression.isEmpty()) {
            return replaceFields(selectStatement.toString(), aliasToActualExpression, true);
        }
        return selectStatement.toString();
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
        if (SqlParserSelectHelper.hasGroupBy(sql) || CollectionUtils.isEmpty(groupByFields)) {
            return sql;
        }
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }

        PlainSelect plainSelect = (PlainSelect) selectBody;
        GroupByElement groupByElement = new GroupByElement();
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
                Function function = getFunction(expression, fieldNameToAggregate);
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
            Function function = getFunction(expression, fieldNameToAggregate);
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
            Function function = getFunction(expression, fieldNameToAggregate);
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
        if (isLogicExpression(whereExpression)) {
            AndExpression andExpression = (AndExpression) whereExpression;
            Expression leftExpression = andExpression.getLeftExpression();
            Expression rightExpression = andExpression.getRightExpression();
            if (isLogicExpression(leftExpression)) {
                modifyWhereExpression(leftExpression, fieldNameToAggregate);
            } else {
                setAggToFunction(leftExpression, fieldNameToAggregate);
            }
            if (isLogicExpression(rightExpression)) {
                modifyWhereExpression(rightExpression, fieldNameToAggregate);
            } else {
                setAggToFunction(rightExpression, fieldNameToAggregate);
            }
            setAggToFunction(rightExpression, fieldNameToAggregate);
        } else {
            setAggToFunction(whereExpression, fieldNameToAggregate);
        }
    }

    private static boolean isLogicExpression(Expression whereExpression) {
        return whereExpression instanceof AndExpression || (whereExpression instanceof OrExpression
                || (whereExpression instanceof XorExpression));
    }


    private static void setAggToFunction(Expression expression, Map<String, String> fieldNameToAggregate) {
        if (!(expression instanceof ComparisonOperator)) {
            return;
        }
        ComparisonOperator comparisonOperator = (ComparisonOperator) expression;
        if (comparisonOperator.getRightExpression() instanceof Column) {
            String columnName = ((Column) (comparisonOperator).getRightExpression()).getColumnName();
            Function function = getFunction(comparisonOperator.getRightExpression(),
                    fieldNameToAggregate.get(columnName));
            if (Objects.nonNull(function)) {
                comparisonOperator.setRightExpression(function);
            }
        }
        if (comparisonOperator.getLeftExpression() instanceof Column) {
            String columnName = ((Column) (comparisonOperator).getLeftExpression()).getColumnName();
            Function function = getFunction(comparisonOperator.getLeftExpression(),
                    fieldNameToAggregate.get(columnName));
            if (Objects.nonNull(function)) {
                comparisonOperator.setLeftExpression(function);
            }
        }
    }


    private static Function getFunction(Expression expression, Map<String, String> fieldNameToAggregate) {
        if (!(expression instanceof Column)) {
            return null;
        }
        String columnName = ((Column) expression).getColumnName();
        if (StringUtils.isEmpty(columnName)) {
            return null;
        }
        Function function = getFunction(expression, fieldNameToAggregate.get(columnName));
        if (Objects.isNull(function)) {
            return null;
        }
        return function;
    }

    private static Function getFunction(Expression expression, String aggregateName) {
        if (StringUtils.isEmpty(aggregateName)) {
            return null;
        }
        Function sumFunction = new Function();
        sumFunction.setName(aggregateName);
        sumFunction.setParameters(new ExpressionList(expression));
        return sumFunction;
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
}

