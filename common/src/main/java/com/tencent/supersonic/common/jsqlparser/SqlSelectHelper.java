package com.tencent.supersonic.common.jsqlparser;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsBooleanExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.LateralView;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.WithItem;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sql Parser Select Helper
 */
@Slf4j
public class SqlSelectHelper {

    public static List<FieldExpression> getFilterExpression(String sql) {
        List<PlainSelect> plainSelectList = getPlainSelect(sql);
        Set<FieldExpression> result = new HashSet<>();
        for (PlainSelect plainSelect : plainSelectList) {
            if (Objects.isNull(plainSelect)) {
                continue;
            }
            Expression where = plainSelect.getWhere();
            if (Objects.nonNull(where)) {
                where.accept(new FieldAndValueAcquireVisitor(result));
            }
            Expression having = plainSelect.getHaving();
            if (Objects.nonNull(having)) {
                having.accept(new FieldAndValueAcquireVisitor(result));
            }
        }
        result = result.stream()
                .filter(fieldExpression -> StringUtils.isNotBlank(fieldExpression.getFieldName()))
                .collect(Collectors.toSet());
        return new ArrayList<>(result);
    }

    public static List<String> getWhereFields(String sql) {
        List<PlainSelect> plainSelectList = getPlainSelect(sql);
        if (CollectionUtils.isEmpty(plainSelectList)) {
            return new ArrayList<>();
        }
        Set<String> result = new HashSet<>();
        getWhereFields(plainSelectList, result);
        return new ArrayList<>(result);
    }

    public static void getWhereFields(List<PlainSelect> plainSelectList, Set<String> result) {
        plainSelectList.stream().forEach(plainSelect -> {
            Expression where = plainSelect.getWhere();
            if (Objects.nonNull(where)) {
                where.accept(new FieldAcquireVisitor(result));
            }
        });
    }

    public static List<String> gePureSelectFields(String sql) {
        List<PlainSelect> plainSelectList = getPlainSelect(sql);
        Set<String> result = new HashSet<>();
        plainSelectList.stream().forEach(plainSelect -> {
            List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
            for (SelectItem selectItem : selectItems) {
                if (!(selectItem.getExpression() instanceof Column)) {
                    continue;
                }
                Column column = (Column) selectItem.getExpression();
                result.add(column.getColumnName());
            }
        });
        return new ArrayList<>(result);
    }

