package com.tencent.supersonic.headless.core.adaptor.db;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.enums.FieldType;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import lombok.extern.slf4j.Slf4j;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class H2Adaptor extends BaseDbAdaptor {

    @Override
    public String getDateFormat(String dateType, String dateFormat, String column) {
        if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT_INT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "FORMATDATETIME(PARSEDATETIME(%s, 'yyyyMMdd'),'yyyy-MM')".replace("%s",
                        column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "DATE_TRUNC('week',%s)".replace("%s", column);
            } else {
                return "FORMATDATETIME(PARSEDATETIME(%s, 'yyyyMMdd'),'yyyy-MM-dd')".replace("%s",
                        column);
            }
        } else if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "FORMATDATETIME(PARSEDATETIME(%s, 'yyyy-MM-dd'),'yyyy-MM') ".replace("%s",
                        column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "DATE_TRUNC('week',%s)".replace("%s", column);
            } else {
                return column;
            }
        }
        return column;
    }

    protected ResultSet getResultSet(String schemaName, DatabaseMetaData metaData)
            throws SQLException {
        return metaData.getTables(schemaName, null, null, new String[] {"TABLE", "VIEW"});
    }

    public List<DBColumn> getColumns(ConnectInfo connectInfo, String catalog, String schemaName,
            String tableName) throws SQLException {
        List<DBColumn> dbColumns = Lists.newArrayList();
        DatabaseMetaData metaData = getDatabaseMetaData(connectInfo);
        ResultSet columns = metaData.getColumns(schemaName, null, tableName, null);
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
    public String rewriteSql(String sql) {
        return sql;
    }

    /**
     * H2 UPSERT using MERGE INTO syntax.
     */
    @Override
    public String buildUpsertSql(String tableName, List<String> columns, List<String> primaryKeys) {
        if (primaryKeys == null || primaryKeys.isEmpty()) {
            // No primary keys - fall back to simple INSERT
            return super.buildUpsertSql(tableName, columns, primaryKeys);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("MERGE INTO ").append(tableName).append(" (");
        sb.append(String.join(", ", columns));
        sb.append(") KEY (");
        sb.append(String.join(", ", primaryKeys));
        sb.append(") VALUES (");
        sb.append(String.join(", ", columns.stream().map(c -> "?").toList()));
        sb.append(")");

        return sb.toString();
    }

    /**
     * Parse H2 EXPLAIN output for row count estimate. H2 EXPLAIN PLAN shows scanCount in its
     * output.
     */
    @Override
    public long parseExplainRowCount(List<String> explainResult) {
        if (explainResult == null || explainResult.isEmpty()) {
            return -1L;
        }
        // H2 EXPLAIN shows "scanCount: N" or row estimates in its plan
        Pattern scanPattern = Pattern.compile("scanCount:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Pattern rowsPattern = Pattern.compile("rows:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

        for (String line : explainResult) {
            Matcher matcher = scanPattern.matcher(line);
            if (matcher.find()) {
                try {
                    return Long.parseLong(matcher.group(1));
                } catch (NumberFormatException e) {
                    // Continue searching
                }
            }
            matcher = rowsPattern.matcher(line);
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
