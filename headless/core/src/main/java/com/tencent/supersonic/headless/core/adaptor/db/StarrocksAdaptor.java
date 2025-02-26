package com.tencent.supersonic.headless.core.adaptor.db;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.enums.FieldType;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
public class StarrocksAdaptor extends MysqlAdaptor {

    @Override
    public List<String> getDBs(ConnectInfo connectionInfo, String catalog) throws SQLException {
        List<String> dbs = Lists.newArrayList();
        final StringBuilder sql = new StringBuilder("SHOW DATABASES");
        if (StringUtils.isNotBlank(catalog)) {
            sql.append(" IN ").append(catalog);
        }
        try (Connection con = getConnection(connectionInfo);
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql.toString())) {
            while (rs.next()) {
                dbs.add(rs.getString(1));
            }
        }
        return dbs;
    }

    @Override
    public List<String> getTables(ConnectInfo connectInfo, String catalog, String schemaName)
            throws SQLException {
        List<String> tablesAndViews = new ArrayList<>();
        final StringBuilder sql = new StringBuilder("SHOW TABLES");
        if (StringUtils.isNotBlank(catalog)) {
            sql.append(" IN ").append(catalog).append(".").append(schemaName);
        } else {
            sql.append(" IN ").append(schemaName);
        }

        try (Connection con = getConnection(connectInfo);
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql.toString())) {
            while (rs.next()) {
                tablesAndViews.add(rs.getString(1));
            }
        }
        return tablesAndViews;
    }

    @Override
    public List<DBColumn> getColumns(ConnectInfo connectInfo, String catalog, String schemaName,
            String tableName) throws SQLException {
        List<DBColumn> dbColumns = new ArrayList<>();

        try (Connection con = getConnection(connectInfo); Statement st = con.createStatement()) {

            // 切换到指定的 catalog（或 database/schema），这在某些 SQL 方言中很重要
            if (StringUtils.isNotBlank(catalog)) {
                st.execute("SET CATALOG " + catalog);
            }

            // 获取 DatabaseMetaData; 需要注意调用此方法的位置（在 USE 之后）
            DatabaseMetaData metaData = con.getMetaData();

            // 获取特定表的列信息
            try (ResultSet columns = metaData.getColumns(schemaName, schemaName, tableName, null)) {
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String dataType = columns.getString("TYPE_NAME");
                    String remarks = columns.getString("REMARKS");
                    FieldType fieldType = classifyColumnType(dataType);
                    dbColumns.add(new DBColumn(columnName, dataType, remarks, fieldType));
                }
            }
        }

        return dbColumns;
    }
}
