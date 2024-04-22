package com.tencent.supersonic.common.util.jsqlparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

@Slf4j
public class FiledFilterReplaceVisitor extends ExpressionVisitorAdapter {

    private List<Expression> waitingForAdds = new ArrayList<>();
    private Set<String> fieldNames;

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
        List<Expression> expressions = parserFilter(expr, JsqlConstants.GREATER_THAN_EQUALS_CONSTANT);
        if (Objects.nonNull(expressions)) {
            waitingForAdds.addAll(expressions);
        }
    }

    public List<Expression> getWaitingForAdds() {
        return waitingForAdds;
    }

    public List<Expression> parserFilter(ComparisonOperator comparisonOperator, String condExpr) {
        List<Expression> result = new ArrayList<>();
        String toString = comparisonOperator.toString();
        Expression leftExpression = comparisonOperator.getLeftExpression();
        if (!(leftExpression instanceof Function)) {
            return result;
        }
        Function leftExpressionFunction = (Function) leftExpression;
        if (leftExpressionFunction.toString().contains(JsqlConstants.DATE_FUNCTION)) {
            return result;
        }

        //List<Expression> leftExpressions = leftExpressionFunction.getParameters().getExpressions();
        ExpressionList<?> leftExpressions = leftExpressionFunction.getParameters();
        if (CollectionUtils.isEmpty(leftExpressions)) {
            return result;
        }
        Column field = (Column) leftExpressions.get(0);
        String columnName = field.getColumnName();
        if (!fieldNames.contains(columnName)) {
            return null;
        }
        try {
            ComparisonOperator expression = (ComparisonOperator) CCJSqlParserUtil.parseCondExpression(condExpr);
            comparisonOperator.setLeftExpression(expression.getLeftExpression());
            comparisonOperator.setRightExpression(expression.getRightExpression());
            comparisonOperator.setASTNode(expression.getASTNode());
            result.add(CCJSqlParserUtil.parseCondExpression(toString));
            return result;
        } catch (JSQLParserException e) {
            log.error("JSQLParserException", e);
        }
        return null;
    }

}
