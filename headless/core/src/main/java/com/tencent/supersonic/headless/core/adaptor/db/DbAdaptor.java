package com.tencent.supersonic.headless.core.adaptor.db;

import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;

import java.sql.SQLException;
import java.util.List;

/** Adapters for different query engines to obtain table, field, and time formatting methods */
public interface DbAdaptor {

    String getDateFormat(String dateType, String dateFormat, String column);

    String functionNameCorrector(String sql);

    List<String> getDBs(ConnectInfo connectInfo) throws SQLException;

    List<String> getTables(ConnectInfo connectInfo, String schemaName) throws SQLException;

    List<DBColumn> getColumns(ConnectInfo connectInfo, String schemaName, String tableName)
            throws SQLException;
}