    public static List<String> getSelectFields(String sql) {
        List<PlainSelect> plainSelectList = getPlainSelect(sql);
        if (CollectionUtils.isEmpty(plainSelectList)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(getSelectFields(plainSelectList));
    }

    public static Set<String> getSelectFields(List<PlainSelect> plainSelectList) {
        Set<String> result = new HashSet<>();
        plainSelectList.stream().forEach(plainSelect -> {
            List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
            for (SelectItem selectItem : selectItems) {
                selectItem.accept(new FieldAcquireVisitor(result));
            }
        });
        return result;
    }

    public static Set<String> getAliasFields(PlainSelect plainSelect) {
        Set<String> result = new HashSet<>();
        List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
        for (SelectItem selectItem : selectItems) {
            selectItem.accept(new AliasAcquireVisitor(result));
        }
        return result;
    }

    public static Set<String> getAliasFields(String sql) {
        List<PlainSelect> plainSelects = getPlainSelects(getPlainSelect(sql));
        Set<String> aliasFields = new HashSet<>();
        plainSelects.forEach(select -> {
            aliasFields.addAll(getAliasFields(select));
        });
        return aliasFields;
    }

    public static List<PlainSelect> getPlainSelect(Select selectStatement) {
        if (selectStatement == null) {
            return null;
        }

        List<PlainSelect> plainSelectList = new ArrayList<>();
        plainSelectList.addAll(getWithItem(selectStatement));
        if (selectStatement instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectStatement;
            getSubPlainSelect(plainSelect, plainSelectList);
        } else if (selectStatement instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectStatement;
            if (!CollectionUtils.isEmpty(setOperationList.getSelects())) {
                setOperationList.getSelects().forEach(subSelectBody -> {
                    PlainSelect subPlainSelect = (PlainSelect) subSelectBody;
                    getSubPlainSelect(subPlainSelect, plainSelectList);
                });
            }
        }
        return plainSelectList;
    }

    public static List<PlainSelect> getPlainSelect(String sql) {
        Select selectStatement = getSelect(sql);
        return getPlainSelect(selectStatement);
    }

    public static Boolean hasSubSelect(String sql) {
        Select selectStatement = getSelect(sql);
        if (selectStatement == null) {
            return false;
        }
        if (selectStatement instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectStatement;
            if (plainSelect.getFromItem() instanceof ParenthesedSelect) {
                return true;
            }
        }
        return false;
    }

    public static void getSubPlainSelect(Select select, List<PlainSelect> plainSelectList) {
        if (select instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) select;
            plainSelectList.add(plainSelect);
            if (plainSelect.getFromItem() instanceof ParenthesedSelect) {
                ParenthesedSelect parenthesedSelect = (ParenthesedSelect) plainSelect.getFromItem();
                Select subSelect = parenthesedSelect.getSelect();
                getSubPlainSelect(subSelect, plainSelectList);
            }
            List<Join> joinList = plainSelect.getJoins();
            if (CollectionUtils.isEmpty(joinList)) {
                return;
            }
            for (Join join : joinList) {
                if (join.getRightItem() instanceof ParenthesedSelect) {
                    ParenthesedSelect parenthesedSelect = (ParenthesedSelect) join.getRightItem();
                    plainSelectList.add(parenthesedSelect.getPlainSelect());
                }
            }
        }
        if (select instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) select;
            for (Select subSelect : setOperationList.getSelects()) {
                getSubPlainSelect(subSelect, plainSelectList);
            }
        }
    }

    public static Select getSelect(String sql) {
        Statement statement = null;
        try {
            statement = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            log.error("parse error, sql:{}", sql, e);
            throw new RuntimeException(e);
        }

        if (statement instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) statement;
            return parenthesedSelect.getSelect();
        }

        if (!(statement instanceof Select)) {
            return null;
        }
        return (Select) statement;
    }

    public static List<PlainSelect> getPlainSelects(List<PlainSelect> plainSelectList) {
        List<PlainSelect> plainSelects = new ArrayList<>();
        for (PlainSelect plainSelect : plainSelectList) {
            plainSelects.add(plainSelect);
            ExpressionVisitorAdapter expressionVisitor = new ExpressionVisitorAdapter() {
                @Override
                public void visit(Select subSelect) {
                    if (subSelect instanceof ParenthesedSelect) {
                        ParenthesedSelect parenthesedSelect = (ParenthesedSelect) subSelect;
                        if (parenthesedSelect.getSelect() instanceof PlainSelect) {
                            plainSelects.add(parenthesedSelect.getPlainSelect());
                        }
                    }
                }
            };

            plainSelect.accept(new SelectVisitorAdapter() {
                @Override
                public void visit(PlainSelect plainSelect) {
                    Expression whereExpression = plainSelect.getWhere();
                    if (whereExpression != null) {
                        whereExpression.accept(expressionVisitor);
                    }
                    Expression having = plainSelect.getHaving();
                    if (Objects.nonNull(having)) {
                        having.accept(expressionVisitor);
                    }
                    List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
                    if (!CollectionUtils.isEmpty(selectItems)) {
                        for (SelectItem selectItem : selectItems) {
                            selectItem.accept(expressionVisitor);
                        }
                    }
                }
            });
        }
        return plainSelects;
    }

    public static List<String> getAllSelectFields(String sql) {
        List<PlainSelect> plainSelects = getPlainSelects(getPlainSelect(sql));
        Set<String> results = new HashSet<>();
        Set<String> aliases = new HashSet<>();
        for (PlainSelect plainSelect : plainSelects) {
            List<String> fields = getFieldsByPlainSelect(plainSelect);
            Set<String> subaliases = getAliasFields(plainSelect);
            subaliases.removeAll(fields);
            results.addAll(fields);
            aliases.addAll(subaliases);
        }
        // do not account in aliases
        results.removeAll(aliases);
        return new ArrayList<>(
                results.stream().map(r -> r.replaceAll("`", "")).collect(Collectors.toList()));
    }

    private static List<String> getFieldsByPlainSelect(PlainSelect plainSelect) {
        if (Objects.isNull(plainSelect)) {
            return new ArrayList<>();
        }
        List<PlainSelect> plainSelectList = new ArrayList<>();
        plainSelectList.add(plainSelect);
        Set<String> selectFields = getSelectFields(plainSelectList);
        Set<String> aliases = getAliasFields(plainSelect);

        Set<String> groupByFields = Sets.newHashSet();
        getGroupByFields(plainSelect, groupByFields);
        groupByFields.removeAll(aliases);

        Set<String> orderByFields = Sets.newHashSet();
        getOrderByFields(plainSelect, orderByFields);
        orderByFields.removeAll(aliases);

        Set<String> whereFields = Sets.newHashSet();
        getWhereFields(plainSelectList, whereFields);
        whereFields.removeAll(aliases);

        Set<String> havingFields = Sets.newHashSet();
        getHavingFields(plainSelect, havingFields);
        havingFields.removeAll(aliases);

        Set<String> lateralFields = Sets.newHashSet();
        getLateralViewsFields(plainSelect, lateralFields);
        lateralFields.removeAll(aliases);

        List<String> results = Lists.newArrayList();
        results.addAll(selectFields);
        results.addAll(groupByFields);
        results.addAll(orderByFields);
        results.addAll(whereFields);
        results.addAll(havingFields);
        results.addAll(lateralFields);
        return new ArrayList<>(results);
    }

    private static void getHavingFields(PlainSelect plainSelect, Set<String> result) {
        Expression having = plainSelect.getHaving();
        if (Objects.nonNull(having)) {
            having.accept(new FieldAcquireVisitor(result));
        }
    }

    private static void getLateralViewsFields(PlainSelect plainSelect, Set<String> result) {
        List<LateralView> lateralViews = plainSelect.getLateralViews();
        if (!CollectionUtils.isEmpty(lateralViews)) {
            lateralViews.stream().forEach(l -> {
                if (Objects.nonNull(l.getGeneratorFunction())) {
                    l.getGeneratorFunction().accept(new FieldAcquireVisitor(result));
                }
            });
        }
    }

    public static List<Expression> getHavingExpression(String sql) {
        List<PlainSelect> plainSelectList = getPlainSelect(sql);
        List<Expression> expressionList = new ArrayList<>();
        for (PlainSelect plainSelect : plainSelectList) {
            Expression having = plainSelect.getHaving();
            if (Objects.nonNull(having)) {
                if (!(having instanceof ComparisonOperator)) {
                    continue;
                }
                ComparisonOperator comparisonOperator = (ComparisonOperator) having;
                if (comparisonOperator.getLeftExpression() instanceof Function) {
                    expressionList.add(comparisonOperator.getLeftExpression());
                } else if (comparisonOperator.getRightExpression() instanceof Function) {
                    expressionList.add(comparisonOperator.getRightExpression());
                }
            }
        }
        return expressionList;
    }

    public static List<FieldExpression> getWhereExpressions(String sql) {
        List<PlainSelect> plainSelectList = getPlainSelect(sql);
        Set<FieldExpression> result = new HashSet<>();
        for (PlainSelect plainSelect : plainSelectList) {
            if (Objects.isNull(plainSelect)) {
                continue;
            }
            Expression where = plainSelect.getWhere();
            if (Objects.nonNull(where)) {
                where.accept(new FieldAndValueAcquireVisitor(result));
            }
            if (plainSelect.getFromItem() instanceof ParenthesedSelect) {
                ParenthesedSelect parenthesedSelect = (ParenthesedSelect) plainSelect.getFromItem();
                Select subSelect = parenthesedSelect.getSelect();
                if (subSelect instanceof PlainSelect) {
                    PlainSelect subPlainSelect = parenthesedSelect.getPlainSelect();
                    Expression subWhere = subPlainSelect.getWhere();
                    if (Objects.nonNull(subWhere)) {
                        subWhere.accept(new FieldAndValueAcquireVisitor(result));
                    }
                } else if (subSelect instanceof ParenthesedSelect) {
                    ParenthesedSelect subParenthesedSelect = (ParenthesedSelect) subSelect;
                    Expression subWhere = subParenthesedSelect.getPlainSelect().getWhere();
                    if (Objects.nonNull(subWhere)) {
                        subWhere.accept(new FieldAndValueAcquireVisitor(result));
                    }
                } else if (subSelect instanceof SetOperationList) {
                    SetOperationList setOperationList = (SetOperationList) subSelect;
                    List<Select> selectList = setOperationList.getSelects();
                    for (Select select : selectList) {
                        Expression subWhere = select.getPlainSelect().getWhere();
                        if (Objects.nonNull(subWhere)) {
                            subWhere.accept(new FieldAndValueAcquireVisitor(result));
                        }
                    }
                }
            }
        }
        return new ArrayList<>(result);
    }

    public static List<FieldExpression> getHavingExpressions(String sql) {
        List<PlainSelect> plainSelectList = getPlainSelect(sql);
        Set<FieldExpression> result = new HashSet<>();
        for (PlainSelect plainSelect : plainSelectList) {
            if (Objects.isNull(plainSelect)) {
                continue;
            }
            Expression having = plainSelect.getHaving();
            if (Objects.nonNull(having)) {
                having.accept(new FieldAndValueAcquireVisitor(result));
            }
        }
        return new ArrayList<>(result);
    }

    public static List<String> getOrderByFields(String sql) {
        List<PlainSelect> plainSelectList = getPlainSelect(sql);
        Set<String> result = new HashSet<>();
        for (PlainSelect plainSelect : plainSelectList) {
            if (Objects.isNull(plainSelect)) {
                continue;
            }
            getOrderByFields(plainSelect, result);
        }
        return new ArrayList<>(result);
    }

    private static Set<FieldExpression> getOrderByFields(PlainSelect plainSelect) {
        Set<FieldExpression> result = new HashSet<>();
        List<OrderByElement> orderByElements = plainSelect.getOrderByElements();
        if (!CollectionUtils.isEmpty(orderByElements)) {
            for (OrderByElement orderByElement : orderByElements) {
                orderByElement.accept(new OrderByAcquireVisitor(result));
            }
        }
        return result;
    }

    private static void getOrderByFields(PlainSelect plainSelect, Set<String> result) {
        Set<FieldExpression> orderByFieldExpressions = getOrderByFields(plainSelect);
        Set<String> collect = orderByFieldExpressions.stream()
                .map(fieldExpression -> fieldExpression.getFieldName()).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        result.addAll(collect);

        Set<String> aliases = getAliasFields(plainSelect);
        result.removeAll(aliases);
    }

    public static List<FieldExpression> getOrderByExpressions(String sql) {
        List<PlainSelect> plainSelectList = getPlainSelect(sql);
        HashSet<FieldExpression> result = new HashSet<>();
        for (PlainSelect plainSelect : plainSelectList) {
            if (Objects.isNull(plainSelect)) {
                return new ArrayList<>();
            }
            result.addAll(getOrderByFields(plainSelect));
        }
        return new ArrayList<>(result);
    }

    public static List<String> getGroupByFields(String sql) {
        List<PlainSelect> plainSelectList = getPlainSelect(sql);
        HashSet<String> result = new HashSet<>();
        for (PlainSelect plainSelect : plainSelectList) {
            if (Objects.isNull(plainSelect)) {
                continue;
            }
            getGroupByFields(plainSelect, result);
        }
        return new ArrayList<>(result);
    }

    private static void getGroupByFields(PlainSelect plainSelect, Set<String> result) {
        GroupByElement groupBy = plainSelect.getGroupBy();
        if (groupBy != null) {
            List<Expression> groupByExpressions = groupBy.getGroupByExpressions();
            for (Expression expression : groupByExpressions) {
                if (expression instanceof Column) {
                    Column column = (Column) expression;
                    result.add(column.getColumnName());
                }
            }
        }
    }

    public static String getTableName(String sql) {
        Table table = getTable(sql);
        return StringUtil.replaceBackticks(table.getName());
    }

    public static List<String> getAggregateFields(String sql) {
        List<PlainSelect> plainSelectList = getPlainSelect(sql);
        Set<String> result = new HashSet<>();
        for (PlainSelect plainSelect : plainSelectList) {
            if (Objects.isNull(plainSelect)) {
                continue;
            }
            List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
            for (SelectItem selectItem : selectItems) {

                if (selectItem.getExpression() instanceof Function) {
                    Function function = (Function) selectItem.getExpression();
                    if (Objects.nonNull(function.getParameters()) && !CollectionUtils
                            .isEmpty(function.getParameters().getExpressions())) {
                        String columnName =
                                function.getParameters().getExpressions().get(0).toString();
                        result.add(columnName);
                    }
                }
            }
        }
        return new ArrayList<>(result);
    }

    public static List<String> getAggregateAsFields(String sql) {
        List<PlainSelect> plainSelectList = getPlainSelect(sql);
        Set<String> result = new HashSet<>();
        for (PlainSelect plainSelect : plainSelectList) {
            if (Objects.isNull(plainSelect)) {
                continue;
            }
            List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
            for (SelectItem selectItem : selectItems) {

                if (selectItem.getExpression() instanceof Function) {
                    Function function = (Function) selectItem.getExpression();
                    Alias alias = selectItem.getAlias();
                    if (alias != null && StringUtils.isNotBlank(alias.getName())) {
                        result.add(alias.getName());
                    } else {
                        if (Objects.nonNull(function.getParameters()) && !CollectionUtils
                                .isEmpty(function.getParameters().getExpressions())) {
                            String columnName =
                                    function.getParameters().getExpressions().get(0).toString();
                            result.add(columnName);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(result);
    }

    public static boolean hasGroupBy(String sql) {
        Select selectStatement = getSelect(sql);

        if (!(selectStatement instanceof PlainSelect)) {
            return false;
        }

        List<PlainSelect> withItem = getWithItem(selectStatement);
        withItem.add((PlainSelect) selectStatement);

        for (PlainSelect plainSelect : withItem) {
            GroupByElement groupBy = plainSelect.getGroupBy();
            if (Objects.nonNull(groupBy)) {
                GroupByVisitor replaceVisitor = new GroupByVisitor();
                groupBy.accept(replaceVisitor);
                return replaceVisitor.isHasAggregateFunction();
            }
        }
        return false;
    }

    public static boolean isLogicExpression(Expression whereExpression) {
        return whereExpression instanceof AndExpression || (whereExpression instanceof OrExpression
                || (whereExpression instanceof XorExpression));
    }

    public static String getColumnName(Expression leftExpression, Expression rightExpression) {
        if (leftExpression instanceof Column) {
            return ((Column) leftExpression).getColumnName();
        }
        if (leftExpression instanceof Function) {
            ExpressionList<?> expressionList = ((Function) leftExpression).getParameters();

            if (!CollectionUtils.isEmpty(expressionList)
                    && expressionList.get(0) instanceof Column) {
                return ((Column) expressionList.get(0)).getColumnName();
            }
        }
        if (rightExpression instanceof Column) {
            return ((Column) rightExpression).getColumnName();
        }
        return "";
    }

    public static String getColumnName(Expression leftExpression) {
        if (leftExpression instanceof Column) {
            Column leftColumnName = (Column) leftExpression;
            return leftColumnName.getColumnName();
        }
        if (leftExpression instanceof Function) {
            Function function = (Function) leftExpression;
            if (!CollectionUtils.isEmpty(function.getParameters())) {
                Expression expression = (Expression) function.getParameters().get(0);
                if (expression instanceof Column) {
                    return ((Column) expression).getColumnName();
                }
            }
        }
        return "";
    }

    public static String getColumValue(Expression expression) {
        if (expression instanceof StringValue) {
            StringValue value = (StringValue) expression;
            return value.getValue();
        }
        if (expression instanceof LongValue) {
            LongValue value = (LongValue) expression;
            return String.valueOf(value.getValue());
        }
        return "";
    }

    public static Boolean hasWith(String sql) {
        Select selectStatement = getSelect(sql);
        if (selectStatement == null) {
            return false;
        }
        List<WithItem> withItemList = selectStatement.getWithItemsList();
        if (!CollectionUtils.isEmpty(withItemList)) {
            return true;
        } else {
            return false;
        }
    }

    public static List<PlainSelect> getWithItem(Select selectStatement) {
        if (selectStatement == null) {
            return new ArrayList<>();
        }
        List<PlainSelect> plainSelectList = new ArrayList<>();
        List<WithItem> withItemList = selectStatement.getWithItemsList();
        if (!CollectionUtils.isEmpty(withItemList)) {
            for (int i = 0; i < withItemList.size(); i++) {
                WithItem withItem = withItemList.get(i);
                Select withSelect = withItem.getSelect();
                if (withSelect instanceof PlainSelect) {
                    PlainSelect withPlainSelect = (PlainSelect) withSelect;
                    plainSelectList.add(withPlainSelect);
                    if (withPlainSelect.getFromItem() instanceof ParenthesedSelect) {
                        ParenthesedSelect parenthesedSelect =
                                (ParenthesedSelect) withPlainSelect.getFromItem();
                        plainSelectList.add(parenthesedSelect.getPlainSelect());
                    }
                }
                if (withSelect instanceof ParenthesedSelect) {
                    ParenthesedSelect parenthesedSelect = (ParenthesedSelect) withSelect;
                    List<PlainSelect> plainSelects = new ArrayList<>();
                    SqlReplaceHelper.getFromSelect(parenthesedSelect, plainSelects);
                    plainSelectList.addAll(plainSelects);
                }
            }
        }
        return plainSelectList;
    }

    public static List<String> getWithName(String sql) {
        Select selectStatement = getSelect(sql);
        if (selectStatement == null) {
            return new ArrayList<>();
        }
        List<String> withNameList = new ArrayList<>();
        List<WithItem> withItemList = selectStatement.getWithItemsList();
        if (!CollectionUtils.isEmpty(withItemList)) {
            for (int i = 0; i < withItemList.size(); i++) {
                WithItem withItem = withItemList.get(i);
                withNameList.add(withItem.getAlias().getName());
            }
        }
        return withNameList;
    }

    public static Table getTable(String sql) {
        Select selectStatement = getSelect(sql);
        if (selectStatement == null) {
            return null;
        }
        List<PlainSelect> plainSelectList = getWithItem(selectStatement);
        if (!CollectionUtils.isEmpty(plainSelectList)) {
            Table table = getTable(plainSelectList.get(0).toString());
            return table;
        }
        if (selectStatement instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectStatement;
            if (plainSelect.getFromItem() instanceof Table) {
                return (Table) plainSelect.getFromItem();
            }
            if (plainSelect.getFromItem() instanceof ParenthesedSelect) {

                PlainSelect subSelect =
                        ((ParenthesedSelect) plainSelect.getFromItem()).getPlainSelect();
                return getTable(subSelect.getSelectBody().toString());
            }

        } else if (selectStatement instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectStatement;
            if (!CollectionUtils.isEmpty(setOperationList.getSelects())) {
                return (Table) ((PlainSelect) setOperationList.getSelects().get(0)).getFromItem();
            }
        }
        return null;
    }

    public static String getDbTableName(String sql) {
        Table table = getTable(sql);
        return table.getFullyQualifiedName();
    }

    public static Set<String> getFieldsFromExpr(String expr) {
        Expression expression = QueryExpressionReplaceVisitor.getExpression(expr);
        Set<String> columns = new HashSet<>();
        if (Objects.nonNull(expression)) {
            getFieldsFromExpr(expression, columns);
        }
        return columns;
    }

    public static void getFieldsFromExpr(Expression expression, Set<String> columns) {
        if (expression instanceof Column) {
            columns.add(((Column) expression).getColumnName());
        }
        if (expression instanceof Function) {
            ExpressionList<?> expressionList = ((Function) expression).getParameters();
            for (Expression expr : expressionList) {
                getFieldsFromExpr(expr, columns);
            }
        }
        if (expression instanceof CaseExpression) {
            CaseExpression expr = (CaseExpression) expression;
            if (Objects.nonNull(expr.getWhenClauses())) {
                for (WhenClause whenClause : expr.getWhenClauses()) {
                    getFieldsFromExpr(whenClause.getWhenExpression(), columns);
                    getFieldsFromExpr(whenClause.getThenExpression(), columns);
                }
            }
            if (Objects.nonNull(expr.getElseExpression())) {
                getFieldsFromExpr(expr.getElseExpression(), columns);
            }
        }
        if (expression instanceof BinaryExpression) {
            BinaryExpression expr = (BinaryExpression) expression;
            getFieldsFromExpr(expr.getLeftExpression(), columns);
            getFieldsFromExpr(expr.getRightExpression(), columns);
        }
        if (expression instanceof InExpression) {
            InExpression inExpression = (InExpression) expression;
            getFieldsFromExpr(inExpression.getLeftExpression(), columns);
        }
        if (expression instanceof Between) {
            Between between = (Between) expression;
            getFieldsFromExpr(between.getLeftExpression(), columns);
        }
        if (expression instanceof IsBooleanExpression) {
            IsBooleanExpression isBooleanExpression = (IsBooleanExpression) expression;
            getFieldsFromExpr(isBooleanExpression.getLeftExpression(), columns);
        }
        if (expression instanceof IsNullExpression) {
            IsNullExpression isNullExpression = (IsNullExpression) expression;
            getFieldsFromExpr(isNullExpression.getLeftExpression(), columns);
        }
        if (expression instanceof Parenthesis) {
            Parenthesis expr = (Parenthesis) expression;
            getFieldsFromExpr(expr.getExpression(), columns);
        }
    }

    public static Boolean hasLimit(String querySql) {
        Select selectStatement = SqlSelectHelper.getSelect(querySql);
        if (selectStatement instanceof PlainSelect) {
            PlainSelect plainSelect = selectStatement.getPlainSelect();
            Limit limit = plainSelect.getLimit();
            return Objects.nonNull(limit);
        } else if (selectStatement instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectStatement;
            Boolean result = true;
            if (!CollectionUtils.isEmpty(setOperationList.getSelects())) {
                for (Select select : setOperationList.getSelects()) {
                    PlainSelect subPlainSelect = select.getPlainSelect();
                    Limit limit = subPlainSelect.getLimit();
                    if (Objects.isNull(limit)) {
                        result = false;
                        break;
                    }
                }
            }
            return result;
        }
        return false;
    }

    public static Map<String, Set<String>> getFieldsWithSubQuery(String sql) {
        List<PlainSelect> plainSelects = getPlainSelects(getPlainSelect(sql));
        Map<String, Set<String>> results = new HashMap<>();
        for (PlainSelect plainSelect : plainSelects) {
            getFieldsWithSubQuery(plainSelect, results);
        }
        return results;
    }

    private static void getFieldsWithSubQuery(PlainSelect plainSelect,
            Map<String, Set<String>> fields) {
        if (plainSelect.getFromItem() instanceof Table) {
            List<String> withAlias = new ArrayList<>();
            if (!CollectionUtils.isEmpty(plainSelect.getWithItemsList())) {
                for (WithItem withItem : plainSelect.getWithItemsList()) {
                    if (Objects.nonNull(withItem.getSelect())) {
                        getFieldsWithSubQuery(withItem.getSelect().getPlainSelect(), fields);
                        withAlias.add(withItem.getAlias().getName());
                    }
                }
            }
            Table table = (Table) plainSelect.getFromItem();
            String tableName = table.getFullyQualifiedName();
            if (!withAlias.contains(tableName)) {
                if (!fields.containsKey(table.getFullyQualifiedName())) {
                    fields.put(tableName, new HashSet<>());
                }
                List<String> sqlFields = getFieldsByPlainSelect(plainSelect).stream()
                        .map(f -> f.replaceAll("`", "")).collect(Collectors.toList());
                fields.get(tableName).addAll(sqlFields);
            }
        }
        if (plainSelect.getFromItem() instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) plainSelect.getFromItem();
            getFieldsWithSubQuery(parenthesedSelect.getPlainSelect(), fields);
            if (CollectionUtils.isEmpty(plainSelect.getJoins())) {
                return;
            }
            for (Join join : plainSelect.getJoins()) {
                if (join.getRightItem() instanceof ParenthesedSelect) {
                    getFieldsWithSubQuery(
                            ((ParenthesedSelect) join.getRightItem()).getPlainSelect(), fields);
                }
                if (join.getFromItem() instanceof ParenthesedSelect) {
                    getFieldsWithSubQuery(((ParenthesedSelect) join.getFromItem()).getPlainSelect(),
                            fields);
                }
            }
        }
    }

    public static Set<Select> getAllSelect(String sql) {
        return getAllSelect(getSelect(sql));
    }

    public static Set<Select> getAllSelect(Select selectStatement) {
        Set<Select> selects = new HashSet<>();
        collectSelects(selectStatement, selects);
        return selects;
    }

    private static void collectSelects(Select select, Set<Select> selects) {
        if (select == null) {
            return;
        }
        if (select instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) select;
            selects.add(plainSelect);
            collectFromItemPlainSelects(plainSelect.getFromItem(), selects);
            collectWithItemPlainSelects(plainSelect.getWithItemsList(), selects);
            collectJoinsPlainSelects(plainSelect.getJoins(), selects);
            collectNestedPlainSelects(plainSelect, selects);
        } else if (select instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) select;
            selects.add(setOperationList);
            if (!CollectionUtils.isEmpty(setOperationList.getSelects())) {
                for (Select subSelectBody : setOperationList.getSelects()) {
                    collectSelects(subSelectBody, selects);
                }
            }
        } else if (select instanceof WithItem) {
            WithItem withItem = (WithItem) select;
            collectSelects(withItem.getSelect(), selects);
        } else if (select instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) select;
            List<PlainSelect> plainSelects = new ArrayList<>();
            SqlReplaceHelper.getFromSelect(parenthesedSelect, plainSelects);
            plainSelects.forEach(s -> collectSelects(s, selects));
        }
    }

    private static void collectJoinsPlainSelects(List<Join> joins, Set<Select> selects) {
        if (CollectionUtils.isEmpty(joins)) {
            return;
        }
        for (Join join : joins) {
            FromItem rightItem = join.getRightItem();
            if (!(rightItem instanceof ParenthesedSelect)) {
                continue;
            }
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) rightItem;
            selects.add(parenthesedSelect.getPlainSelect());
        }
    }

    private static void collectFromItemPlainSelects(FromItem fromItem, Set<Select> selects) {
        if (fromItem instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) fromItem;
            collectSelects(parenthesedSelect.getSelect(), selects);
        }
    }

    public static void collectWithItemPlainSelects(List<WithItem> withItemList,
            Set<Select> selects) {
        if (CollectionUtils.isEmpty(withItemList)) {
            return;
        }
        for (WithItem withItem : withItemList) {
            collectSelects(withItem.getSelect(), selects);
        }
    }

    private static void collectNestedPlainSelects(PlainSelect plainSelect, Set<Select> selects) {
        ExpressionVisitorAdapter expressionVisitor = new ExpressionVisitorAdapter() {
            @Override
            public void visit(Select subSelect) {
                if (subSelect instanceof ParenthesedSelect) {
                    ParenthesedSelect parenthesedSelect = (ParenthesedSelect) subSelect;
                    if (parenthesedSelect.getSelect() instanceof PlainSelect) {
                        selects.add(parenthesedSelect.getPlainSelect());
                    }
                }
            }
        };

        plainSelect.accept(new SelectVisitorAdapter() {
            @Override
            public void visit(PlainSelect plainSelect) {
                Expression whereExpression = plainSelect.getWhere();
                if (whereExpression != null) {
                    whereExpression.accept(expressionVisitor);
                }
                Expression having = plainSelect.getHaving();
                if (Objects.nonNull(having)) {
                    having.accept(expressionVisitor);
                }
                List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
                if (!CollectionUtils.isEmpty(selectItems)) {
                    for (SelectItem selectItem : selectItems) {
                        selectItem.accept(expressionVisitor);
                    }
                }
            }
        });
    }

    public static void addMissingGroupby(PlainSelect plainSelect) {
        if (Objects.nonNull(plainSelect.getGroupBy())
                && !plainSelect.getGroupBy().getGroupByExpressionList().isEmpty()) {
            return;
        }
        GroupByElement groupBy = new GroupByElement();
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            Expression expression = selectItem.getExpression();
            if (expression instanceof Column) {
                groupBy.addGroupByExpression(expression);
            }
        }
        if (!groupBy.getGroupByExpressionList().isEmpty()) {
            plainSelect.setGroupByElement(groupBy);
        }
    }

    public static boolean hasAggregateFunction(PlainSelect plainSelect) {
        List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
        FunctionVisitor visitor = new FunctionVisitor();
        for (SelectItem selectItem : selectItems) {
            selectItem.accept(visitor);
        }
        return !visitor.getFunctionNames().isEmpty();
    }

}
