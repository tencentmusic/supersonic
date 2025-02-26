package com.tencent.supersonic.headless.core.adaptor.db;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.enums.FieldType;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
public abstract class BaseDbAdaptor implements DbAdaptor {

    @Override
    public List<String> getCatalogs(ConnectInfo connectInfo) throws SQLException {
        List<String> catalogs = Lists.newArrayList();
        try (Connection con = getConnection(connectInfo);
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("SHOW CATALOGS")) {
            while (rs.next()) {
                catalogs.add(rs.getString(1));
            }
        }
        return catalogs;
    }

    public List<String> getDBs(ConnectInfo connectionInfo, String catalog) throws SQLException {
        // Except for special types implemented separately, the generic logic catalog does not take
        // effect.
        return getDBs(connectionInfo);
    }

    protected List<String> getDBs(ConnectInfo connectionInfo) throws SQLException {
        List<String> dbs = Lists.newArrayList();
        try {
            try (ResultSet schemaSet = getDatabaseMetaData(connectionInfo).getSchemas()) {
                while (schemaSet.next()) {
                    String db = schemaSet.getString("TABLE_SCHEM");
                    dbs.add(db);
                }
            }
        } catch (Exception e) {
            log.warn("get meta schemas failed", e);
            log.warn("get meta schemas failed, try to get catalogs");
        }
        try {
            try (ResultSet catalogSet = getDatabaseMetaData(connectionInfo).getCatalogs()) {
                while (catalogSet.next()) {
                    String db = catalogSet.getString("TABLE_CAT");
                    dbs.add(db);
                }
            }
        } catch (Exception e) {
            log.warn("get meta catalogs failed", e);
            log.warn("get meta catalogs failed, try to get schemas");
        }
        return dbs;
    }

    @Override
    public List<String> getTables(ConnectInfo connectInfo, String catalog, String schemaName)
            throws SQLException {
        // Except for special types implemented separately, the generic logic catalog does not take
        // effect.
        return getTables(connectInfo, schemaName);
    }

    protected List<String> getTables(ConnectInfo connectionInfo, String schemaName)
            throws SQLException {
        List<String> tablesAndViews = new ArrayList<>();

        try {
            try (ResultSet resultSet =
                    getResultSet(schemaName, getDatabaseMetaData(connectionInfo))) {
                while (resultSet.next()) {
                    String name = resultSet.getString("TABLE_NAME");
                    tablesAndViews.add(name);
                }
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



    public List<DBColumn> getColumns(ConnectInfo connectInfo, String catalog, String schemaName,
            String tableName) throws SQLException {
        List<DBColumn> dbColumns = new ArrayList<>();
        // 确保连接会自动关闭
        try (ResultSet columns =
                getDatabaseMetaData(connectInfo).getColumns(catalog, schemaName, tableName, null)) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String dataType = columns.getString("TYPE_NAME");
                String remarks = columns.getString("REMARKS");
                FieldType fieldType = classifyColumnType(dataType);
                dbColumns.add(new DBColumn(columnName, dataType, remarks, fieldType));
            }
        }
        return dbColumns;
    }

    protected DatabaseMetaData getDatabaseMetaData(ConnectInfo connectionInfo) throws SQLException {
        Connection connection = getConnection(connectionInfo);
        return connection.getMetaData();
    }

    public Connection getConnection(ConnectInfo connectionInfo) throws SQLException {
        final Properties properties = getProperties(connectionInfo);
        return DriverManager.getConnection(connectionInfo.getUrl(), properties);
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

    public Properties getProperties(ConnectInfo connectionInfo) {
        final Properties properties = new Properties();
        String url = connectionInfo.getUrl().toLowerCase();

        // 设置通用属性
        properties.setProperty("user", connectionInfo.getUserName());

        // 针对 Presto 和 Trino ssl=false 的情况，不需要设置密码
        if (url.startsWith("jdbc:presto") || url.startsWith("jdbc:trino")) {
            // 检查是否需要处理 SSL
            if (!url.contains("ssl=false")) {
                properties.setProperty("password", connectionInfo.getPassword());
            }
        } else {
            // 针对其他数据库类型
            properties.setProperty("password", connectionInfo.getPassword());
        }

        return properties;
    }
}
