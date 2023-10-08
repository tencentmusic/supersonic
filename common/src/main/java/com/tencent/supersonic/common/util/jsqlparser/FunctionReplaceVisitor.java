package com.tencent.supersonic.common.util.jsqlparser;

import com.tencent.supersonic.common.util.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.collections.CollectionUtils;

@Slf4j
public class FunctionReplaceVisitor extends ExpressionVisitorAdapter {

    private List<Expression> waitingForAdds = new ArrayList<>();

    @Override
    public void visit(MinorThan expr) {
        List<Expression> expressions = reparseDate(expr, ">");
        if (Objects.nonNull(expressions)) {
            waitingForAdds.addAll(expressions);
        }
    }

    @Override
    public void visit(EqualsTo expr) {
        List<Expression> expressions = reparseDate(expr, ">=");
        if (Objects.nonNull(expressions)) {
            waitingForAdds.addAll(expressions);
        }
    }

    @Override
    public void visit(MinorThanEquals expr) {
        List<Expression> expressions = reparseDate(expr, ">=");
        if (Objects.nonNull(expressions)) {
            waitingForAdds.addAll(expressions);
        }
    }


    @Override
    public void visit(GreaterThan expr) {
        List<Expression> expressions = reparseDate(expr, "<");
        if (Objects.nonNull(expressions)) {
            waitingForAdds.addAll(expressions);
        }
    }

    @Override
    public void visit(GreaterThanEquals expr) {
        List<Expression> expressions = reparseDate(expr, "<=");
        if (Objects.nonNull(expressions)) {
            waitingForAdds.addAll(expressions);
        }
    }

    public List<Expression> getWaitingForAdds() {
        return waitingForAdds;
    }


    public List<Expression> reparseDate(ComparisonOperator comparisonOperator, String startDateOperator) {
        List<Expression> result = new ArrayList<>();
        Expression leftExpression = comparisonOperator.getLeftExpression();
        if (!(leftExpression instanceof Function)) {
            return result;
        }
        Function leftExpressionFunction = (Function) leftExpression;
        if (!leftExpressionFunction.toString().contains(JsqlConstants.DATE_FUNCTION)) {
            return result;
        }
        List<Expression> leftExpressions = leftExpressionFunction.getParameters().getExpressions();
        if (CollectionUtils.isEmpty(leftExpressions) || leftExpressions.size() < 3) {
            return result;
        }
        Column field = (Column) leftExpressions.get(1);
        String columnName = field.getColumnName();
        try {
            String startDateValue = DateFunctionHelper.getStartDateStr(comparisonOperator, leftExpressions);
            String endDateValue = DateFunctionHelper.getEndDateValue(leftExpressions);
            String endDateOperator = comparisonOperator.getStringExpression();
            String condExpr =
                    columnName + StringUtil.getSpaceWrap(DateFunctionHelper.getEndDateOperator(comparisonOperator))
                            + StringUtil.getCommaWrap(endDateValue);
            ComparisonOperator expression = (ComparisonOperator) CCJSqlParserUtil.parseCondExpression(condExpr);

            String startDataCondExpr =
                    columnName + StringUtil.getSpaceWrap(startDateOperator) + StringUtil.getCommaWrap(startDateValue);

            if (JsqlConstants.EQUAL.equalsIgnoreCase(endDateOperator)) {
                result.add(CCJSqlParserUtil.parseCondExpression(condExpr));
                expression = (ComparisonOperator) CCJSqlParserUtil.parseCondExpression(JsqlConstants.EQUAL_CONSTANT);
            }
            comparisonOperator.setLeftExpression(null);
            comparisonOperator.setRightExpression(null);
            comparisonOperator.setASTNode(null);

            comparisonOperator.setLeftExpression(expression.getLeftExpression());
            comparisonOperator.setRightExpression(expression.getRightExpression());
            comparisonOperator.setASTNode(expression.getASTNode());

            result.add(CCJSqlParserUtil.parseCondExpression(startDataCondExpr));
            return result;
        } catch (JSQLParserException e) {
            log.error("JSQLParserException", e);
        }

        return null;
    }

}