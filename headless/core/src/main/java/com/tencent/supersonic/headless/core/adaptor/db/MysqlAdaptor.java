package com.tencent.supersonic.headless.core.adaptor.db;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MysqlAdaptor extends BaseDbAdaptor {

    /** transform YYYYMMDD to YYYY-MM-DD YYYY-MM YYYY-MM-DD(MONDAY) */
    @Override
    public String getDateFormat(String dateType, String dateFormat, String column) {
        if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT_INT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "DATE_FORMAT(%s, '%Y-%m')".replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "DATE_FORMAT(DATE_SUB(%s, INTERVAL (DAYOFWEEK(%s) - 2) DAY), '%Y-%m-%d')"
                        .replace("%s", column);
            } else {
                return "date_format(str_to_date(%s, '%Y%m%d'),'%Y-%m-%d')".replace("%s", column);
            }
        } else if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "DATE_FORMAT(%s, '%Y-%m') ".replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "DATE_FORMAT(DATE_SUB(%s, INTERVAL (DAYOFWEEK(%s) - 2) DAY), '%Y-%m-%d')"
                        .replace("%s", column);
            } else {
                return column;
            }
        }
        return column;
    }

    @Override
    public String rewriteSql(String sql) {
        return sql;
    }

    /**
     * MySQL UPSERT using INSERT ... ON DUPLICATE KEY UPDATE syntax.
     */
    @Override
    public String buildUpsertSql(String tableName, List<String> columns, List<String> primaryKeys) {
        if (primaryKeys == null || primaryKeys.isEmpty()) {
            // No primary keys - fall back to simple INSERT
            return super.buildUpsertSql(tableName, columns, primaryKeys);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(tableName).append(" (");
        sb.append(String.join(", ", columns));
        sb.append(") VALUES (");
        sb.append(String.join(", ", columns.stream().map(c -> "?").toList()));
        sb.append(") ON DUPLICATE KEY UPDATE ");

        // Update all non-primary-key columns
        List<String> updateCols = columns.stream().filter(col -> !primaryKeys.contains(col))
                .collect(Collectors.toList());

        if (updateCols.isEmpty()) {
            // All columns are primary keys - just set one to itself to avoid error
            sb.append(primaryKeys.get(0)).append(" = VALUES(").append(primaryKeys.get(0))
                    .append(")");
        } else {
            sb.append(updateCols.stream().map(col -> col + " = VALUES(" + col + ")")
                    .collect(Collectors.joining(", ")));
        }

        return sb.toString();
    }

    /**
     * Parse MySQL EXPLAIN output for row count estimate. MySQL EXPLAIN returns rows in a column
     * named "rows".
     */
    @Override
    public long parseExplainRowCount(List<String> explainResult) {
        if (explainResult == null || explainResult.isEmpty()) {
            return -1L;
        }
        // MySQL EXPLAIN output typically has "rows" as a field
        // For simple queries, we look for numeric values
        for (String line : explainResult) {
            // Try to find "rows" field in JSON or tabular format
            Pattern rowsPattern =
                    Pattern.compile("\"?rows\"?\\s*[=:]\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = rowsPattern.matcher(line);
            if (matcher.find()) {
                try {
                    return Long.parseLong(matcher.group(1));
                } catch (NumberFormatException e) {
                    // Continue searching
                }
            }
        }
        return -1L;
    }
}
