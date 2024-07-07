package com.tencent.supersonic.common.jsqlparser;

import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

/**
 * Sql Parser Select function Helper
 */
@Slf4j
public class SqlSelectFunctionHelper {

    public static List<String> aggregateFunctionName = Arrays.asList("SUM", "COUNT", "MAX", "MIN", "AVG");

    public static boolean hasAggregateFunction(String sql) {
        if (!CollectionUtils.isEmpty(getFunctions(sql))) {
            return true;
        }
        return SqlSelectHelper.hasGroupBy(sql);
    }

    public static boolean hasFunction(String sql, String functionName) {
        Set<String> functions = getFunctions(sql);
        if (!CollectionUtils.isEmpty(functions)) {
            return functions.stream().anyMatch(function -> function.equalsIgnoreCase(functionName));
        }
        return false;
    }

    public static Set<String> getFunctions(String sql) {
        Select selectStatement = SqlSelectHelper.getSelect(sql);
        if (!(selectStatement instanceof PlainSelect)) {
            return new HashSet<>();
        }
        PlainSelect plainSelect = (PlainSelect) selectStatement;
        List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
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
        if (AggOperatorEnum.isCountDistinct(aggregateName)) {
            sumFunction.setName("count");
            sumFunction.setDistinct(true);
        } else {
            sumFunction.setName(aggregateName);
        }
        sumFunction.setParameters(new ExpressionList(expression));
        return sumFunction;
    }

    public static String getFirstAggregateFunctions(String expr) {
        List<String> functions = getAggregateFunctions(expr);
        return CollectionUtils.isEmpty(functions) ? "" : functions.get(0);
    }

    public static List<String> getAggregateFunctions(String expr) {
        Expression expression = QueryExpressionReplaceVisitor.getExpression(expr);
        if (Objects.nonNull(expression)) {
            FunctionVisitor visitor = new FunctionVisitor();
            expression.accept(visitor);
            Set<String> functions = visitor.getFunctionNames();
            return functions.stream()
                    .filter(t -> aggregateFunctionName.contains(t.toUpperCase())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public static boolean hasAsterisk(String sql) {
        List<PlainSelect> plainSelectList = SqlSelectHelper.getPlainSelect(sql);
        if (CollectionUtils.isEmpty(plainSelectList)) {
            return false;
        }
        for (PlainSelect plainSelect : plainSelectList) {
            List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
            if (selectItems.stream().anyMatch(item -> item.getExpression() instanceof AllColumns)) {
                return true;
            }
        }
        return false;
    }
}

