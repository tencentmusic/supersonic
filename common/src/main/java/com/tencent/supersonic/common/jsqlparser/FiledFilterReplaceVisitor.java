package com.tencent.supersonic.common.jsqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;

@Slf4j
public class FiledFilterReplaceVisitor extends ExpressionVisitorAdapter {

    private List<Expression> waitingForAdds = new ArrayList<>();
    private Set<String> fieldNames;
    private Map<String, String> fieldNameMap = new HashMap<>();

    private static Set<String> HAVING_AGG_TYPES = Set.of("SUM", "AVG", "MAX", "MIN", "COUNT");

    public FiledFilterReplaceVisitor(Map<String, String> fieldNameMap) {
        this.fieldNameMap = fieldNameMap;
        this.fieldNames = fieldNameMap.keySet();
    }

    public FiledFilterReplaceVisitor(Set<String> fieldNames) {
        this.fieldNames = fieldNames;
    }

    @Override
    public void visit(MinorThan expr) {
        List<Expression> expressions = parserFilter(expr, JsqlConstants.MINOR_THAN_CONSTANT);
        if (Objects.nonNull(expressions)) {
            waitingForAdds.addAll(expressions);
        }
    }

    @Override
    public void visit(EqualsTo expr) {
        List<Expression> expressions = parserFilter(expr, JsqlConstants.EQUAL_CONSTANT);
        if (Objects.nonNull(expressions)) {
            waitingForAdds.addAll(expressions);
        }
    }

    @Override
    public void visit(MinorThanEquals expr) {
        List<Expression> expressions = parserFilter(expr, JsqlConstants.MINOR_THAN_EQUALS_CONSTANT);
        if (Objects.nonNull(expressions)) {
            waitingForAdds.addAll(expressions);
        }
    }

    @Override
    public void visit(GreaterThan expr) {
        List<Expression> expressions = parserFilter(expr, JsqlConstants.GREATER_THAN_CONSTANT);
        if (Objects.nonNull(expressions)) {
            waitingForAdds.addAll(expressions);
        }
    }

    @Override
    public void visit(GreaterThanEquals expr) {
        List<Expression> expressions =
                parserFilter(expr, JsqlConstants.GREATER_THAN_EQUALS_CONSTANT);
        if (Objects.nonNull(expressions)) {
            waitingForAdds.addAll(expressions);
        }
    }

    public List<Expression> getWaitingForAdds() {
        return waitingForAdds;
    }

    public List<Expression> parserFilter(ComparisonOperator comparisonOperator, String condExpr) {
        List<Expression> result = new ArrayList<>();
        String comparisonOperatorStr = comparisonOperator.toString();
        Expression leftExpression = comparisonOperator.getLeftExpression();

        if (!(leftExpression instanceof Function)) {
            if (leftExpression instanceof Column) {
                Column leftColumn = (Column) leftExpression;
                String agg = fieldNameMap.get(leftColumn.getColumnName());
                if (agg != null && HAVING_AGG_TYPES.contains(agg.toUpperCase())) {
                    Expression expression = parseCondExpression(comparisonOperator, condExpr);
                    if (Objects.nonNull(expression)) {
                        result.add(expression);
                        return result;
                    } else {
                        return null;
                    }
                }
                return result;
            } else {
                return result;
            }
        }

        Function leftFunction = (Function) leftExpression;
        if (leftFunction.toString().contains(JsqlConstants.DATE_FUNCTION)) {
            return result;
        }

        ExpressionList<?> leftFunctionParams = leftFunction.getParameters();
        if (CollectionUtils.isEmpty(leftFunctionParams)
                || !(leftFunctionParams.get(0) instanceof Column)) {
            return result;
        }

        Column field = (Column) leftFunctionParams.get(0);
        String columnName = field.getColumnName();
        if (!fieldNames.contains(columnName)) {
            return null;
        }

        Expression expression = parseCondExpression(comparisonOperator, condExpr);
        if (Objects.nonNull(expression)) {
            result.add(expression);
            return result;
        } else {
            return null;
        }
    }

    private Expression parseCondExpression(ComparisonOperator comparisonOperator, String condExpr) {
        try {
            String comparisonOperatorStr = comparisonOperator.toString();
            ComparisonOperator parsedExpression =
                    (ComparisonOperator) CCJSqlParserUtil.parseCondExpression(condExpr);
            comparisonOperator.setLeftExpression(parsedExpression.getLeftExpression());
            comparisonOperator.setRightExpression(parsedExpression.getRightExpression());
            comparisonOperator.setASTNode(parsedExpression.getASTNode());
            return CCJSqlParserUtil.parseCondExpression(comparisonOperatorStr);
        } catch (JSQLParserException e) {
            log.error("JSQLParserException", e);
        }
        return null;
    }
}
