package com.tencent.supersonic.common.util.jsqlparser;

import com.tencent.supersonic.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;

import java.util.List;

//mainly deal with datediff in s2sql

@Slf4j
public class SqlDateDiffBase extends SqlEditBase {
    public Expression distinguishFilter(Expression leftExpression, Expression expression) throws Exception {
        if (leftExpression instanceof Function) {
            Function function = (Function) leftExpression;
            if (function.getName().equals(JsqlConstants.DATE_FUNCTION)) {
                ComparisonOperator comparisonOperator = (ComparisonOperator) expression;
                List<Expression> leftExpressions = function.getParameters().getExpressions();
                Column field = (Column) function.getParameters().getExpressions().get(1);
                String columnName = field.getColumnName();
                try {
                    String startDateValue = DateFunctionHelper.getStartDateStr(comparisonOperator, leftExpressions);
                    String endDateValue = DateFunctionHelper.getEndDateValue(leftExpressions);
                    String dateOperator = comparisonOperator.getStringExpression();
                    String endDateOperator = JsqlConstants.rightMap.get(dateOperator);
                    String startDateOperator = JsqlConstants.leftMap.get(dateOperator);

                    String endDateCondExpr = columnName + endDateOperator + StringUtil.getCommaWrap(endDateValue);
                    ComparisonOperator rightExpression = (ComparisonOperator)
                            CCJSqlParserUtil.parseCondExpression(endDateCondExpr);

                    String startDateCondExpr = columnName + StringUtil.getSpaceWrap(startDateOperator)
                            + StringUtil.getCommaWrap(startDateValue);
                    ComparisonOperator newLeftExpression = (ComparisonOperator)
                            CCJSqlParserUtil.parseCondExpression(startDateCondExpr);

                    AndExpression andExpression = new AndExpression(newLeftExpression, rightExpression);
                    if (JsqlConstants.GREATER_THAN.equals(dateOperator)
                            || JsqlConstants.GREATER_THAN_EQUALS.equals(dateOperator)) {
                        return newLeftExpression;
                    } else {
                        return CCJSqlParserUtil.parseCondExpression("(" + andExpression.toString() + ")");
                    }
                } catch (JSQLParserException e) {
                    log.error("JSQLParserException", e);
                }
            }
            return expression;
        } else {
            return expression;
        }
    }
}
