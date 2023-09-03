package com.tencent.supersonic.common.util.jsqlparser;

import com.tencent.supersonic.common.util.DatePeriodEnum;
import com.tencent.supersonic.common.util.DateUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;

@Slf4j
public class DateFunctionHelper {

    public static final String DATE_FUNCTION = "datediff";
    public static final double HALF_YEAR = 0.5d;
    public static final int SIX_MONTH = 6;
    public static final String EQUAL = "=";

    public static String getStartDateStr(ComparisonOperator minorThanEquals, List<Expression> expressions) {
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

    public static String getEndDateOperator(ComparisonOperator comparisonOperator) {
        String operator = comparisonOperator.getStringExpression();
        if (EQUAL.equalsIgnoreCase(operator)) {
            operator = "<=";
        }
        return operator;
    }

    public static String getEndDateValue(List<Expression> leftExpressions) {
        StringValue date = (StringValue) leftExpressions.get(2);
        return date.getValue();
    }

    private static String getUnit(List<Expression> expressions) {
        StringValue unit = (StringValue) expressions.get(0);
        return unit.getValue();
    }


}