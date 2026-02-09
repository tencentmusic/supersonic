package com.tencent.supersonic.headless.core.utils;

import javax.sql.DataSource;

import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.pojo.enums.DataType;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.pojo.JdbcDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.rmi.ServerException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.tencent.supersonic.common.pojo.Constants.AT_SYMBOL;

/** tools functions about sql query */
@Slf4j
@Component
public class SqlUtils {

    @Getter
    private DatabaseResp database;

    @Autowired
    private JdbcDataSource jdbcDataSource;

    @Value("${s2.source.result-limit:1000000}")
    private int resultLimit;

    @Value("${s2.source.enable-query-log:false}")
    private boolean isQueryLogEnable;

    /**
     * Query timeout in seconds. 0 means no timeout. Default is 30 seconds. This sets
     * Statement.setQueryTimeout() on the underlying JDBC connection.
     */
    @Value("${s2.source.query-timeout:30}")
    private int queryTimeout;

    @Getter
    private DataType dataTypeEnum;

    @Getter
    private JdbcDataSourceUtils jdbcDataSourceUtils;

    /**
     * Pool type for connection pool isolation. Null means default pool.
     */
    @Getter
    private String poolType;

    /**
     * Override for max active connections. Null means use pool type default.
     */
    @Getter
    private Integer poolMaxActive;

    /**
     * Override for max wait time in ms. Null means use pool type default.
     */
    @Getter
    private Integer poolMaxWaitMs;

    public SqlUtils() {}

    public SqlUtils(DatabaseResp database) {
        this.database = database;
        this.dataTypeEnum = DataType.urlOf(database.getUrl());
    }

    /**
     * Initialize SqlUtils with default (INTERACTIVE) pool type.
     */
    public SqlUtils init(DatabaseResp database) {
        return init(database, "INTERACTIVE", null, null);
    }

    /**
     * Initialize SqlUtils with specified pool type for connection pool isolation.
     *
     * @param database the database configuration
     * @param poolType pool type (INTERACTIVE, REPORT, EXPORT, SYNC)
     * @param maxActive optional override for max active connections
     * @param maxWaitMs optional override for max wait time in ms
     */
    public SqlUtils init(DatabaseResp database, String poolType, Integer maxActive,
            Integer maxWaitMs) {
        return SqlUtilsBuilder.getBuilder()
                .withName(database.getId() + AT_SYMBOL + database.getName())
                .withType(database.getType()).withJdbcUrl(database.getUrl())
                .withUsername(database.getUsername()).withPassword(database.getPassword())
                .withJdbcDataSource(this.jdbcDataSource).withResultLimit(this.resultLimit)
                .withIsQueryLogEnable(this.isQueryLogEnable).withQueryTimeout(this.queryTimeout)
                .withPoolType(poolType).withPoolMaxActive(maxActive).withPoolMaxWaitMs(maxWaitMs)
                .build();
    }

    public List<Map<String, Object>> execute(String sql) throws ServerException {
        try {
            List<Map<String, Object>> list = jdbcTemplate().queryForList(sql);
            log.info("list:{}", list);
            return list;
        } catch (Exception e) {
            log.error(e.toString(), e);
            throw new ServerException(e.getMessage());
        }
    }

    public void execute(String sql, SemanticQueryResp queryResultWithColumns) {
        getResult(sql, queryResultWithColumns, jdbcTemplate());
    }

    public JdbcTemplate jdbcTemplate() throws RuntimeException {
        Connection connection = null;
        try {
            connection = jdbcDataSourceUtils.getConnection(database, poolType, poolMaxActive,
                    poolMaxWaitMs);
        } catch (Exception e) {
            log.warn("e:", e);
        } finally {
            JdbcDataSourceUtils.releaseConnection(connection);
        }
        DataSource dataSource = poolType != null
                ? jdbcDataSourceUtils.getDataSource(database, poolType, poolMaxActive,
                        poolMaxWaitMs)
                : jdbcDataSourceUtils.getDataSource(database);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setDatabaseProductName(database.getName());
        jdbcTemplate.setFetchSize(500);
        if (queryTimeout > 0) {
            jdbcTemplate.setQueryTimeout(queryTimeout);
        }
        return jdbcTemplate;
    }

    public void queryInternal(String sql, SemanticQueryResp queryResultWithColumns) {
        getResult(sql, queryResultWithColumns, jdbcTemplate());
    }

