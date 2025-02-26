package com.tencent.supersonic.headless.core.utils;

import javax.sql.DataSource;

import com.alibaba.druid.util.StringUtils;
import com.tencent.supersonic.common.util.MD5Util;
import com.tencent.supersonic.headless.api.pojo.enums.DataType;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.core.pojo.JdbcDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import static com.tencent.supersonic.common.pojo.Constants.*;

/** tools functions about jdbc */
@Slf4j
public class JdbcDataSourceUtils {

    @Getter
    private static Set releaseSourceSet = new HashSet();
    private JdbcDataSource jdbcDataSource;

    public JdbcDataSourceUtils(JdbcDataSource jdbcDataSource) {
        this.jdbcDataSource = jdbcDataSource;
    }

    public static boolean testDatabase(DatabaseResp database) {

        try {
            Class.forName(getDriverClassName(database.getUrl()));
        } catch (ClassNotFoundException e) {
            log.error(e.toString(), e);
            return false;
        }
        // presto/trino ssl=false connection need password
        if (database.getUrl().startsWith("jdbc:presto")
                || database.getUrl().startsWith("jdbc:trino")) {
            if (database.getUrl().toLowerCase().contains("ssl=false")) {
                try (Connection con = DriverManager.getConnection(database.getUrl(),
                        database.getUsername(), null)) {
                    return con != null;
                } catch (SQLException e) {
                    log.error(e.toString(), e);
                }
            }
        } else {
            try (Connection con = DriverManager.getConnection(database.getUrl(),
                    database.getUsername(), database.passwordDecrypt())) {
                return con != null;
            } catch (SQLException e) {
                log.error(e.toString(), e);
            }
        }

        return false;
    }

    public static void releaseConnection(Connection connection) {
        if (null != connection) {
            try {
                connection.close();
            } catch (Exception e) {
                log.error("Connection release error", e);
            }
        }
    }

    public static void closeResult(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                log.error("ResultSet close error", e);
            }
        }
    }

    public static String isSupportedDatasource(String jdbcUrl) {
        String dataSourceName = getDataSourceName(jdbcUrl);
        if (StringUtils.isEmpty(dataSourceName)) {
            throw new RuntimeException("Not supported dataSource: jdbcUrl=" + jdbcUrl);
        }

        if (!DataType.getAllSupportedDatasourceNameSet().contains(dataSourceName)) {
            throw new RuntimeException("Not supported dataSource: jdbcUrl=" + jdbcUrl);
        }

        String urlPrefix = String.format(JDBC_PREFIX_FORMATTER, dataSourceName);
        String checkUrl = jdbcUrl.replaceFirst(DOUBLE_SLASH, EMPTY).replaceFirst(AT_SYMBOL, EMPTY);
        if (urlPrefix.equals(checkUrl)) {
            throw new RuntimeException("Communications link failure");
        }

        return dataSourceName;
    }

    public static String getDataSourceName(String jdbcUrl) {
        String dataSourceName = null;
        jdbcUrl = jdbcUrl.replaceAll(NEW_LINE_CHAR, EMPTY).replaceAll(SPACE, EMPTY).trim();
        Matcher matcher = PATTERN_JDBC_TYPE.matcher(jdbcUrl);
        if (matcher.find()) {
            dataSourceName = matcher.group().split(COLON)[1];
        }
        return dataSourceName;
    }

    public static String getDriverClassName(String jdbcUrl) {

        String className = null;

        try {
            className = DriverManager.getDriver(jdbcUrl.trim()).getClass().getName();
        } catch (SQLException e) {
            log.error("e", e);
        }

        if (!StringUtils.isEmpty(className) && !className.contains("com.sun.proxy")
                && !className.contains("net.sf.cglib.proxy")) {
            return className;
        }

        DataType dataTypeEnum = DataType.urlOf(jdbcUrl);
        if (dataTypeEnum != null) {
            return dataTypeEnum.getDriver();
        }
        throw new RuntimeException("Not supported data type: jdbcUrl=" + jdbcUrl);
    }

    public static String getKey(String name, String jdbcUrl, String username, String password,
            String version, boolean isExt) {

        StringBuilder sb = new StringBuilder();

        sb.append(StringUtils.isEmpty(name) ? "null" : name).append(COLON);
        sb.append(StringUtils.isEmpty(username) ? "null" : username).append(COLON);
        sb.append(StringUtils.isEmpty(password) ? "null" : password).append(COLON);
        sb.append(jdbcUrl.trim()).append(COLON);
        if (isExt && !StringUtils.isEmpty(version)) {
            sb.append(version);
        } else {
            sb.append("null");
        }

        return MD5Util.getMD5(sb.toString(), true, 64);
    }

    public DataSource getDataSource(DatabaseResp database) throws RuntimeException {
        return jdbcDataSource.getDataSource(database);
    }

    public Connection getConnection(DatabaseResp database) throws RuntimeException {
        Connection conn = getConnectionWithRetry(database);
        if (conn == null) {
            try {
                releaseDataSource(database);
                DataSource dataSource = getDataSource(database);
                return dataSource.getConnection();
            } catch (Exception e) {
                log.error("Get connection error, jdbcUrl:{}, e:{}", database.getUrl(), e);
                throw new RuntimeException("Get connection error, jdbcUrl:" + database.getUrl()
                        + " you can try again later or reset datasource");
            }
        }
        return conn;
    }

    private Connection getConnectionWithRetry(DatabaseResp database) {
        int rc = 1;
        for (;;) {

            if (rc > 3) {
                return null;
            }

            try {
                Connection connection = getDataSource(database).getConnection();
                if (connection != null && connection.isValid(5)) {
                    return connection;
                }
            } catch (Exception e) {
                log.error("e", e);
            }

            try {
                Thread.sleep((long) Math.pow(2, rc) * 1000);
            } catch (InterruptedException e) {
                log.error("e", e);
            }

            rc++;
        }
    }

    public void releaseDataSource(DatabaseResp database) {
        jdbcDataSource.removeDatasource(database);
    }
}
