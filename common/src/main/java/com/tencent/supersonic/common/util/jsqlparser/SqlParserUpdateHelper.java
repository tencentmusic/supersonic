package com.tencent.supersonic.common.util.jsqlparser;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
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

    public static String replaceFields(String sql, Map<String, String> fieldToBizName) {
        return replaceFields(sql, fieldToBizName, false);
    }

    public static String replaceFields(String sql, Map<String, String> fieldToBizName, boolean exactReplace) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return sql;
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        //1. replace where fields
        Expression where = plainSelect.getWhere();
        FieldReplaceVisitor visitor = new FieldReplaceVisitor(fieldToBizName, exactReplace);
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
                orderByElement.accept(new OrderByReplaceVisitor(fieldToBizName, exactReplace));
            }
        }

        //4. replace group by fields
        GroupByElement groupByElement = plainSelect.getGroupBy();
        if (Objects.nonNull(groupByElement)) {
            groupByElement.accept(new GroupByReplaceVisitor(fieldToBizName, exactReplace));
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

}