    private SemanticQueryResp getResult(String sql, SemanticQueryResp queryResultWithColumns,
            JdbcTemplate jdbcTemplate) {
        jdbcTemplate.query(sql, rs -> {
            if (null == rs) {
                return queryResultWithColumns;
            }

            ResultSetMetaData metaData = rs.getMetaData();
            List<QueryColumn> queryColumns = new ArrayList<>();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String key = metaData.getColumnLabel(i);
                queryColumns.add(new QueryColumn(key, metaData.getColumnTypeName(i)));
            }
            queryResultWithColumns.setColumns(queryColumns);

            List<Map<String, Object>> resultList = getAllData(rs, queryColumns);
            queryResultWithColumns.setResultList(resultList);
            return queryResultWithColumns;
        });
        return queryResultWithColumns;
    }

    private List<Map<String, Object>> getAllData(ResultSet rs, List<QueryColumn> queryColumns) {
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            while (rs.next()) {
                data.add(getLineData(rs, queryColumns));
            }
        } catch (Exception e) {
            log.warn("error in getAllData, e:", e);
        }
        return data;
    }

    private Map<String, Object> getLineData(ResultSet rs, List<QueryColumn> queryColumns)
            throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        for (QueryColumn queryColumn : queryColumns) {
            String colName = queryColumn.getBizName();
            Object value = rs.getObject(colName);
            map.put(colName, getValue(value));
        }
        return map;
    }

    private Object getValue(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate.format(DateTimeFormatter.ofPattern(DateUtils.DEFAULT_DATE_FORMAT));
        } else if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(DateTimeFormatter.ofPattern(DateUtils.DEFAULT_TIME_FORMAT));
        } else if (value instanceof Date date) {
            return DateUtils.format(date);
        } else if (value instanceof byte[]) {
            return new String((byte[]) value);
        }
        return value;
    }

    public static final class SqlUtilsBuilder {

        private JdbcDataSource jdbcDataSource;
        private int resultLimit;
        private boolean isQueryLogEnable;
        private int queryTimeout;
        private String name;
        private String type;
        private String jdbcUrl;
        private String username;
        private String password;
        private String poolType;
        private Integer poolMaxActive;
        private Integer poolMaxWaitMs;

        private SqlUtilsBuilder() {}

        public static SqlUtilsBuilder getBuilder() {
            return new SqlUtilsBuilder();
        }

        SqlUtilsBuilder withJdbcDataSource(JdbcDataSource jdbcDataSource) {
            this.jdbcDataSource = jdbcDataSource;
            return this;
        }

        SqlUtilsBuilder withResultLimit(int resultLimit) {
            this.resultLimit = resultLimit;
            return this;
        }

        SqlUtilsBuilder withIsQueryLogEnable(boolean isQueryLogEnable) {
            this.isQueryLogEnable = isQueryLogEnable;
            return this;
        }

        SqlUtilsBuilder withQueryTimeout(int queryTimeout) {
            this.queryTimeout = queryTimeout;
            return this;
        }

        SqlUtilsBuilder withName(String name) {
            this.name = name;
            return this;
        }

        SqlUtilsBuilder withType(String type) {
            this.type = type;
            return this;
        }

        SqlUtilsBuilder withJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return this;
        }

        SqlUtilsBuilder withUsername(String username) {
            this.username = username;
            return this;
        }

        SqlUtilsBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        SqlUtilsBuilder withPoolType(String poolType) {
            this.poolType = poolType;
            return this;
        }

        SqlUtilsBuilder withPoolMaxActive(Integer poolMaxActive) {
            this.poolMaxActive = poolMaxActive;
            return this;
        }

        SqlUtilsBuilder withPoolMaxWaitMs(Integer poolMaxWaitMs) {
            this.poolMaxWaitMs = poolMaxWaitMs;
            return this;
        }

        public SqlUtils build() {
            DatabaseResp database = DatabaseResp.builder().name(this.name)
                    .type(this.type.toUpperCase()).url(this.jdbcUrl).username(this.username)
                    .password(this.password).build();

            SqlUtils sqlUtils = new SqlUtils(database);
            sqlUtils.jdbcDataSource = this.jdbcDataSource;
            sqlUtils.resultLimit = this.resultLimit;
            sqlUtils.isQueryLogEnable = this.isQueryLogEnable;
            sqlUtils.queryTimeout = this.queryTimeout;
            sqlUtils.jdbcDataSourceUtils = new JdbcDataSourceUtils(this.jdbcDataSource);
            sqlUtils.poolType = this.poolType;
            sqlUtils.poolMaxActive = this.poolMaxActive;
            sqlUtils.poolMaxWaitMs = this.poolMaxWaitMs;

            return sqlUtils;
        }
    }
}
