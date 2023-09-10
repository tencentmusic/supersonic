package com.tencent.supersonic.common.util.jsqlparser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.springframework.util.CollectionUtils;

/**
 * Sql Parser Select Helper
 */
@Slf4j
public class SqlParserSelectHelper {

    public static List<FilterExpression> getFilterExpression(String sql) {
        PlainSelect plainSelect = getPlainSelect(sql);
        if (Objects.isNull(plainSelect)) {
            return new ArrayList<>();
        }
        Set<FilterExpression> result = new HashSet<>();
        Expression where = plainSelect.getWhere();
        if (Objects.nonNull(where)) {
            where.accept(new FieldAndValueAcquireVisitor(result));
        }
        return new ArrayList<>(result);
    }

    public static List<String> getWhereFields(String sql) {
        PlainSelect plainSelect = getPlainSelect(sql);
        if (Objects.isNull(plainSelect)) {
            return new ArrayList<>();
        }
        Set<String> result = new HashSet<>();
        Expression where = plainSelect.getWhere();
        if (Objects.nonNull(where)) {
            where.accept(new FieldAcquireVisitor(result));
        }
        return new ArrayList<>(result);
    }

    public static List<String> getOrderByFields(String sql) {
        PlainSelect plainSelect = getPlainSelect(sql);
        if (Objects.isNull(plainSelect)) {
            return new ArrayList<>();
        }
        Set<String> result = new HashSet<>();
        List<OrderByElement> orderByElements = plainSelect.getOrderByElements();
        if (!CollectionUtils.isEmpty(orderByElements)) {
            for (OrderByElement orderByElement : orderByElements) {
                orderByElement.accept(new OrderByAcquireVisitor(result));
            }
        }
        return new ArrayList<>(result);
    }

    public static List<String> getSelectFields(String sql) {
        PlainSelect plainSelect = getPlainSelect(sql);
        if (Objects.isNull(plainSelect)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(getSelectFields(plainSelect));
    }

    public static Set<String> getSelectFields(PlainSelect plainSelect) {
        List<SelectItem> selectItems = plainSelect.getSelectItems();
        Set<String> result = new HashSet<>();
        for (SelectItem selectItem : selectItems) {
            selectItem.accept(new FieldAcquireVisitor(result));
        }
        return result;
    }

    public static PlainSelect getPlainSelect(String sql) {
        Select selectStatement = getSelect(sql);
        if (selectStatement == null) {
            return null;
        }
        SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            return null;
        }
        return (PlainSelect) selectBody;
    }

    public static Select getSelect(String sql) {
        Statement statement = null;
        try {
            statement = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            log.error("parse error", e);
            return null;
        }

        if (!(statement instanceof Select)) {
            return null;
        }
        return (Select) statement;
    }


    public static List<String> getAllFields(String sql) {

        PlainSelect plainSelect = getPlainSelect(sql);
        if (Objects.isNull(plainSelect)) {
            return new ArrayList<>();
        }
        Set<String> result = getSelectFields(plainSelect);

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
        List<OrderByElement> orderByElements = plainSelect.getOrderByElements();
        if (!CollectionUtils.isEmpty(orderByElements)) {
            for (OrderByElement orderByElement : orderByElements) {
                orderByElement.accept(new OrderByAcquireVisitor(result));
            }
        }

        Expression where = plainSelect.getWhere();
        if (where != null) {
            where.accept(new ExpressionVisitorAdapter() {
                @Override
                public void visit(Column column) {
                    result.add(column.getColumnName());
                }
            });
        }

        return new ArrayList<>(result);
    }

    public static String getTableName(String sql) {
        Select selectStatement = getSelect(sql);
        if (selectStatement == null) {
            return null;
        }
        SelectBody selectBody = selectStatement.getSelectBody();
        PlainSelect plainSelect = (PlainSelect) selectBody;

        Table table = (Table) plainSelect.getFromItem();
        return table.getName();
    }


    public static boolean hasAggregateFunction(String sql) {
        Select selectStatement = getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            return false;
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        List<SelectItem> selectItems = plainSelect.getSelectItems();
        AggregateFunctionVisitor visitor = new AggregateFunctionVisitor();
        for (SelectItem selectItem : selectItems) {
            selectItem.accept(visitor);
        }
        boolean selectFunction = visitor.hasAggregateFunction();
        if (selectFunction) {
            return true;
        }
        GroupByElement groupBy = plainSelect.getGroupBy();
        if (Objects.nonNull(groupBy)) {
            GroupByVisitor replaceVisitor = new GroupByVisitor();
            groupBy.accept(replaceVisitor);
            return replaceVisitor.isHasAggregateFunction();
        }
        return false;
    }

}

