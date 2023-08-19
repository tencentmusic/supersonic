package com.tencent.supersonic.common.util.jsqlparser;

import com.tencent.supersonic.common.util.DatePeriodEnum;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
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

    public static final String DATE_FUNCTION = "datediff";
    public static final double HALF_YEAR = 0.5d;
    public static final int SIX_MONTH = 6;
    public static final String EQUAL = "=";
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
        if (!leftExpressionFunction.toString().contains(DATE_FUNCTION)) {
            return result;
        }
        List<Expression> leftExpressions = leftExpressionFunction.getParameters().getExpressions();
        if (CollectionUtils.isEmpty(leftExpressions) || leftExpressions.size() < 3) {
            return result;
        }
        Column field = (Column) leftExpressions.get(1);
        String columnName = field.getColumnName();
        try {
            String startDateValue = getStartDateStr(comparisonOperator, leftExpressions);
            String endDateValue = getEndDateValue(leftExpressions);
            String endDateOperator = comparisonOperator.getStringExpression();
            String condExpr =
                    columnName + StringUtil.getSpaceWrap(getEndDateOperator(comparisonOperator))
                            + StringUtil.getCommaWrap(endDateValue);
            ComparisonOperator expression = (ComparisonOperator) CCJSqlParserUtil.parseCondExpression(condExpr);

            String startDataCondExpr =
                    columnName + StringUtil.getSpaceWrap(startDateOperator) + StringUtil.getCommaWrap(startDateValue);

            if (EQUAL.equalsIgnoreCase(endDateOperator)) {
                result.add(CCJSqlParserUtil.parseCondExpression(condExpr));
                expression = (ComparisonOperator) CCJSqlParserUtil.parseCondExpression(" 1 = 1 ");
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

    private String getStartDateStr(ComparisonOperator minorThanEquals, List<Expression> expressions) {
        String unitValue = getUnit(expressions);
        String dateValue = getEndDateValue(expressions);
        String dateStr = "";
        Expression rightExpression = minorThanEquals.getRightExpression();
        DatePeriodEnum datePeriodEnum = DatePeriodEnum.get(unitValue);
        if (rightExpression instanceof DoubleValue) {
            DoubleValue value = (DoubleValue) rightExpression;
            double doubleValue = value.getValue();
            if (DatePeriodEnum.YEAR.equals(datePeriodEnum) && doubleValue == HALF_YEAR) {
                datePeriodEnum = DatePeriodEnum.MONTH;
                dateStr = DateUtils.getBeforeDate(dateValue, SIX_MONTH, datePeriodEnum);
            }
        } else if (rightExpression instanceof LongValue) {
            LongValue value = (LongValue) rightExpression;
            long doubleValue = value.getValue();
            dateStr = DateUtils.getBeforeDate(dateValue, (int) doubleValue, datePeriodEnum);
        }
        return dateStr;
    }

    private String getEndDateOperator(ComparisonOperator comparisonOperator) {
        String operator = comparisonOperator.getStringExpression();
        if (EQUAL.equalsIgnoreCase(operator)) {
            operator = "<=";
        }
        return operator;
    }

    private String getEndDateValue(List<Expression> leftExpressions) {
        StringValue date = (StringValue) leftExpressions.get(2);
        return date.getValue();
    }

    private String getUnit(List<Expression> expressions) {
        StringValue unit = (StringValue) expressions.get(0);
        return unit.getValue();
    }


}