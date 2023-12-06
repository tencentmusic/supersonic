package com.tencent.supersonic.common.util.jsqlparser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.SetOperationList;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.util.CollectionUtils;

/**
 * Sql Parser Select Helper
 */
@Slf4j
public class SqlParserSelectHelper {

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

    private static void getWhereFields(List<PlainSelect> plainSelectList, Set<String> result) {
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
            List<SelectItem> selectItems = plainSelect.getSelectItems();
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
        SelectBody selectBody = selectStatement.getSelectBody();

        List<PlainSelect> plainSelectList = new ArrayList<>();
        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;
            plainSelectList.add(plainSelect);
        } else if (selectBody instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectBody;
            if (!CollectionUtils.isEmpty(setOperationList.getSelects())) {
                setOperationList.getSelects().forEach(subSelectBody -> {
                    PlainSelect subPlainSelect = (PlainSelect) subSelectBody;
                    plainSelectList.add(subPlainSelect);
                });
            }
        }
        return plainSelectList;
    }

    public static Select getSelect(String sql) {
        Statement statement = null;
        try {
            statement = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            log.error("parse error, sql:{}", sql, e);
            return null;
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
                public void visit(SubSelect subSelect) {
                    SelectBody subSelectBody = subSelect.getSelectBody();
                    if (subSelectBody instanceof PlainSelect) {
                        plainSelects.add((PlainSelect) subSelectBody);
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
                    List<SelectItem> selectItems = plainSelect.getSelectItems();
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

        return new ArrayList<>(result);
    }

    private static void getHavingFields(PlainSelect plainSelect, Set<String> result) {
        Expression having = plainSelect.getHaving();
        if (Objects.nonNull(having)) {
            having.accept(new FieldAcquireVisitor(result));
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
        return table.getName();
    }

    public static List<String> getAggregateFields(String sql) {
        List<PlainSelect> plainSelectList = getPlainSelect(sql);
        Set<String> result = new HashSet<>();
        for (PlainSelect plainSelect : plainSelectList) {
            if (Objects.isNull(plainSelect)) {
                continue;
            }
            List<SelectItem> selectItems = plainSelect.getSelectItems();
            for (SelectItem selectItem : selectItems) {
                if (selectItem instanceof SelectExpressionItem) {
                    SelectExpressionItem expressionItem = (SelectExpressionItem) selectItem;
                    if (expressionItem.getExpression() instanceof Function) {
                        Function function = (Function) expressionItem.getExpression();
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
        SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            return false;
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        GroupByElement groupBy = plainSelect.getGroupBy();
        if (Objects.nonNull(groupBy)) {
            GroupByVisitor replaceVisitor = new GroupByVisitor();
            groupBy.accept(replaceVisitor);
            return replaceVisitor.isHasAggregateFunction();
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
            List<Expression> expressionList = ((Function) leftExpression).getParameters().getExpressions();
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
            if (!CollectionUtils.isEmpty(function.getParameters().getExpressions())) {
                Expression expression = function.getParameters().getExpressions().get(0);
                if (expression instanceof Column) {
                    return ((Column) expression).getColumnName();
                }
            }
        }
        return "";
    }

    public static Expression getTimeFilter(List<ImmutablePair<String, String>> times, String columnBegin,
            String columnEnd) {
        Expression expression = null;
        for (ImmutablePair<String, String> t : times) {
            Expression expr = null;
            ComparisonOperator left = new MinorThanEquals();
            if (t.left.equals(t.right)) {
                left.setLeftExpression(new Column(columnBegin));
                left.setRightExpression(new StringValue(t.left));
                ComparisonOperator right = new GreaterThan();
                right.setLeftExpression(new Column(columnEnd));
                right.setRightExpression(new StringValue(t.right));
                expr = new AndExpression(left, right);
            } else {
                left.setLeftExpression(new StringValue(t.left));
                left.setRightExpression(new Column(columnEnd));
                ComparisonOperator right = new GreaterThanEquals();
                right.setLeftExpression(new StringValue(t.right));
                right.setRightExpression(new Column(columnBegin));
                expr = new AndExpression(left, right);
            }
            if (expression == null) {
                expression = expr;
                continue;
            }
            expression = new OrExpression(expression, expr);
        }
        return expression;
    }

    public static Table getTable(String sql) {
        Select selectStatement = getSelect(sql);
        if (selectStatement == null) {
            return null;
        }
        SelectBody selectBody = selectStatement.getSelectBody();
        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;
            return (Table) plainSelect.getFromItem();
        } else if (selectBody instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectBody;
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

    public static String getNormalizedSql(String sql) {
        Select selectStatement = getSelect(sql);
        if (selectStatement == null) {
            return null;
        }
        return selectStatement.toString();
    }
}

