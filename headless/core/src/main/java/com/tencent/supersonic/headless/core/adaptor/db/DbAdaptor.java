package com.tencent.supersonic.headless.core.adaptor.db;

import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.enums.FieldType;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;

import java.sql.SQLException;
import java.util.List;

/** Adapters for different query engines to obtain table, field, and time formatting methods */
public interface DbAdaptor {

    String getDateFormat(String dateType, String dateFormat, String column);

    String rewriteSql(String sql);

    List<String> getCatalogs(ConnectInfo connectInfo) throws SQLException;

    List<String> getDBs(ConnectInfo connectInfo, String catalog) throws SQLException;

    List<String> getTables(ConnectInfo connectInfo, String catalog, String schemaName)
            throws SQLException;

    List<DBColumn> getColumns(ConnectInfo connectInfo, String catalog, String schemaName,
            String tableName) throws SQLException;

    FieldType classifyColumnType(String typeName);

    /**
     * Build an UPSERT (INSERT ... ON CONFLICT UPDATE) SQL statement for the given table, columns
     * and primary keys. This handles duplicates by updating existing rows instead of failing.
     *
     * @param tableName the target table name
     * @param columns list of column names to insert/update
     * @param primaryKeys list of primary key column names for conflict detection
     * @return the database-specific UPSERT SQL with placeholders (?)
     */
    String buildUpsertSql(String tableName, List<String> columns, List<String> primaryKeys);

    /**
     * Parse row count estimate from EXPLAIN output. Returns -1 if estimation is not possible.
     *
     * @param explainResult the result rows from EXPLAIN query
     * @return estimated row count, or -1 if unknown
     */
    long parseExplainRowCount(List<String> explainResult);
}
