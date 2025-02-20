package com.tencent.supersonic.headless.core.adaptor.db;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.enums.FieldType;
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

    @Override
    public List<String> getCatalogs(ConnectInfo connectInfo) throws SQLException {
        // Apart from supporting multiple catalog types of data sources, other types will return an
        // empty set by default.
        return List.of();
    }

    public List<String> getDBs(ConnectInfo connectionInfo, String catalog) throws SQLException {
        // Except for special types implemented separately, the generic logic catalog does not take
        // effect.
        return getDBs(connectionInfo);
    }

    protected List<String> getDBs(ConnectInfo connectionInfo) throws SQLException {
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
            FieldType fieldType = classifyColumnType(dataType);
            dbColumns.add(new DBColumn(columnName, dataType, remarks, fieldType));
        }
        return dbColumns;
    }

    protected DatabaseMetaData getDatabaseMetaData(ConnectInfo connectionInfo) throws SQLException {
        Connection connection = DriverManager.getConnection(connectionInfo.getUrl(),
                connectionInfo.getUserName(), connectionInfo.getPassword());
        return connection.getMetaData();
    }

    public FieldType classifyColumnType(String typeName) {
        switch (typeName.toUpperCase()) {
            case "INT":
            case "INTEGER":
            case "BIGINT":
            case "SMALLINT":
            case "TINYINT":
            case "FLOAT":
            case "DOUBLE":
            case "DECIMAL":
            case "NUMERIC":
                return FieldType.measure;
            case "DATE":
            case "TIME":
            case "TIMESTAMP":
                return FieldType.time;
            default:
                return FieldType.categorical;
        }
    }

}
