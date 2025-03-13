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
}
