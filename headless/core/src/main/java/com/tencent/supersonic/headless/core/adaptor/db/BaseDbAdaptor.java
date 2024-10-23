package com.tencent.supersonic.headless.core.adaptor.db;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class BaseDbAdaptor implements DbAdaptor {

    public List<String> getDBs(ConnectInfo connectionInfo) throws SQLException {
        List<String> dbs = Lists.newArrayList();
        DatabaseMetaData metaData = getDatabaseMetaData(connectionInfo);
        try {
            ResultSet schemaSet = metaData.getSchemas();
            while (schemaSet.next()) {
                String db = schemaSet.getString("TABLE_SCHEM");
                dbs.add(db);
            }
        } catch (Exception e) {
            log.info("get meta schemas failed, try to get catalogs");
        }
        try {
            ResultSet catalogSet = metaData.getCatalogs();
            while (catalogSet.next()) {
                String db = catalogSet.getString("TABLE_CAT");
                dbs.add(db);
            }
        } catch (Exception e) {
            log.info("get meta catalogs failed, try to get schemas");
        }
        return dbs;
    }

    public List<String> getTables(ConnectInfo connectionInfo, String schemaName)
            throws SQLException {
        List<String> tablesAndViews = new ArrayList<>();
        DatabaseMetaData metaData = getDatabaseMetaData(connectionInfo);

        try {
            ResultSet resultSet = getResultSet(schemaName, metaData);
            while (resultSet.next()) {
                String name = resultSet.getString("TABLE_NAME");
                tablesAndViews.add(name);
            }
        } catch (SQLException e) {
            log.error("Failed to get tables and views", e);
        }
        return tablesAndViews;
    }

    protected ResultSet getResultSet(String schemaName, DatabaseMetaData metaData)
            throws SQLException {
        return metaData.getTables(schemaName, schemaName, null, new String[] {"TABLE", "VIEW"});
    }

    public List<DBColumn> getColumns(ConnectInfo connectInfo, String schemaName, String tableName)
            throws SQLException {
        List<DBColumn> dbColumns = Lists.newArrayList();
        DatabaseMetaData metaData = getDatabaseMetaData(connectInfo);
        ResultSet columns = metaData.getColumns(schemaName, schemaName, tableName, null);
        while (columns.next()) {
            String columnName = columns.getString("COLUMN_NAME");
            String dataType = columns.getString("TYPE_NAME");
            String remarks = columns.getString("REMARKS");
            dbColumns.add(new DBColumn(columnName, dataType, remarks));
        }
        return dbColumns;
    }

    protected DatabaseMetaData getDatabaseMetaData(ConnectInfo connectionInfo) throws SQLException {
        Connection connection = DriverManager.getConnection(connectionInfo.getUrl(),
                connectionInfo.getUserName(), connectionInfo.getPassword());
        return connection.getMetaData();
    }

}
