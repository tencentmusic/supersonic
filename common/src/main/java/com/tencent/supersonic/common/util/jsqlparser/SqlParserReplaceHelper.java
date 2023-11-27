package com.tencent.supersonic.common.util.jsqlparser;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.SetOperationList;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

/**
 * Sql Parser replace Helper
 */
@Slf4j
public class SqlParserReplaceHelper {

    public static String replaceSelectFields(String sql, Map<String, String> fieldNameMap) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        ((PlainSelect) selectBody).getSelectItems().stream().forEach(o -> {
            SelectExpressionItem selectExpressionItem = (SelectExpressionItem) o;
            String alias = "";
            if (selectExpressionItem.getExpression() instanceof Function) {
                Function function = (Function) selectExpressionItem.getExpression();
                Column column = (Column) function.getParameters().getExpressions().get(0);
                if (fieldNameMap.containsKey(column.getColumnName())) {
                    String value = fieldNameMap.get(column.getColumnName());
                    alias = value;
                    List<Expression> expressions = new ArrayList<>();
                    expressions.add(new Column(value));
                    function.getParameters().setExpressions(expressions);
                }
            }
            if (selectExpressionItem.getExpression() instanceof Column) {
                Column column = (Column) selectExpressionItem.getExpression();
                String columnName = column.getColumnName();
                if (fieldNameMap.containsKey(columnName)) {
                    String value = fieldNameMap.get(columnName);
                    alias = value;
                    if (StringUtils.isNotBlank(value)) {
                        selectExpressionItem.setExpression(new Column(value));
                    }
                }
            }
            if (Objects.nonNull(selectExpressionItem.getAlias()) && StringUtils.isNotBlank(alias)) {
                selectExpressionItem.getAlias().setName(alias);
            }
        });
        return selectStatement.toString();
    }

    public static String replaceValue(String sql, Map<String, Map<String, String>> filedNameToValueMap) {
        return replaceValue(sql, filedNameToValueMap, true);
    }

    public static String replaceValue(String sql, Map<String, Map<String, String>> filedNameToValueMap,
                                      boolean exactReplace) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        List<PlainSelect> plainSelectList = new ArrayList<>();
        plainSelectList.add((PlainSelect) selectBody);
        List<PlainSelect> plainSelects = SqlParserSelectHelper.getPlainSelects(plainSelectList);
        for (PlainSelect plainSelect : plainSelects) {
            Expression where = plainSelect.getWhere();
            FieldlValueReplaceVisitor visitor = new FieldlValueReplaceVisitor(exactReplace, filedNameToValueMap);
            if (Objects.nonNull(where)) {
                where.accept(visitor);
            }
        }
        return selectStatement.toString();
    }

    public static String replaceFieldNameByValue(String sql, Map<String, Set<String>> fieldValueToFieldNames) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        List<PlainSelect> plainSelectList = new ArrayList<>();
        plainSelectList.add((PlainSelect) selectBody);
        List<PlainSelect> plainSelects = SqlParserSelectHelper.getPlainSelects(plainSelectList);
        for (PlainSelect plainSelect : plainSelects) {
            Expression where = plainSelect.getWhere();
            FiledNameReplaceVisitor visitor = new FiledNameReplaceVisitor(fieldValueToFieldNames);
            if (Objects.nonNull(where)) {
                where.accept(visitor);
            }
        }
        return selectStatement.toString();
    }

    public static String replaceFields(String sql, Map<String, String> fieldNameMap) {
        return replaceFields(sql, fieldNameMap, false);
    }

    public static String replaceFields(String sql, Map<String, String> fieldNameMap, boolean exactReplace) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        System.out.println(selectStatement.getSelectBody());
        SelectBody selectBody = selectStatement.getSelectBody();
        List<PlainSelect> plainSelectList = new ArrayList<>();
        if (selectBody instanceof PlainSelect) {
            plainSelectList.add((PlainSelect) selectBody);
        } else if (selectBody instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectBody;
            //replace select
            if (!CollectionUtils.isEmpty(setOperationList.getSelects())) {
                setOperationList.getSelects().forEach(subSelectBody -> {
                    PlainSelect subPlainSelect = (PlainSelect) subSelectBody;
                    plainSelectList.add(subPlainSelect);
                });
            }
            //replace order by
            List<OrderByElement> orderByElements = setOperationList.getOrderByElements();
            if (!CollectionUtils.isEmpty(orderByElements)) {
                for (OrderByElement orderByElement : orderByElements) {
                    orderByElement.accept(new OrderByReplaceVisitor(fieldNameMap, exactReplace));
                }
            }
        } else {
            return sql;
        }
        List<PlainSelect> plainSelects = SqlParserSelectHelper.getPlainSelects(plainSelectList);
        for (PlainSelect plainSelect : plainSelects) {
            replaceFieldsInPlainOneSelect(fieldNameMap, exactReplace, plainSelect);
        }
        return selectStatement.toString();
    }

    private static void replaceFieldsInPlainOneSelect(Map<String, String> fieldNameMap, boolean exactReplace,
                                                      PlainSelect plainSelect) {
        //1. replace where fields
        Expression where = plainSelect.getWhere();
        FieldReplaceVisitor visitor = new FieldReplaceVisitor(fieldNameMap, exactReplace);
        if (Objects.nonNull(where)) {
            where.accept(visitor);
        }

        //2. replace select fields
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            selectItem.accept(visitor);
            replaceAsName(fieldNameMap, selectItem);
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
        List<Join> joins = plainSelect.getJoins();
        if (!CollectionUtils.isEmpty(joins)) {
            for (Join join : joins) {
                join.getOnExpression().accept(visitor);
                SelectBody subSelectBody = ((SubSelect) join.getRightItem()).getSelectBody();
                List<PlainSelect> plainSelectList = new ArrayList<>();
                plainSelectList.add((PlainSelect) subSelectBody);
                List<PlainSelect> subPlainSelects = SqlParserSelectHelper.getPlainSelects(plainSelectList);
                for (PlainSelect subPlainSelect : subPlainSelects) {
                    replaceFieldsInPlainOneSelect(fieldNameMap, exactReplace, subPlainSelect);
                }
            }
        }
    }

    private static void replaceAsName(Map<String, String> fieldNameMap, SelectItem selectItem) {
        if (selectItem instanceof SelectExpressionItem) {
            SelectExpressionItem expressionItem = (SelectExpressionItem) selectItem;
            Alias alias = expressionItem.getAlias();
            if (Objects.isNull(alias)) {
                return;
            }
            String aliasName = alias.getName();
            String replaceFieldName = fieldNameMap.get(aliasName);
            if (StringUtils.isNotBlank(replaceFieldName)) {
                alias.setName(replaceFieldName);
            }
        }
    }

    public static String replaceFunction(String sql, Map<String, String> functionMap) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        List<PlainSelect> plainSelectList = new ArrayList<>();
        plainSelectList.add((PlainSelect) selectBody);
        List<PlainSelect> plainSelects = SqlParserSelectHelper.getPlainSelects(plainSelectList);
        for (PlainSelect plainSelect : plainSelects) {
            replaceFunction(functionMap, plainSelect);
        }
        return selectStatement.toString();
    }

    private static void replaceFunction(Map<String, String> functionMap, PlainSelect selectBody) {
        PlainSelect plainSelect = selectBody;
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
        Expression having = plainSelect.getHaving();
        if (Objects.nonNull(having)) {
            replaceHavingFunction(functionMap, having);
        }
        List<OrderByElement> orderByElementList = plainSelect.getOrderByElements();
        replaceOrderByFunction(functionMap, orderByElementList);
    }

    public static String replaceFunction(String sql) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        List<PlainSelect> plainSelectList = new ArrayList<>();
        plainSelectList.add((PlainSelect) selectBody);
        List<PlainSelect> plainSelects = SqlParserSelectHelper.getPlainSelects(plainSelectList);
        for (PlainSelect plainSelect : plainSelects) {
            replaceFunction(plainSelect);
        }
        return selectStatement.toString();
    }

    private static void replaceFunction(PlainSelect selectBody) {
        PlainSelect plainSelect = selectBody;

        //1. replace where dataDiff function
        Expression where = plainSelect.getWhere();
        FunctionReplaceVisitor visitor = new FunctionReplaceVisitor();
        if (Objects.nonNull(where)) {
            where.accept(visitor);
        }
        //2. add Waiting Expression
        List<Expression> waitingForAdds = visitor.getWaitingForAdds();
        addWaitingExpression(plainSelect, where, waitingForAdds);
    }

    private static void replaceHavingFunction(Map<String, String> functionMap, Expression having) {
        if (Objects.nonNull(having)) {
            if (having instanceof AndExpression) {
                AndExpression andExpression = (AndExpression) having;
                replaceHavingFunction(functionMap, andExpression.getLeftExpression());
                replaceHavingFunction(functionMap, andExpression.getRightExpression());
            } else if (having instanceof OrExpression) {
                OrExpression orExpression = (OrExpression) having;
                replaceHavingFunction(functionMap, orExpression.getLeftExpression());
                replaceHavingFunction(functionMap, orExpression.getRightExpression());
            } else {
                replaceComparisonOperatorFunction(functionMap, having);
            }
        }
    }

    private static void replaceComparisonOperatorFunction(Map<String, String> functionMap, Expression expression) {
        if (Objects.isNull(expression)) {
            return;
        }
        if (expression instanceof GreaterThanEquals) {
            replaceFilterFunction(functionMap, (GreaterThanEquals) expression);
        } else if (expression instanceof GreaterThan) {
            replaceFilterFunction(functionMap, (GreaterThan) expression);
        } else if (expression instanceof MinorThan) {
            replaceFilterFunction(functionMap, (MinorThan) expression);
        } else if (expression instanceof MinorThanEquals) {
            replaceFilterFunction(functionMap, (MinorThanEquals) expression);
        } else if (expression instanceof EqualsTo) {
            replaceFilterFunction(functionMap, (EqualsTo) expression);
        } else if (expression instanceof NotEqualsTo) {
            replaceFilterFunction(functionMap, (NotEqualsTo) expression);
        }
    }

    private static void replaceOrderByFunction(Map<String, String> functionMap,
                                               List<OrderByElement> orderByElementList) {
        if (Objects.isNull(orderByElementList)) {
            return;
        }
        for (OrderByElement orderByElement : orderByElementList) {
            if (orderByElement.getExpression() instanceof Function) {
                Function function = (Function) orderByElement.getExpression();
                if (functionMap.containsKey(function.getName())) {
                    function.setName(functionMap.get(function.getName()));
                }
            }
        }
    }

    private static <T extends ComparisonOperator> void replaceFilterFunction(
            Map<String, String> functionMap, T comparisonExpression) {
        Expression expression = comparisonExpression.getLeftExpression();
        if (expression instanceof Function) {
            Function function = (Function) expression;
            if (functionMap.containsKey(function.getName())) {
                function.setName(functionMap.get(function.getName()));
            }
        }
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

    public static String replaceTable(String sql, String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            return sql;
        }
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;
            replaceSingleTable(plainSelect, tableName);
        } else if (selectBody instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectBody;
            if (!CollectionUtils.isEmpty(setOperationList.getSelects())) {
                setOperationList.getSelects().forEach(subSelectBody -> {
                    PlainSelect subPlainSelect = (PlainSelect) subSelectBody;
                    replaceSingleTable(subPlainSelect, tableName);
                });
            }
        }

        return selectStatement.toString();
    }

    public static void replaceSingleTable(PlainSelect plainSelect, String tableName) {
        // replace table name
        List<PlainSelect> plainSelects = new ArrayList<>();
        plainSelects.add(plainSelect);
        List<PlainSelect> painSelects = SqlParserSelectHelper.getPlainSelects(plainSelects);
        for (PlainSelect painSelect : painSelects) {
            painSelect.accept(
                    new SelectVisitorAdapter() {
                        @Override
                        public void visit(PlainSelect plainSelect) {
                            plainSelect.getFromItem().accept(new TableNameReplaceVisitor(tableName));
                        }
                    });
            List<Join> joins = painSelect.getJoins();
            if (!CollectionUtils.isEmpty(joins)) {
                for (Join join : joins) {
                    SelectBody subSelectBody = ((SubSelect) join.getRightItem()).getSelectBody();
                    List<PlainSelect> plainSelectList = new ArrayList<>();
                    plainSelectList.add((PlainSelect) subSelectBody);
                    List<PlainSelect> subPlainSelects = SqlParserSelectHelper.getPlainSelects(plainSelectList);
                    for (PlainSelect subPlainSelect : subPlainSelects) {
                        subPlainSelect.getFromItem().accept(new TableNameReplaceVisitor(tableName));
                    }
                }
            }
        }
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

    public static String replaceHavingValue(String sql, Map<String, Map<String, String>> filedNameToValueMap) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        Expression having = plainSelect.getHaving();
        FieldlValueReplaceVisitor visitor = new FieldlValueReplaceVisitor(false, filedNameToValueMap);
        if (Objects.nonNull(having)) {
            having.accept(visitor);
        }
        return selectStatement.toString();
    }
}

