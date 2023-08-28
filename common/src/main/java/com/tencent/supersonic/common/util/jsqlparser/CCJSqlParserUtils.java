package com.tencent.supersonic.common.util.jsqlparser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
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
import net.sf.jsqlparser.util.SelectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

/**
 * CC JSql ParserUtils
 */
@Slf4j
public class CCJSqlParserUtils {

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

    public static List<String> getSelectFields(String sql) {
        PlainSelect plainSelect = getPlainSelect(sql);
        if (Objects.isNull(plainSelect)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(getSelectFields(plainSelect));
    }

    private static Set<String> getSelectFields(PlainSelect plainSelect) {
        List<SelectItem> selectItems = plainSelect.getSelectItems();
        Set<String> result = new HashSet<>();
        for (SelectItem selectItem : selectItems) {
            selectItem.accept(new FieldAcquireVisitor(result));
        }
        return result;
    }

    private static PlainSelect getPlainSelect(String sql) {
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

    private static Select getSelect(String sql) {
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
        if (orderByElements != null) {
            for (OrderByElement orderByElement : orderByElements) {
                Expression expression = orderByElement.getExpression();

                if (expression instanceof Column) {
                    Column column = (Column) expression;
                    result.add(column.getColumnName());
                }
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


    public static String replaceFields(String sql, Map<String, String> fieldToBizName) {
        Select selectStatement = getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        //1. replace where fields
        Expression where = plainSelect.getWhere();
        FieldReplaceVisitor visitor = new FieldReplaceVisitor(fieldToBizName);
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
                orderByElement.accept(new OrderByReplaceVisitor(fieldToBizName));
            }
        }

        //4. replace group by fields
        GroupByElement groupByElement = plainSelect.getGroupBy();
        if (Objects.nonNull(groupByElement)) {
            groupByElement.accept(new GroupByReplaceVisitor(fieldToBizName));
        }
        return selectStatement.toString();
    }


    public static String replaceFunction(String sql) {
        Select selectStatement = getSelect(sql);
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
        Select selectStatement = getSelect(sql);
        // add fields to select
        for (String field : fields) {
            SelectUtils.addExpression(selectStatement, new Column(field));
        }
        return selectStatement.toString();
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

    public static String replaceTable(String sql, String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            return sql;
        }
        Select selectStatement = getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        PlainSelect plainSelect = (PlainSelect) selectBody;
        // replace table name
        Table table = (Table) plainSelect.getFromItem();
        table.setName(tableName);
        return selectStatement.toString();
    }


    public static String addWhere(String sql, String column, Object value) {
        if (StringUtils.isEmpty(column) || Objects.isNull(value)) {
            return sql;
        }
        Select selectStatement = getSelect(sql);
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
        Select selectStatement = getSelect(sql);
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
        return visitor.hasAggregateFunction();
    }


}

