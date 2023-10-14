package com.tencent.supersonic.common.util.jsqlparser;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

/**
 * Sql Parser Select function Helper
 */
@Slf4j
public class SqlParserSelectFunctionHelper {

    public static boolean hasAggregateFunction(String sql) {
        if (!CollectionUtils.isEmpty(getFunctions(sql))) {
            return true;
        }
        return SqlParserSelectHelper.hasGroupBy(sql);
    }

    public static boolean hasFunction(String sql, String functionName) {
        Set<String> functions = getFunctions(sql);
        if (!CollectionUtils.isEmpty(functions)) {
            return functions.stream().anyMatch(function -> function.equalsIgnoreCase(functionName));
        }
        return false;
    }

    public static Set<String> getFunctions(String sql) {
        Select selectStatement = SqlParserSelectHelper.getSelect(sql);
        SelectBody selectBody = selectStatement.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            return new HashSet<>();
        }
        PlainSelect plainSelect = (PlainSelect) selectBody;
        List<SelectItem> selectItems = plainSelect.getSelectItems();
        FunctionVisitor visitor = new FunctionVisitor();
        for (SelectItem selectItem : selectItems) {
            selectItem.accept(visitor);
        }
        return visitor.getFunctionNames();
    }

    public static Function getFunction(Expression expression, Map<String, String> fieldNameToAggregate) {
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

    public static Function getFunction(Expression expression, String aggregateName) {
        if (StringUtils.isEmpty(aggregateName)) {
            return null;
        }
        Function sumFunction = new Function();
        sumFunction.setName(aggregateName);
        sumFunction.setParameters(new ExpressionList(expression));
        return sumFunction;
    }

}

