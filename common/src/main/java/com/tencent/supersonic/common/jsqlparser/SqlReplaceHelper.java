package com.tencent.supersonic.common.jsqlparser;

import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.util.EditDistanceUtils;
import com.tencent.supersonic.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
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
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Sql Parser replace Helper
 */
@Slf4j
public class SqlReplaceHelper {

    private final static double replaceColumnThreshold = 0.4;

    public static String escapeTableName(String table) {
        return String.format("`%s`", table);
    }

    public static String replaceAggFields(String sql,
            Map<String, Pair<String, String>> fieldNameToAggMap) {
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
                    if (Objects.nonNull(selectExpressionItem.getAlias())
                            && StringUtils.isNotBlank(field)) {
                        selectExpressionItem.getAlias().setName(field);
                    }
                }
            }
        });
        return selectStatement.toString();
    }

    public static String replaceValue(String sql,
            Map<String, Map<String, String>> filedNameToValueMap) {
        return replaceValue(sql, filedNameToValueMap, true);
    }

    public static String replaceValue(String sql,
            Map<String, Map<String, String>> filedNameToValueMap, boolean exactReplace) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        List<PlainSelect> plainSelects = SqlSelectHelper.getPlainSelect(selectStatement);
        for (PlainSelect plainSelect : plainSelects) {
            Expression where = plainSelect.getWhere();
            FieldValueReplaceVisitor visitor =
                    new FieldValueReplaceVisitor(exactReplace, filedNameToValueMap);
            if (Objects.nonNull(where)) {
                where.accept(visitor);
            }
        }
        return selectStatement.toString();
    }

    public static String replaceFieldNameByValue(String sql,
            Map<String, Set<String>> fieldValueToFieldNames) {
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

    public static String replaceFields(String sql, Map<String, String> fieldNameMap,
            boolean exactReplace) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        Set<Select> plainSelectList = SqlSelectHelper.getAllSelect(selectStatement);
        for (Select plainSelect : plainSelectList) {
            if (plainSelect instanceof PlainSelect) {
                replaceFieldsInPlainOneSelect(fieldNameMap, exactReplace,
                        (PlainSelect) plainSelect);
            }
            if (plainSelect instanceof SetOperationList) {
                replaceFieldsInSetOperationList(fieldNameMap, exactReplace,
                        (SetOperationList) plainSelect);
            }
        }
        return selectStatement.toString();
    }

    private static void replaceFieldsInPlainOneSelect(Map<String, String> fieldNameMap,
            boolean exactReplace, PlainSelect plainSelect) {
        // 1. replace where fields
        Expression where = plainSelect.getWhere();
        FieldReplaceVisitor visitor = new FieldReplaceVisitor(fieldNameMap, exactReplace);
        if (Objects.nonNull(where)) {
            where.accept(visitor);
        }

        // 2. replace select fields
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            selectItem.accept(visitor);
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

        // 3. replace oder by fields
        List<OrderByElement> orderByElements = plainSelect.getOrderByElements();
        if (!CollectionUtils.isEmpty(orderByElements)) {
            for (OrderByElement orderByElement : orderByElements) {
                orderByElement.accept(new OrderByReplaceVisitor(fieldNameMap, exactReplace));
            }
        }
        // 4. replace group by fields
        GroupByElement groupByElement = plainSelect.getGroupBy();
        if (Objects.nonNull(groupByElement)) {
            groupByElement.accept(new GroupByReplaceVisitor(fieldNameMap, exactReplace));
        }
        // 5. replace having fields
        Expression having = plainSelect.getHaving();
        if (Objects.nonNull(having)) {
            having.accept(visitor);
        }
        List<Join> joins = plainSelect.getJoins();
        if (!CollectionUtils.isEmpty(joins)) {
            for (Join join : joins) {
                if (CollectionUtils.isEmpty(join.getOnExpressions())) {
                    continue;

                }
                join.getOnExpressions().stream().forEach(onExpression -> {
                    onExpression.accept(visitor);
                });
            }
        }
    }


    private static void replaceFieldsInSetOperationList(Map<String, String> fieldNameMap,
            boolean exactReplace, SetOperationList operationList) {
        List<OrderByElement> orderByElements = operationList.getOrderByElements();
        if (!CollectionUtils.isEmpty(orderByElements)) {
            for (OrderByElement orderByElement : orderByElements) {
                orderByElement.accept(new OrderByReplaceVisitor(fieldNameMap, exactReplace));
            }
        }
        List<Select> selects = operationList.getSelects();
        if (!CollectionUtils.isEmpty(selects)) {
            for (Select select : selects) {
                if (select instanceof PlainSelect) {
                    replaceFieldsInPlainOneSelect(fieldNameMap, exactReplace, (PlainSelect) select);
                }
            }
        }
        List<WithItem> withItems = operationList.getWithItemsList();
        if (!CollectionUtils.isEmpty(withItems)) {
            for (WithItem withItem : withItems) {
                Select select = withItem.getSelect();
                if (select instanceof PlainSelect) {
                    replaceFieldsInPlainOneSelect(fieldNameMap, exactReplace, (PlainSelect) select);
                } else if (select instanceof ParenthesedSelect) {
                    replaceFieldsInPlainOneSelect(fieldNameMap, exactReplace,
                            select.getPlainSelect());
                }
            }
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

    private static void replaceFunction(Map<String, String> functionMap,
            Map<String, UnaryOperator> functionCall, PlainSelect selectBody) {
        PlainSelect plainSelect = selectBody;
        // 1. replace where dataDiff function
        Expression where = plainSelect.getWhere();

        FunctionNameReplaceVisitor visitor =
                new FunctionNameReplaceVisitor(functionMap, functionCall);
        if (Objects.nonNull(where)) {
            where.accept(visitor);
        }
        GroupByElement groupBy = plainSelect.getGroupBy();
        if (Objects.nonNull(groupBy)) {
            GroupByFunctionReplaceVisitor replaceVisitor =
                    new GroupByFunctionReplaceVisitor(functionMap, functionCall);
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

    private static void replaceComparisonOperatorFunction(Map<String, String> functionMap,
            Expression expression) {
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
        List<String> withNameList = SqlSelectHelper.getWithName(sql);
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        List<PlainSelect> plainSelectList = SqlSelectHelper.getWithItem(selectStatement);

        if (!CollectionUtils.isEmpty(plainSelectList)) {
            plainSelectList.forEach(
                    plainSelect -> processPlainSelect(plainSelect, tableName, withNameList));
        }
        if (selectStatement instanceof PlainSelect) {
            processPlainSelect((PlainSelect) selectStatement, tableName, withNameList);
        } else if (selectStatement instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectStatement;
            if (!CollectionUtils.isEmpty(setOperationList.getSelects())) {
                setOperationList.getSelects()
                        .forEach(subSelectBody -> processPlainSelect((PlainSelect) subSelectBody,
                                tableName, withNameList));
            }
        }

        return selectStatement.toString();
    }

    private static void processPlainSelect(PlainSelect plainSelect, String tableName,
            List<String> withNameList) {
        if (plainSelect.getFromItem() instanceof Table) {
            replaceSingleTable(plainSelect, tableName, withNameList);
        } else if (plainSelect.getFromItem() instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) plainSelect.getFromItem();
            PlainSelect subPlainSelect = parenthesedSelect.getPlainSelect();
            replaceSingleTable(subPlainSelect, tableName, withNameList);
        }
        replaceSubTable(plainSelect, tableName, withNameList);
    }

    public static void replaceSingleTable(PlainSelect plainSelect, String tableName,
            List<String> withNameList) {
        List<PlainSelect> plainSelects =
                SqlSelectHelper.getPlainSelects(Collections.singletonList(plainSelect));
        plainSelects.forEach(painSelect -> {
            painSelect.accept(new SelectVisitorAdapter() {
                @Override
                public void visit(PlainSelect plainSelect) {
                    if (Objects.nonNull(plainSelect.getFromItem())) {
                        plainSelect.getFromItem().accept(new TableNameReplaceVisitor(tableName,
                                new HashSet<>(withNameList)));
                    }
                }
            });
            replaceJoins(painSelect, tableName, withNameList);
        });
    }

    private static void replaceJoins(PlainSelect plainSelect, String tableName,
            List<String> withNameList) {
        List<Join> joins = plainSelect.getJoins();
        TableNameReplaceVisitor fromItemVisitor =
                new TableNameReplaceVisitor(tableName, new HashSet<>(withNameList));
        if (!CollectionUtils.isEmpty(joins)) {
            for (Join join : joins) {
                if (join.getRightItem() instanceof ParenthesedFromItem) {
                    List<PlainSelect> subPlainSelects = SqlSelectHelper.getPlainSelects(
                            Collections.singletonList((PlainSelect) join.getRightItem()));
                    subPlainSelects.forEach(subPlainSelect -> {
                        subPlainSelect.getFromItem().accept(fromItemVisitor);
                    });
                } else if (join.getRightItem() instanceof Table) {
                    join.getRightItem().accept(fromItemVisitor);
                }
            }
        }
    }

    public static void replaceSubTable(PlainSelect plainSelect, String tableName,
            List<String> withNameList) {
        if (plainSelect.getFromItem() instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) plainSelect.getFromItem();
            replaceSingleTable(parenthesedSelect.getPlainSelect(), tableName, withNameList);
        }

        List<Join> joinList = plainSelect.getJoins();
        if (!CollectionUtils.isEmpty(joinList)) {
            joinList.forEach(join -> {
                if (join.getFromItem() instanceof ParenthesedSelect) {
                    ParenthesedSelect parenthesedSelect = (ParenthesedSelect) join.getFromItem();
                    replaceSingleTable(parenthesedSelect.getPlainSelect(), tableName, withNameList);
                }
            });
        }
    }

    public static String replaceAliasFieldName(String sql, Map<String, String> fieldNameMap) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectStatement;
        FieldAliasReplaceNameVisitor visitor = new FieldAliasReplaceNameVisitor(fieldNameMap);
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            selectItem.accept(visitor);
        }
        Map<String, String> aliasToActualExpression = visitor.getAliasToActualExpression();
        if (Objects.nonNull(aliasToActualExpression) && !aliasToActualExpression.isEmpty()) {
            return replaceFields(selectStatement.toString(), aliasToActualExpression, true);
        }
        return selectStatement.toString();
    }

    public static String replaceAliasWithBackticks(String sql) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectStatement;
        FieldAliasReplaceWithBackticksVisitor visitor = new FieldAliasReplaceWithBackticksVisitor();
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            selectItem.accept(visitor);
        }
        // Replace `order by` and `group by`
        // Get the map of field aliases that have been replaced
        Map<String, String> aliasReplacedMap = visitor.getFieldAliasReplacedMap();

        // If no aliases have been replaced, return the original SQL statement as a string
        if (aliasReplacedMap.isEmpty()) {
            return selectStatement.toString();
        }
        // Order by elements
        List<OrderByElement> orderByElements = selectStatement.getOrderByElements();
        if (!CollectionUtils.isEmpty(orderByElements)) {
            for (OrderByElement orderByElement : orderByElements) {
                orderByElement.accept(new OrderByReplaceVisitor(aliasReplacedMap, true));
            }
        }
        // Group by elements
        GroupByElement groupByElement = plainSelect.getGroupBy();
        if (Objects.nonNull(groupByElement)) {
            groupByElement.accept(new GroupByReplaceVisitor(aliasReplacedMap, true));
        }
        // Alias columns
        for (SelectItem<?> selectItem : plainSelect.getSelectItems()) {
            if (selectItem.getExpression() instanceof Column) {
                replaceColumn((Column) selectItem.getExpression(), aliasReplacedMap, true);
            }
        }
        // Having
        Expression having = plainSelect.getHaving();
        if (Objects.nonNull(having)) {
            ExpressionReplaceVisitor expressionReplaceVisitor =
                    new ExpressionReplaceVisitor(aliasReplacedMap);
            having.accept(expressionReplaceVisitor);
        }
        return selectStatement.toString();
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

    public static String replaceHavingValue(String sql,
            Map<String, Map<String, String>> filedNameToValueMap) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (!(selectStatement instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectStatement;
        Expression having = plainSelect.getHaving();
        FieldValueReplaceVisitor visitor = new FieldValueReplaceVisitor(false, filedNameToValueMap);
        if (Objects.nonNull(having)) {
            having.accept(visitor);
        }
        return selectStatement.toString();
    }

    public static Expression distinguishDateDiffFilter(Expression leftExpression,
            Expression expression) {
        if (leftExpression instanceof Function) {
            Function function = (Function) leftExpression;
            if (function.getName().equals(JsqlConstants.DATE_FUNCTION)) {
                ComparisonOperator comparisonOperator = (ComparisonOperator) expression;
                ExpressionList<?> leftExpressions = function.getParameters();
                Column field = (Column) function.getParameters().getExpressions().get(1);
                String columnName = field.getColumnName();
                try {
                    String startDateValue =
                            DateFunctionHelper.getStartDateStr(comparisonOperator, leftExpressions);
                    String endDateValue = DateFunctionHelper.getEndDateValue(leftExpressions);
                    String dateOperator = comparisonOperator.getStringExpression();
                    String endDateOperator = JsqlConstants.rightMap.get(dateOperator);
                    String startDateOperator = JsqlConstants.leftMap.get(dateOperator);

                    String endDateCondExpr =
                            columnName + endDateOperator + StringUtil.getCommaWrap(endDateValue);
                    ComparisonOperator rightExpression = (ComparisonOperator) CCJSqlParserUtil
                            .parseCondExpression(endDateCondExpr);

                    String startDateCondExpr =
                            columnName + StringUtil.getSpaceWrap(startDateOperator)
                                    + StringUtil.getCommaWrap(startDateValue);
                    ComparisonOperator newLeftExpression = (ComparisonOperator) CCJSqlParserUtil
                            .parseCondExpression(startDateCondExpr);

                    AndExpression andExpression =
                            new AndExpression(newLeftExpression, rightExpression);
                    if (JsqlConstants.GREATER_THAN.equals(dateOperator)
                            || JsqlConstants.GREATER_THAN_EQUALS.equals(dateOperator)) {
                        return newLeftExpression;
                    } else {
                        return CCJSqlParserUtil
                                .parseCondExpression("(" + andExpression.toString() + ")");
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

    private static Select replaceAggAliasOrderbyField(Select selectStatement) {
        if (selectStatement instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectStatement;
            if (Objects.nonNull(plainSelect.getOrderByElements())) {
                Map<String, String> selectNames = new HashMap<>();
                for (int i = 0; i < plainSelect.getSelectItems().size(); i++) {
                    SelectItem<?> f = plainSelect.getSelectItem(i);
                    if (Objects.nonNull(f.getAlias()) && f.getExpression() instanceof Function) {
                        Function function = (Function) f.getExpression();
                        String alias = f.getAlias().getName();
                        if (function.getParameters().size() == 1
                                && function.getParameters().get(0) instanceof Column) {
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
                        if (function.getParameters().size() == 1
                                && function.getParameters().get(0) instanceof Column) {
                            Column column = (Column) function.getParameters().get(0);
                            if (selectNames.containsKey(column.getColumnName())) {
                                o.setExpression(
                                        new LongValue(selectNames.get(column.getColumnName())));
                            }
                        }
                    }
                });
            }
            if (plainSelect.getFromItem() instanceof ParenthesedSelect) {
                ParenthesedSelect parenthesedSelect = (ParenthesedSelect) plainSelect.getFromItem();
                parenthesedSelect
                        .setSelect(replaceAggAliasOrderbyField(parenthesedSelect.getSelect()));
            }
            return selectStatement;
        }
        return selectStatement;
    }

    public static String replaceAggAliasOrderbyField(String sql) {
        Select selectStatement = replaceAggAliasOrderbyField(SqlSelectHelper.getSelect(sql));
        return selectStatement.toString();
    }

    public static String replaceExpression(String expr, Map<String, String> replace) {
        Expression expression = QueryExpressionReplaceVisitor.getExpression(expr);
        if (Objects.nonNull(expression)) {
            if (expression instanceof Column && replace.containsKey(expr)) {
                return replace.get(expr);
            }
            ExpressionReplaceVisitor expressionReplaceVisitor =
                    new ExpressionReplaceVisitor(replace);
            expression.accept(expressionReplaceVisitor);
            return expression.toString();
        }
        return expr;
    }

    public static String replaceSqlByExpression(String tableName, String sql,
            Map<String, String> replace) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        List<PlainSelect> plainSelectList = new ArrayList<>();
        if (selectStatement instanceof PlainSelect) {
            // if with statement exists, replace expression in the with statement.
            if (!CollectionUtils.isEmpty(selectStatement.getWithItemsList())) {
                selectStatement.getWithItemsList().forEach(withItem -> {
                    plainSelectList.add(withItem.getSelect().getPlainSelect());
                });
            }
            plainSelectList.add((PlainSelect) selectStatement);
        } else if (selectStatement instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectStatement;
            if (!CollectionUtils.isEmpty(setOperationList.getSelects())) {
                setOperationList.getSelects().forEach(subSelectBody -> {
                    PlainSelect subPlainSelect = (PlainSelect) subSelectBody;
                    plainSelectList.add(subPlainSelect);
                });
            }
            List<Select> selects = setOperationList.getSelects();
            if (!CollectionUtils.isEmpty(selects)) {
                for (Select select : selects) {
                    if (select instanceof PlainSelect) {
                        plainSelectList.add((PlainSelect) select);
                    }
                }
            }
            List<WithItem> withItems = setOperationList.getWithItemsList();
            if (!CollectionUtils.isEmpty(withItems)) {
                for (WithItem withItem : withItems) {
                    Select select = withItem.getSelect();
                    if (select instanceof PlainSelect) {
                        plainSelectList.add((PlainSelect) select);
                    } else if (select instanceof ParenthesedSelect) {
                        plainSelectList.add(select.getPlainSelect());
                    }
                }
            }
        } else {
            return sql;
        }

        List<PlainSelect> plainSelects = SqlSelectHelper.getPlainSelects(plainSelectList);
        for (PlainSelect plainSelect : plainSelects) {
            if (Objects.nonNull(plainSelect.getFromItem())) {
                Table table = (Table) plainSelect.getFromItem();
                if (table.getName().equals(tableName)) {
                    replacePlainSelectByExpr(plainSelect, replace);
                    if (SqlSelectHelper.hasAggregateFunction(plainSelect)) {
                        SqlSelectHelper.addMissingGroupby(plainSelect);
                    }
                }
            }
        }
        return selectStatement.toString();
    }

    public static void replaceSqlByPositions(Select select) {
        if (select instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) select;
            if (plainSelect.getSelectItems() != null) {
                Map<String, Integer> columnMap = new HashMap<>();
                for (int i = 0; i < plainSelect.getSelectItems().size(); i++) {
                    SelectItem selectItem = plainSelect.getSelectItems().get(i);
                    if (selectItem.getAlias() != null) {
                        columnMap.put(selectItem.getAlias().getName(), i + 1);
                    } else if (selectItem.getExpression() instanceof Column) {
                        Column column = (Column) selectItem.getExpression();
                        columnMap.put(column.getColumnName(), i + 1);
                    }
                }
                if (plainSelect.getGroupBy() != null) {
                    ExpressionList groupByExpressionList =
                            plainSelect.getGroupBy().getGroupByExpressionList();
                    List<Expression> groupByExpressions = groupByExpressionList.getExpressions();
                    for (Expression expression : groupByExpressions) {
                        if (expression instanceof Column) {
                            Column column = (Column) expression;
                            if (columnMap.containsKey(column.getColumnName())) {
                                column.setColumnName(
                                        String.valueOf(columnMap.get(column.getColumnName())));
                            }
                        }
                    }
                }
                if (plainSelect.getOrderByElements() != null) {
                    for (OrderByElement orderByElement : plainSelect.getOrderByElements()) {
                        if (orderByElement.getExpression() instanceof Column) {
                            Column column = (Column) orderByElement.getExpression();
                            if (columnMap.containsKey(column.getColumnName())) {
                                orderByElement.setExpression(
                                        new LongValue(columnMap.get(column.getColumnName())));
                            }
                        }
                    }
                }
            }
        }
        if (select instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) select;
            replaceSqlByPositions(parenthesedSelect.getSelect());
        }
        if (select instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) select;
            setOperationList.getSelects().forEach(s -> replaceSqlByPositions(s));
        }
    }

    public static String replaceSqlByPositions(String sql) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        replaceSqlByPositions(selectStatement);
        return selectStatement.toString();
    }

    private static void replacePlainSelectByExpr(PlainSelect plainSelect,
            Map<String, String> replace) {
        QueryExpressionReplaceVisitor expressionReplaceVisitor =
                new QueryExpressionReplaceVisitor(replace);
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
                orderByElement.setExpression(QueryExpressionReplaceVisitor
                        .replace(orderByElement.getExpression(), replace));
            }
        }
    }

    public static void replaceFunction(Function expression, Map<String, String> fieldNameMap,
            boolean exactReplace) {
        Function function = expression;
        ExpressionList<?> expressions = function.getParameters();
        for (Expression column : expressions) {
            if (column instanceof Column) {
                replaceColumn((Column) column, fieldNameMap, exactReplace);
            }
        }
    }

    public static void replaceColumn(Column column, Map<String, String> fieldNameMap,
            boolean exactReplace) {
        String columnName = StringUtil.replaceBackticks(column.getColumnName());
        String replaceColumn = getReplaceValue(columnName, fieldNameMap, exactReplace);
        if (StringUtils.isNotBlank(replaceColumn)) {
            log.debug("Replaced column {} to {}", column.getColumnName(), replaceColumn);
            column.setColumnName(replaceColumn);
        }
    }

    public static String getReplaceValue(String beforeValue, Map<String, String> valueMap,
            boolean exactReplace) {
        String replaceValue = valueMap.get(beforeValue);
        if (StringUtils.isNotBlank(replaceValue)) {
            return replaceValue;
        }
        if (exactReplace) {
            return null;
        }
        Optional<Map.Entry<String, String>> first =
                valueMap.entrySet().stream().sorted((k1, k2) -> {
                    String k1Value = k1.getKey();
                    String k2Value = k2.getKey();
                    Double k1Similarity = EditDistanceUtils.getSimilarity(beforeValue, k1Value);
                    Double k2Similarity = EditDistanceUtils.getSimilarity(beforeValue, k2Value);
                    return k2Similarity.compareTo(k1Similarity);
                }).collect(Collectors.toList()).stream().findFirst();

        if (first.isPresent()) {
            replaceValue = first.get().getValue();
            double similarity = EditDistanceUtils.getSimilarity(beforeValue, replaceValue);
            if (similarity > replaceColumnThreshold) {
                return replaceValue;
            }
        }
        return beforeValue;
    }

}
