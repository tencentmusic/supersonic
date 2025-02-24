package com.tencent.supersonic.headless.core.adaptor.db;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class StarrocksAdaptor extends MysqlAdaptor {

    @Override
    public List<String> getDBs(ConnectInfo connectionInfo, String catalog) throws SQLException {
        List<String> dbs = Lists.newArrayList();
        final StringBuilder sql =  new StringBuilder("SHOW DATABASES");
        if (StringUtils.isNotBlank(catalog)) {
            sql.append(" IN ").append(catalog);
        }
        try (Connection con = DriverManager.getConnection(connectionInfo.getUrl(),
                connectionInfo.getUserName(), connectionInfo.getPassword());
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
        final StringBuilder sql =  new StringBuilder("SHOW TABLES");
        if (StringUtils.isNotBlank(catalog)) {
            sql.append(" IN ").append(catalog).append(".").append(schemaName);
        }else {
            sql.append(" IN ").append(schemaName);
        }

        try (Connection con = DriverManager.getConnection(connectInfo.getUrl(),
                connectInfo.getUserName(), connectInfo.getPassword());
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql.toString())) {
            while (rs.next()) {
                tablesAndViews.add(rs.getString(1));
            }
        }
        return tablesAndViews;
    }
}
