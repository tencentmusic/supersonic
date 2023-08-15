package com.tencent.supersonic.common.util.jsqlparser;

import com.tencent.supersonic.common.util.DatePeriodEnum;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.StringUtil;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class ParseVisitorHelper {

    public static final double HALF_YEAR = 0.5d;
    public static final int SIX_MONTH = 6;
    public static final String DATE_FUNCTION = "datediff";

    public void replaceColumn(Column column, Map<String, String> fieldToBizName) {
        String columnName = column.getColumnName();
        column.setColumnName(getReplaceColumn(columnName, fieldToBizName));
    }

    public String getReplaceColumn(String columnName, Map<String, String> fieldToBizName) {
        String fieldBizName = fieldToBizName.get(columnName);
        if (StringUtils.isNotEmpty(fieldBizName)) {
            return fieldBizName;
        } else {
            Optional<Entry<String, String>> first = fieldToBizName.entrySet().stream().sorted((k1, k2) -> {
                String k1FieldNameDb = k1.getKey();
                String k2FieldNameDb = k2.getKey();
                Double k1Similarity = getSimilarity(columnName, k1FieldNameDb);
                Double k2Similarity = getSimilarity(columnName, k2FieldNameDb);
                return k2Similarity.compareTo(k1Similarity);
            }).collect(Collectors.toList()).stream().findFirst();

            if (first.isPresent()) {
                return first.get().getValue();
            }
        }
        return columnName;
    }

    public Expression reparseDate(ComparisonOperator comparisonOperator, Map<String, String> fieldToBizName,
            String startDateOperator) {
        Expression leftExpression = comparisonOperator.getLeftExpression();
        if (leftExpression instanceof Column) {
            Column leftExpressionColumn = (Column) leftExpression;
            replaceColumn(leftExpressionColumn, fieldToBizName);
            return null;
        }

        if (!(leftExpression instanceof Function)) {
            return null;
        }
        Function leftExpressionFunction = (Function) leftExpression;
        if (!leftExpressionFunction.toString().contains(DATE_FUNCTION)) {
            return null;
        }
        List<Expression> leftExpressions = leftExpressionFunction.getParameters().getExpressions();
        if (CollectionUtils.isEmpty(leftExpressions) || leftExpressions.size() < 3) {
            return null;
        }
        Column field = (Column) leftExpressions.get(1);
        String columnName = field.getColumnName();
        String startDateValue = getStartDateStr(comparisonOperator, leftExpressions);
        String fieldBizName = fieldToBizName.get(columnName);
        try {
            String endDateValue = getEndDateValue(leftExpressions);
            String stringExpression = comparisonOperator.getStringExpression();

            String condExpr =
                    fieldBizName + StringUtil.getSpaceWrap(stringExpression) + StringUtil.getCommaWrap(endDateValue);
            ComparisonOperator expression = (ComparisonOperator) CCJSqlParserUtil.parseCondExpression(condExpr);

            comparisonOperator.setLeftExpression(null);
            comparisonOperator.setRightExpression(null);
            comparisonOperator.setASTNode(null);

            comparisonOperator.setLeftExpression(expression.getLeftExpression());
            comparisonOperator.setRightExpression(expression.getRightExpression());
            comparisonOperator.setASTNode(expression.getASTNode());

            String startDataCondExpr =
                    fieldBizName + StringUtil.getSpaceWrap(startDateOperator) + StringUtil.getCommaWrap(startDateValue);
            return CCJSqlParserUtil.parseCondExpression(startDataCondExpr);
        } catch (JSQLParserException e) {
            log.error("JSQLParserException", e);
        }

        return null;
    }

    private String getEndDateValue(List<Expression> leftExpressions) {
        StringValue date = (StringValue) leftExpressions.get(2);
        return date.getValue();
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

    private String getUnit(List<Expression> expressions) {
        StringValue unit = (StringValue) expressions.get(0);
        return unit.getValue();
    }


    public static int editDistance(String word1, String word2) {
        final int m = word1.length();
        final int n = word2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int j = 0; j <= n; ++j) {
            dp[0][j] = j;
        }
        for (int i = 0; i <= m; ++i) {
            dp[i][0] = i;
        }

        for (int i = 1; i <= m; ++i) {
            char ci = word1.charAt(i - 1);
            for (int j = 1; j <= n; ++j) {
                char cj = word2.charAt(j - 1);
                if (ci == cj) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else if (i > 1 && j > 1 && ci == word2.charAt(j - 2) && cj == word1.charAt(i - 2)) {
                    dp[i][j] = 1 + Math.min(dp[i - 2][j - 2], Math.min(dp[i][j - 1], dp[i - 1][j]));
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1] + 1, Math.min(dp[i][j - 1] + 1, dp[i - 1][j] + 1));
                }
            }
        }
        return dp[m][n];
    }

    public double getSimilarity(String word1, String word2) {
        return 1 - (double) editDistance(word1, word2) / Math.max(word2.length(), word1.length());
    }
}