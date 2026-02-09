package com.tencent.supersonic.headless.core.adaptor.db;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.enums.FieldType;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class PostgresqlAdaptor extends BaseDbAdaptor {

    @Override
    public String getDateFormat(String dateType, String dateFormat, String column) {
        if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT_INT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "to_char(to_date(%s,'yyyymmdd'), 'yyyy-mm')".replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "to_char(date_trunc('week',to_date(%s, 'yyyymmdd')),'yyyy-mm-dd')"
                        .replace("%s", column);
            } else {
                return "to_char(to_date(%s,'yyyymmdd'), 'yyyy-mm-dd')".replace("%s", column);
            }
        } else if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "to_char(to_date(%s,'yyyy-mm-dd'), 'yyyy-mm')".replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "to_char(date_trunc('week',to_date(%s, 'yyyy-mm-dd')),'yyyy-mm-dd')"
                        .replace("%s", column);
            } else {
                return column;
            }
        }
        return column;
    }

    @Override
    public String rewriteSql(String sql) {
        Map<String, String> functionMap = new HashMap<>();
        functionMap.put("MONTH".toLowerCase(), "TO_CHAR");
        functionMap.put("DAY".toLowerCase(), "TO_CHAR");
        functionMap.put("YEAR".toLowerCase(), "TO_CHAR");
        Map<String, UnaryOperator> functionCall = new HashMap<>();
        functionCall.put("MONTH".toLowerCase(), o -> {
            if (Objects.nonNull(o) && o instanceof ExpressionList) {
                ExpressionList expressionList = (ExpressionList) o;
                expressionList.add(new StringValue("MM"));
                return expressionList;
            }
            return o;
        });
        functionCall.put("DAY".toLowerCase(), o -> {
            if (Objects.nonNull(o) && o instanceof ExpressionList) {
                ExpressionList expressionList = (ExpressionList) o;
                expressionList.add(new StringValue("dd"));
                return expressionList;
            }
            return o;
        });
        functionCall.put("YEAR".toLowerCase(), o -> {
            if (Objects.nonNull(o) && o instanceof ExpressionList) {
                ExpressionList expressionList = (ExpressionList) o;
                expressionList.add(new StringValue("YYYY"));
                return expressionList;
            }
            return o;
        });
        sql = SqlReplaceHelper.replaceFunction(sql, functionMap, functionCall);
        sql = sql.replaceAll("`", "\"");
        return sql;
    }

    public List<String> getTables(ConnectInfo connectionInfo, String schemaName)
            throws SQLException {
        List<String> tablesAndViews = Lists.newArrayList();
        DatabaseMetaData metaData = getDatabaseMetaData(connectionInfo);
        try (ResultSet resultSet =
                metaData.getTables(null, schemaName, null, new String[] {"TABLE", "VIEW"})) {
            while (resultSet.next()) {
                String name = resultSet.getString("TABLE_NAME");
                tablesAndViews.add(name);
            }
        } catch (SQLException e) {
            log.error("Failed to get tables and views", e);
        }
        return tablesAndViews;
    }

    public List<DBColumn> getColumns(ConnectInfo connectInfo, String catalog, String schemaName,
            String tableName) throws SQLException {
        List<DBColumn> dbColumns = Lists.newArrayList();
        DatabaseMetaData metaData = getDatabaseMetaData(connectInfo);
        ResultSet columns = metaData.getColumns(null, schemaName, tableName, null);
        while (columns.next()) {
            String columnName = columns.getString("COLUMN_NAME");
            String dataType = columns.getString("TYPE_NAME");
            String remarks = columns.getString("REMARKS");
            FieldType fieldType = classifyColumnType(dataType);
            dbColumns.add(new DBColumn(columnName, dataType, remarks, fieldType));
        }
        return dbColumns;
    }

    @Override
    public FieldType classifyColumnType(String typeName) {
        switch (typeName.toUpperCase()) {
            case "INT":
            case "INTEGER":
            case "BIGINT":
            case "SMALLINT":
            case "SERIAL":
            case "BIGSERIAL":
            case "SMALLSERIAL":
            case "REAL":
            case "DOUBLE PRECISION":
            case "NUMERIC":
            case "DECIMAL":
                return FieldType.measure;
            case "DATE":
            case "TIME":
            case "TIMESTAMP":
            case "TIMESTAMPTZ":
            case "INTERVAL":
                return FieldType.time;
            case "VARCHAR":
            case "CHAR":
            case "TEXT":
            case "CHARACTER VARYING":
            case "CHARACTER":
            case "UUID":
            default:
                return FieldType.categorical;
        }
    }

    /**
     * PostgreSQL UPSERT using INSERT ... ON CONFLICT DO UPDATE syntax.
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
        sb.append(") ON CONFLICT (");
        sb.append(String.join(", ", primaryKeys));
        sb.append(") DO UPDATE SET ");

        // Update all non-primary-key columns
        List<String> updateCols = columns.stream().filter(col -> !primaryKeys.contains(col))
                .collect(Collectors.toList());

        if (updateCols.isEmpty()) {
            // All columns are primary keys - use DO NOTHING instead
            return sb.toString().replace(") DO UPDATE SET ", ") DO NOTHING");
        }

        sb.append(updateCols.stream().map(col -> col + " = EXCLUDED." + col)
                .collect(Collectors.joining(", ")));

        return sb.toString();
    }

    /**
     * Parse PostgreSQL EXPLAIN output for row count estimate. PostgreSQL EXPLAIN shows "rows=N" in
     * the plan output.
     */
    @Override
    public long parseExplainRowCount(List<String> explainResult) {
        if (explainResult == null || explainResult.isEmpty()) {
            return -1L;
        }
        // PostgreSQL EXPLAIN shows "rows=N" in its output
        Pattern rowsPattern = Pattern.compile("rows=(\\d+)", Pattern.CASE_INSENSITIVE);
        long totalRows = 0;
        boolean found = false;

        for (String line : explainResult) {
            Matcher matcher = rowsPattern.matcher(line);
            // Take the first rows= value (top-level plan node)
            if (matcher.find() && !found) {
                try {
                    totalRows = Long.parseLong(matcher.group(1));
                    found = true;
                } catch (NumberFormatException e) {
                    // Continue searching
                }
            }
        }
        return found ? totalRows : -1L;
    }
}
