package com.tencent.supersonic.common.jsqlparser;

import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.ParenthesedFromItem;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.FromItem;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;

/**
 * Sql Parser replace Helper
 */
@Slf4j
public class SqlReplaceHelper {

    public static String replaceSelectFields(String sql, Map<String, String> fieldNameMap) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        ((PlainSelect) selectStatement).getSelectItems().stream().forEach(o -> {
            SelectItem selectExpressionItem = (SelectItem) o;
            String alias = "";
            if (selectExpressionItem.getExpression() instanceof Function) {
                Function function = (Function) selectExpressionItem.getExpression();
                Column column = (Column) function.getParameters().getExpressions().get(0);
                if (fieldNameMap.containsKey(column.getColumnName())) {
                    String value = fieldNameMap.get(column.getColumnName());
                    alias = value;
                    function.withParameters(new Column(value));
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

    public static String replaceAggFields(String sql, Map<String, Pair<String, String>> fieldNameToAggMap) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);

        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        ((PlainSelect) selectStatement).getSelectItems().stream().forEach(o -> {
            SelectItem selectExpressionItem = (SelectItem) o;
            if (selectExpressionItem.getExpression() instanceof Function) {
                Function function = (Function) selectExpressionItem.getExpression();
                Column column = (Column) function.getParameters().getExpressions().get(0);
                if (fieldNameToAggMap.containsKey(column.getColumnName())) {
                    Pair<String, String> agg = fieldNameToAggMap.get(column.getColumnName());
                    String field = agg.getKey();
                    String func = agg.getRight();
                    if (AggOperatorEnum.isCountDistinct(func)) {
                        function.setName("count");
                        function.setDistinct(true);
                    } else {
                        function.setName(func);
                    }
                    function.withParameters(new Column(field));
                    if (Objects.nonNull(selectExpressionItem.getAlias()) && StringUtils.isNotBlank(field)) {
                        selectExpressionItem.getAlias().setName(field);
                    }
                }
            }
        });
        return selectStatement.toString();
    }

    public static String replaceValue(String sql, Map<String, Map<String, String>> filedNameToValueMap) {
        return replaceValue(sql, filedNameToValueMap, true);
    }

    public static String replaceValue(String sql, Map<String, Map<String, String>> filedNameToValueMap,
                                      boolean exactReplace) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        //List<PlainSelect> plainSelectList = new ArrayList<>();
        //plainSelectList.add((PlainSelect) selectStatement);
        List<PlainSelect> plainSelects = SqlSelectHelper.getPlainSelect(selectStatement);
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
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        List<PlainSelect> plainSelectList = new ArrayList<>();
        plainSelectList.add((PlainSelect) selectStatement);
        List<PlainSelect> plainSelects = SqlSelectHelper.getPlainSelects(plainSelectList);
        for (PlainSelect plainSelect : plainSelects) {
            Expression where = plainSelect.getWhere();
            FiledNameReplaceVisitor visitor = new FiledNameReplaceVisitor(fieldValueToFieldNames);
            if (Objects.nonNull(where)) {
                where.accept(visitor);
            }
        }
        return selectStatement.toString();
    }

    public static void getFromSelect(FromItem fromItem, List<PlainSelect> plainSelectList) {
        if (!(fromItem instanceof ParenthesedSelect)) {
            return;
        }
        ParenthesedSelect parenthesedSelect = (ParenthesedSelect) fromItem;
        Select select = parenthesedSelect.getSelect();
        if (select instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) select;
            plainSelectList.add(plainSelect);
            getFromSelect(plainSelect.getFromItem(), plainSelectList);
        } else if (select instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) select;
            if (!CollectionUtils.isEmpty(setOperationList.getSelects())) {
                setOperationList.getSelects().forEach(subSelectBody -> {
                    PlainSelect subPlainSelect = (PlainSelect) subSelectBody;
                    plainSelectList.add(subPlainSelect);
                    getFromSelect(subPlainSelect.getFromItem(), plainSelectList);
                });
            }
        }
    }

    public static String replaceFields(String sql, Map<String, String> fieldNameMap) {
        return replaceFields(sql, fieldNameMap, false);
    }

    public static String replaceFields(String sql, Map<String, String> fieldNameMap, boolean exactReplace) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        List<PlainSelect> plainSelectList = SqlSelectHelper.getWithItem(selectStatement);
        //plainSelectList.add(selectStatement.getPlainSelect());
        if (selectStatement instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectStatement;
            plainSelectList.add(plainSelect);
            getFromSelect(plainSelect.getFromItem(), plainSelectList);
            //plainSelectList.add((PlainSelect) selectStatement);
        } else if (selectStatement instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectStatement;
            if (!CollectionUtils.isEmpty(setOperationList.getSelects())) {
                setOperationList.getSelects().forEach(subSelectBody -> {
                    //PlainSelect subPlainSelect = (PlainSelect) subSelectBody;
                    //plainSelectList.add(subPlainSelect);
                    PlainSelect subPlainSelect = (PlainSelect) subSelectBody;
                    plainSelectList.add(subPlainSelect);
                    getFromSelect(subPlainSelect.getFromItem(), plainSelectList);
                });
            }
            List<OrderByElement> orderByElements = setOperationList.getOrderByElements();
            if (!CollectionUtils.isEmpty(orderByElements)) {
                for (OrderByElement orderByElement : orderByElements) {
                    orderByElement.accept(new OrderByReplaceVisitor(fieldNameMap, exactReplace));
                }
            }
        } else {
            return sql;
        }
        List<PlainSelect> plainSelects = SqlSelectHelper.getPlainSelects(plainSelectList);
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

        if (plainSelect.getFromItem() instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) plainSelect.getFromItem();
            Select select = parenthesedSelect.getSelect();
            if (select instanceof PlainSelect) {
                PlainSelect subPlainSelect = (PlainSelect) select;
                replaceFieldsInPlainOneSelect(fieldNameMap, exactReplace, subPlainSelect);
            } else if (select instanceof SetOperationList) {
                SetOperationList setOperationList = (SetOperationList) select;
                if (!CollectionUtils.isEmpty(setOperationList.getSelects())) {
                    setOperationList.getSelects().forEach(subSelectBody -> {
                        PlainSelect subPlainSelect = (PlainSelect) subSelectBody;
                        replaceFieldsInPlainOneSelect(fieldNameMap, exactReplace, subPlainSelect);
                    });
                }
            }
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
                if (!CollectionUtils.isEmpty(join.getOnExpressions())) {
                    join.getOnExpressions().stream().forEach(onExpression -> {
                        onExpression.accept(visitor);
                    });
                }
                if (!(join.getRightItem() instanceof ParenthesedSelect)) {
                    continue;
                }
                ParenthesedSelect parenthesedSelect = (ParenthesedSelect) join.getRightItem();
                List<PlainSelect> plainSelectList = new ArrayList<>();
                plainSelectList.add(parenthesedSelect.getPlainSelect());
                List<PlainSelect> subPlainSelects = SqlSelectHelper.getPlainSelects(plainSelectList);
                for (PlainSelect subPlainSelect : subPlainSelects) {
                    replaceFieldsInPlainOneSelect(fieldNameMap, exactReplace, subPlainSelect);
                }
            }
        }
    }

    private static void replaceAsName(Map<String, String> fieldNameMap, SelectItem selectItem) {

        Alias alias = selectItem.getAlias();
        if (Objects.isNull(alias)) {
            return;
        }
        String aliasName = alias.getName();
        String replaceFieldName = fieldNameMap.get(aliasName);
        if (StringUtils.isNotBlank(replaceFieldName)) {
            alias.setName(replaceFieldName);
        }

    }

    public static String replaceFunction(String sql, Map<String, String> functionMap) {
        return replaceFunction(sql, functionMap, null);
    }

    public static String replaceFunction(String sql, Map<String, String> functionMap,
                                         Map<String, UnaryOperator> functionCall) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        List<PlainSelect> plainSelectList = new ArrayList<>();
        plainSelectList.add((PlainSelect) selectStatement);
        List<PlainSelect> plainSelects = SqlSelectHelper.getPlainSelects(plainSelectList);
        for (PlainSelect plainSelect : plainSelects) {
            replaceFunction(functionMap, functionCall, plainSelect);
        }
        return selectStatement.toString();
    }

    private static void replaceFunction(Map<String, String> functionMap, Map<String, UnaryOperator> functionCall,
                                        PlainSelect selectBody) {
        PlainSelect plainSelect = selectBody;
        //1. replace where dataDiff function
        Expression where = plainSelect.getWhere();

        FunctionNameReplaceVisitor visitor = new FunctionNameReplaceVisitor(functionMap, functionCall);
        if (Objects.nonNull(where)) {
            where.accept(visitor);
        }
        GroupByElement groupBy = plainSelect.getGroupBy();
        if (Objects.nonNull(groupBy)) {
            GroupByFunctionReplaceVisitor replaceVisitor = new GroupByFunctionReplaceVisitor(functionMap, functionCall);
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
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        Expression where = ((PlainSelect) selectStatement).getWhere();
        try {
            Expression expression = SqlRemoveHelper.filteredExpression(where, SqlEditEnum.DATEDIFF);
            ((PlainSelect) selectStatement).setWhere(expression);
        } catch (Exception e) {
            log.info("replaceFunction has an exception:{}", e.toString());
        }

        return selectStatement.toString();
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

    public static String replaceTable(String sql, String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            return sql;
        }
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        List<PlainSelect> plainSelectList = SqlSelectHelper.getWithItem(selectStatement);
        if (!CollectionUtils.isEmpty(plainSelectList)) {
            List<String> withNameList = SqlSelectHelper.getWithName(sql);
            plainSelectList.stream().forEach(plainSelect -> {
                if (plainSelect.getFromItem() instanceof Table) {
                    Table table = (Table) plainSelect.getFromItem();
                    if (!withNameList.contains(table.getName())) {
                        replaceSingleTable(plainSelect, tableName);
                    }
                }
                if (plainSelect.getFromItem() instanceof ParenthesedSelect) {
                    ParenthesedSelect parenthesedSelect = (ParenthesedSelect) plainSelect.getFromItem();
                    PlainSelect subPlainSelect = parenthesedSelect.getPlainSelect();
                    Table table = (Table) subPlainSelect.getFromItem();
                    if (!withNameList.contains(table.getName())) {
                        replaceSingleTable(subPlainSelect, tableName);
                    }
                }
            });
            return selectStatement.toString();
        }
        if (selectStatement instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectStatement;
            replaceSingleTable(plainSelect, tableName);
            replaceSubTable(plainSelect, tableName);
        } else if (selectStatement instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectStatement;
            if (!CollectionUtils.isEmpty(setOperationList.getSelects())) {
                setOperationList.getSelects().forEach(subSelectBody -> {
                    PlainSelect subPlainSelect = (PlainSelect) subSelectBody;
                    replaceSingleTable(subPlainSelect, tableName);
                    replaceSubTable(subPlainSelect, tableName);
                });
            }
        }

        return selectStatement.toString();
    }

    public static void replaceSubTable(PlainSelect plainSelect, String tableName) {
        if (plainSelect.getFromItem() instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) plainSelect.getFromItem();
            PlainSelect subPlainSelect = parenthesedSelect.getPlainSelect();
            replaceSingleTable(subPlainSelect, tableName);
        }
        List<Join> joinList = plainSelect.getJoins();
        if (CollectionUtils.isEmpty(joinList)) {
            return;
        }
        for (Join join : joinList) {
            if (join.getFromItem() instanceof ParenthesedSelect) {
                ParenthesedSelect parenthesedSelect = (ParenthesedSelect) join.getFromItem();
                replaceSingleTable(parenthesedSelect.getPlainSelect(), tableName);
            }
        }
    }

    public static void replaceSingleTable(PlainSelect plainSelect, String tableName) {
        // replace table name
        List<PlainSelect> plainSelects = new ArrayList<>();
        plainSelects.add(plainSelect);
        List<PlainSelect> painSelects = SqlSelectHelper.getPlainSelects(plainSelects);
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
                    if (join.getRightItem() instanceof ParenthesedFromItem) {
                        List<PlainSelect> plainSelectList = new ArrayList<>();
                        plainSelectList.add((PlainSelect) join.getRightItem());
                        List<PlainSelect> subPlainSelects = SqlSelectHelper.getPlainSelects(plainSelectList);
                        for (PlainSelect subPlainSelect : subPlainSelects) {
                            subPlainSelect.getFromItem().accept(new TableNameReplaceVisitor(tableName));
                        }
                    } else if (join.getRightItem() instanceof Table) {
                        Table table = (Table) join.getRightItem();
                        table.setName(tableName);
                    }
                }
            }
        }
    }

    public static String replaceAlias(String sql) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectStatement;
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
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectStatement;
        Expression having = plainSelect.getHaving();
        FieldlValueReplaceVisitor visitor = new FieldlValueReplaceVisitor(false, filedNameToValueMap);
        if (Objects.nonNull(having)) {
            having.accept(visitor);
        }
        return selectStatement.toString();
    }

    public static Expression distinguishDateDiffFilter(Expression leftExpression, Expression expression) {
        if (leftExpression instanceof Function) {
            Function function = (Function) leftExpression;
            if (function.getName().equals(JsqlConstants.DATE_FUNCTION)) {
                ComparisonOperator comparisonOperator = (ComparisonOperator) expression;
                ExpressionList<?> leftExpressions = function.getParameters();
                Column field = (Column) function.getParameters().getExpressions().get(1);
                String columnName = field.getColumnName();
                try {
                    String startDateValue = DateFunctionHelper.getStartDateStr(comparisonOperator, leftExpressions);
                    String endDateValue = DateFunctionHelper.getEndDateValue(leftExpressions);
                    String dateOperator = comparisonOperator.getStringExpression();
                    String endDateOperator = JsqlConstants.rightMap.get(dateOperator);
                    String startDateOperator = JsqlConstants.leftMap.get(dateOperator);

                    String endDateCondExpr = columnName + endDateOperator + StringUtil.getCommaWrap(endDateValue);
                    ComparisonOperator rightExpression = (ComparisonOperator)
                            CCJSqlParserUtil.parseCondExpression(endDateCondExpr);

                    String startDateCondExpr = columnName + StringUtil.getSpaceWrap(startDateOperator)
                            + StringUtil.getCommaWrap(startDateValue);
                    ComparisonOperator newLeftExpression = (ComparisonOperator)
                            CCJSqlParserUtil.parseCondExpression(startDateCondExpr);

                    AndExpression andExpression = new AndExpression(newLeftExpression, rightExpression);
                    if (JsqlConstants.GREATER_THAN.equals(dateOperator)
                            || JsqlConstants.GREATER_THAN_EQUALS.equals(dateOperator)) {
                        return newLeftExpression;
                    } else {
                        return CCJSqlParserUtil.parseCondExpression("(" + andExpression.toString() + ")");
                    }
                } catch (JSQLParserException e) {
                    log.error("JSQLParserException", e);
                }
            }
            return expression;
        } else {
            return expression;
        }
    }

    private static Select replaceAggAliasOrderItem(Select selectStatement) {
        if (selectStatement instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectStatement;
            if (Objects.nonNull(plainSelect.getOrderByElements())) {
                Map<String, String> selectNames = new HashMap<>();
                for (int i = 0; i < plainSelect.getSelectItems().size(); i++) {
                    SelectItem<?> f = plainSelect.getSelectItem(i);
                    if (Objects.nonNull(f.getAlias()) && f.getExpression() instanceof Function) {
                        Function function = (Function) f.getExpression();
                        String alias = f.getAlias().getName();
                        if (function.getParameters().size() == 1 && function.getParameters().get(0) instanceof Column) {
                            Column column = (Column) function.getParameters().get(0);
                            if (column.getColumnName().equalsIgnoreCase(alias)) {
                                selectNames.put(alias, String.valueOf(i + 1));
                            }
                        }
                    }
                }
                plainSelect.getOrderByElements().stream().forEach(o -> {
                    if (o.getExpression() instanceof Function) {
                        Function function = (Function) o.getExpression();
                        if (function.getParameters().size() == 1 && function.getParameters().get(0) instanceof Column) {
                            Column column = (Column) function.getParameters().get(0);
                            if (selectNames.containsKey(column.getColumnName())) {
                                o.setExpression(new LongValue(selectNames.get(column.getColumnName())));
                            }
                        }
                    }
                });
            }
            if (plainSelect.getFromItem() instanceof ParenthesedSelect) {
                ParenthesedSelect parenthesedSelect = (ParenthesedSelect) plainSelect.getFromItem();
                parenthesedSelect.setSelect(replaceAggAliasOrderItem(parenthesedSelect.getSelect()));
            }
            return selectStatement;
        }
        return selectStatement;
    }

    public static String replaceAggAliasOrderItem(String sql) {
        Select selectStatement = replaceAggAliasOrderItem(SqlSelectHelper.getSelect(sql));
        return selectStatement.toString();
    }

    public static String replaceExpression(String expr, Map<String, String> replace) {
        Expression expression = QueryExpressionReplaceVisitor.getExpression(expr);
        if (Objects.nonNull(expression)) {
            if (expression instanceof Column && replace.containsKey(expr)) {
                return replace.get(expr);
            }
            ExpressionReplaceVisitor expressionReplaceVisitor = new ExpressionReplaceVisitor(replace);
            expression.accept(expressionReplaceVisitor);
            return expression.toString();
        }
        return expr;
    }

    public static String replaceSqlByExpression(String sql, Map<String, String> replace) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        List<PlainSelect> plainSelectList = new ArrayList<>();
        if (selectStatement instanceof PlainSelect) {
            plainSelectList.add((PlainSelect) selectStatement);
        } else if (selectStatement instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectStatement;
            if (!CollectionUtils.isEmpty(setOperationList.getSelects())) {
                setOperationList.getSelects().forEach(subSelectBody -> {
                    PlainSelect subPlainSelect = (PlainSelect) subSelectBody;
                    plainSelectList.add(subPlainSelect);
                });
            }
        } else {
            return sql;
        }
        List<PlainSelect> plainSelects = SqlSelectHelper.getPlainSelects(plainSelectList);
        for (PlainSelect plainSelect : plainSelects) {
            replacePlainSelectByExpr(plainSelect, replace);
        }
        return selectStatement.toString();
    }

    private static void replacePlainSelectByExpr(PlainSelect plainSelect, Map<String, String> replace) {
        QueryExpressionReplaceVisitor expressionReplaceVisitor = new QueryExpressionReplaceVisitor(replace);
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            selectItem.accept(expressionReplaceVisitor);
        }
        Expression having = plainSelect.getHaving();
        if (Objects.nonNull(having)) {
            having.accept(expressionReplaceVisitor);
        }

        Expression where = plainSelect.getWhere();
        if (Objects.nonNull(where)) {
            where.accept(expressionReplaceVisitor);
        }

        List<OrderByElement> orderByElements = plainSelect.getOrderByElements();
        if (!CollectionUtils.isEmpty(orderByElements)) {
            for (OrderByElement orderByElement : orderByElements) {
                orderByElement.setExpression(
                        QueryExpressionReplaceVisitor.replace(orderByElement.getExpression(), replace));
            }
        }
    }

    public static String dealAliasToOrderBy(String querySql) {
        Select selectStatement = SqlSelectHelper.getSelect(querySql);
        List<PlainSelect> plainSelectList = new ArrayList<>();
        //List<PlainSelect> withPlainSelectList = SqlSelectHelper.getWithItem(selectStatement);
        //if (!CollectionUtils.isEmpty(withPlainSelectList)) {
        //    plainSelectList.addAll(withPlainSelectList);
        //}
        if (selectStatement instanceof PlainSelect) {
            plainSelectList.add((PlainSelect) selectStatement);
        } else if (selectStatement instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectStatement;
            if (!CollectionUtils.isEmpty(setOperationList.getSelects())) {
                setOperationList.getSelects().forEach(subSelectBody -> {
                    PlainSelect subPlainSelect = (PlainSelect) subSelectBody;
                    plainSelectList.add(subPlainSelect);
                });
            }
        }
        for (PlainSelect plainSelect : plainSelectList) {
            List<SelectItem<?>> selectItemList = plainSelect.getSelectItems();
            List<OrderByElement> orderByElementList = plainSelect.getOrderByElements();
            if (CollectionUtils.isEmpty(orderByElementList)) {
                continue;
            }
            Map<String, Expression> map = new HashMap<>();
            for (int i = 0; i < selectItemList.size(); i++) {
                if (!Objects.isNull(selectItemList.get(i).getAlias())) {
                    map.put(selectItemList.get(i).getAlias().getName(), selectItemList.get(i).getExpression());
                    selectItemList.get(i).setAlias(null);
                }
            }
            for (OrderByElement orderByElement : orderByElementList) {
                if (map.containsKey(orderByElement.getExpression().toString())) {
                    orderByElement.setExpression(map.get(orderByElement.getExpression().toString()));
                }
            }
            plainSelect.setOrderByElements(orderByElementList);
        }
        return selectStatement.toString();
    }

}

