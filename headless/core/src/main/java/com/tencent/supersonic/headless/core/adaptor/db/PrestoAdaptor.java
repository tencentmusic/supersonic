package com.tencent.supersonic.headless.core.adaptor.db;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.sql.*;
import java.util.List;

public class PrestoAdaptor extends BaseDbAdaptor {

    /** transform YYYYMMDD to YYYY-MM-DD YYYY-MM YYYY-MM-DD(MONDAY) */
    @Override
    public String getDateFormat(String dateType, String dateFormat, String column) {
        if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT_INT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return String.format("date_format(%s, '%%Y-%%m')", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return String.format(
                        "date_format(date_add('day', - (day_of_week(%s) - 2), %s), '%%Y-%%m-%%d')",
                        column, column);
            } else {
                return String.format("date_format(date_parse(%s, '%%Y%%m%%d'), '%%Y-%%m-%%d')",
                        column);
            }
        } else if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return String.format("date_format(%s, '%%Y-%%m')", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return String.format(
                        "date_format(date_add('day', - (day_of_week(%s) - 2), %s), '%%Y-%%m-%%d')",
                        column, column);
            } else {
                return column;
            }
        }
        return column;
    }

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
        List<String> dbs = Lists.newArrayList();
        String sql = "SHOW SCHEMAS";
        if (StringUtils.isNotBlank(catalog)) {
            sql = "SHOW SCHEMAS IN " + catalog;
        }
        try (Connection con = DriverManager.getConnection(connectionInfo.getUrl(),
                connectionInfo.getUserName(), connectionInfo.getPassword());
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                dbs.add(rs.getString(1));
            }
        }
        return dbs;
    }

    @Override
    public String rewriteSql(String sql) {
        return sql;
    }
}
