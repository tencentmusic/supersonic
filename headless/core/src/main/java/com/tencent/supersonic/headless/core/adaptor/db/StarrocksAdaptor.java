package com.tencent.supersonic.headless.core.adaptor.db;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.sql.*;
import java.util.List;

@Slf4j
public class StarrocksAdaptor extends MysqlAdaptor {

    @Override
    public List<String> getCatalogs(ConnectInfo connectInfo) throws SQLException {
        List<String> catalogs = Lists.newArrayList();
        try (Connection con = DriverManager.getConnection(connectInfo.getUrl(),
                connectInfo.getUserName(), connectInfo.getPassword());
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("SHOW CATALOGS")) {
            while (rs.next()) {
                catalogs.add(rs.getString(1));
            }
        }
        return catalogs;
    }

    @Override
    public List<String> getDBs(ConnectInfo connectionInfo, String catalog) throws SQLException {
        Assert.hasText(catalog, "StarRocks type catalog can not be null or empty");
        List<String> dbs = Lists.newArrayList();
        try (Connection con = DriverManager.getConnection(connectionInfo.getUrl(),
                connectionInfo.getUserName(), connectionInfo.getPassword());
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("SHOW DATABASES IN " + catalog)) {
            while (rs.next()) {
                dbs.add(rs.getString(1));
            }
        }
        return dbs;
    }
}
