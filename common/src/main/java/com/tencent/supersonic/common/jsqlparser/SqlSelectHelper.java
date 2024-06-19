package com.tencent.supersonic.common.jsqlparser;

import com.tencent.supersonic.common.util.StringUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
import net.sf.jsqlparser.statement.select.Distinct;
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

    public static List<PlainSelect> getPlainSelect(String sql) {
        Select selectStatement = getSelect(sql);
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
            return null;
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

    public static List<String> getAllFields(String sql) {
        List<PlainSelect> plainSelects = getPlainSelects(getPlainSelect(sql));
        Set<String> results = new HashSet<>();
        for (PlainSelect plainSelect : plainSelects) {
            List<String> fields = getFieldsByPlainSelect(plainSelect);
            results.addAll(fields);
        }
        return new ArrayList<>(results);
    }

    private static List<String> getFieldsByPlainSelect(PlainSelect plainSelect) {
        if (Objects.isNull(plainSelect)) {
            return new ArrayList<>();
        }
        List<PlainSelect> plainSelectList = new ArrayList<>();
        plainSelectList.add(plainSelect);
        Set<String> result = getSelectFields(plainSelectList);

        getGroupByFields(plainSelect, result);

        getOrderByFields(plainSelect, result);

        getWhereFields(plainSelectList, result);

        getHavingFields(plainSelect, result);

        getLateralViewsFields(plainSelect, result);

        return new ArrayList<>(result);
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
                PlainSelect subPlainSelect = parenthesedSelect.getPlainSelect();
                Expression subWhere = subPlainSelect.getWhere();
                if (Objects.nonNull(subWhere)) {
                    subWhere.accept(new FieldAndValueAcquireVisitor(result));
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
                .map(fieldExpression -> fieldExpression.getFieldName())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        result.addAll(collect);
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
                    if (Objects.nonNull(function.getParameters())
                            && !CollectionUtils.isEmpty(function.getParameters().getExpressions())) {
                        String columnName = function.getParameters().getExpressions().get(0).toString();
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
                        if (Objects.nonNull(function.getParameters())
                                && !CollectionUtils.isEmpty(function.getParameters().getExpressions())) {
                            String columnName = function.getParameters().getExpressions().get(0).toString();
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
        PlainSelect plainSelect = (PlainSelect) selectStatement;
        GroupByElement groupBy = plainSelect.getGroupBy();
        if (Objects.nonNull(groupBy)) {
            GroupByVisitor replaceVisitor = new GroupByVisitor();
            groupBy.accept(replaceVisitor);
            return replaceVisitor.isHasAggregateFunction();
        }
        return false;
    }

    public static boolean hasDistinct(String sql) {
        Select selectStatement = getSelect(sql);

        if (!(selectStatement instanceof PlainSelect)) {
            return false;
        }
        PlainSelect plainSelect = (PlainSelect) selectStatement;
        Distinct distinct = plainSelect.getDistinct();
        return Objects.nonNull(distinct);
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

            if (!CollectionUtils.isEmpty(expressionList) && expressionList.get(0) instanceof Column) {
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
                        ParenthesedSelect parenthesedSelect = (ParenthesedSelect) withPlainSelect.getFromItem();
                        plainSelectList.add(parenthesedSelect.getPlainSelect());
                    }
                }
                if (withSelect instanceof ParenthesedSelect) {
                    ParenthesedSelect parenthesedSelect = (ParenthesedSelect) withSelect;
                    PlainSelect withPlainSelect = parenthesedSelect.getPlainSelect();
                    plainSelectList.add(withPlainSelect);
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

    public static Map<String, WithItem> getWith(String sql) {
        Select selectStatement = getSelect(sql);
        if (selectStatement == null) {
            return new HashMap<>();
        }
        Map<String, WithItem> withMap = new HashMap<>();
        List<WithItem> withItemList = selectStatement.getWithItemsList();
        if (!CollectionUtils.isEmpty(withItemList)) {
            for (int i = 0; i < withItemList.size(); i++) {
                WithItem withItem = withItemList.get(i);
                withMap.put(withItem.getAlias().getName(), withItem);
            }
        }
        return withMap;
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

                PlainSelect subSelect = ((ParenthesedSelect) plainSelect.getFromItem()).getPlainSelect();
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

    public static Set<String> getColumnFromExpr(String expr) {
        Expression expression = QueryExpressionReplaceVisitor.getExpression(expr);
        Set<String> columns = new HashSet<>();
        if (Objects.nonNull(expression)) {
            getColumnFromExpr(expression, columns);
        }
        return columns;
    }

    public static void getColumnFromExpr(Expression expression, Set<String> columns) {
        if (expression instanceof Column) {
            columns.add(((Column) expression).getColumnName());
        }
        if (expression instanceof Function) {
            ExpressionList<?> expressionList = ((Function) expression).getParameters();
            for (Expression expr : expressionList) {
                getColumnFromExpr(expr, columns);
            }
        }
        if (expression instanceof CaseExpression) {
            CaseExpression expr = (CaseExpression) expression;
            if (Objects.nonNull(expr.getWhenClauses())) {
                for (WhenClause whenClause : expr.getWhenClauses()) {
                    getColumnFromExpr(whenClause.getWhenExpression(), columns);
                    getColumnFromExpr(whenClause.getThenExpression(), columns);
                }
            }
            if (Objects.nonNull(expr.getElseExpression())) {
                getColumnFromExpr(expr.getElseExpression(), columns);
            }
        }
        if (expression instanceof BinaryExpression) {
            BinaryExpression expr = (BinaryExpression) expression;
            getColumnFromExpr(expr.getLeftExpression(), columns);
            getColumnFromExpr(expr.getRightExpression(), columns);
        }
        if (expression instanceof InExpression) {
            InExpression inExpression = (InExpression) expression;
            getColumnFromExpr(inExpression.getLeftExpression(), columns);
        }
        if (expression instanceof Between) {
            Between between = (Between) expression;
            getColumnFromExpr(between.getLeftExpression(), columns);
        }
        if (expression instanceof IsBooleanExpression) {
            IsBooleanExpression isBooleanExpression = (IsBooleanExpression) expression;
            getColumnFromExpr(isBooleanExpression.getLeftExpression(), columns);
        }
        if (expression instanceof IsNullExpression) {
            IsNullExpression isNullExpression = (IsNullExpression) expression;
            getColumnFromExpr(isNullExpression.getLeftExpression(), columns);
        }
        if (expression instanceof Parenthesis) {
            Parenthesis expr = (Parenthesis) expression;
            getColumnFromExpr(expr.getExpression(), columns);
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

    private static void getFieldsWithSubQuery(PlainSelect plainSelect, Map<String, Set<String>> fields) {
        if (plainSelect.getFromItem() instanceof Table) {
            boolean isWith = false;
            if (!CollectionUtils.isEmpty(plainSelect.getWithItemsList())) {
                for (WithItem withItem : plainSelect.getWithItemsList()) {
                    if (Objects.nonNull(withItem.getSelect())) {
                        getFieldsWithSubQuery(withItem.getSelect().getPlainSelect(), fields);
                        isWith = true;
                    }
                }
            }
            if (!isWith) {
                Table table = (Table) plainSelect.getFromItem();
                if (!fields.containsKey(table.getFullyQualifiedName())) {
                    fields.put(table.getFullyQualifiedName(), new HashSet<>());
                }
                List<String> sqlFields = getFieldsByPlainSelect(plainSelect).stream().map(f -> f.replaceAll("`", ""))
                        .collect(
                                Collectors.toList());
                fields.get(table.getFullyQualifiedName()).addAll(sqlFields);
            }
        }
        if (plainSelect.getFromItem() instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) plainSelect.getFromItem();
            getFieldsWithSubQuery(parenthesedSelect.getPlainSelect(), fields);
            if (!CollectionUtils.isEmpty(plainSelect.getJoins())) {
                for (Join join : plainSelect.getJoins()) {
                    if (join.getRightItem() instanceof ParenthesedSelect) {
                        getFieldsWithSubQuery(((ParenthesedSelect) join.getRightItem()).getPlainSelect(), fields);
                    }
                    if (join.getFromItem() instanceof ParenthesedSelect) {
                        getFieldsWithSubQuery(((ParenthesedSelect) join.getFromItem()).getPlainSelect(), fields);
                    }
                }
            }
        }
    }
}

