package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.pojo.enums.DataType;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.pojo.Database;
import com.tencent.supersonic.headless.core.pojo.JdbcDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.rmi.ServerException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.tencent.supersonic.common.pojo.Constants.AT_SYMBOL;

/**
 * tools functions about sql query
 */
@Slf4j
@Component
public class SqlUtils {

    @Getter
    private Database database;

    @Autowired
    private JdbcDataSource jdbcDataSource;

    @Value("${s2.source.result-limit:1000000}")
    private int resultLimit;

    @Value("${s2.source.enable-query-log:false}")
    private boolean isQueryLogEnable;

    @Getter
    private DataType dataTypeEnum;

    @Getter
    private JdbcDataSourceUtils jdbcDataSourceUtils;

    public SqlUtils() {

    }

    public SqlUtils(Database database) {
        this.database = database;
        this.dataTypeEnum = DataType.urlOf(database.getUrl());
    }

    public SqlUtils init(Database database) {
        //todo Password decryption
        return SqlUtilsBuilder
                .getBuilder()
                .withName(database.getId() + AT_SYMBOL + database.getName())
                .withType(database.getType())
                .withJdbcUrl(database.getUrl())
                .withUsername(database.getUsername())
                .withPassword(database.getPassword())
                .withJdbcDataSource(this.jdbcDataSource)
                .withResultLimit(this.resultLimit)
                .withIsQueryLogEnable(this.isQueryLogEnable)
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
            connection = jdbcDataSourceUtils.getConnection(database);
        } catch (Exception e) {
            log.warn("e:", e);
        } finally {
            JdbcDataSourceUtils.releaseConnection(connection);
        }
        DataSource dataSource = jdbcDataSourceUtils.getDataSource(database);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setDatabaseProductName(database.getName());
        jdbcTemplate.setFetchSize(500);
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

    private Map<String, Object> getLineData(ResultSet rs, List<QueryColumn> queryColumns) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        for (QueryColumn queryColumn : queryColumns) {
            String colName = queryColumn.getNameEn();
            Object value = rs.getObject(colName);
            map.put(colName, getValue(value));
        }
        return map;
    }

    private Object getValue(Object value) {
        if (value instanceof LocalDate) {
            LocalDate localDate = (LocalDate) value;
            return localDate.format(DateTimeFormatter.ofPattern(DateUtils.DATE_FORMAT));
        } else if (value instanceof LocalDateTime) {
            LocalDateTime localDateTime = (LocalDateTime) value;
            return localDateTime.format(DateTimeFormatter.ofPattern(DateUtils.TIME_FORMAT));
        } else if (value instanceof Date) {
            Date date = (Date) value;
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
        private String name;
        private String type;
        private String jdbcUrl;
        private String username;
        private String password;

        private SqlUtilsBuilder() {

        }

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

        public SqlUtils build() {
            Database database = Database.builder()
                    .name(this.name)
                    .type(this.type)
                    .url(this.jdbcUrl)
                    .username(this.username)
                    .password(this.password)
                    .build();

            SqlUtils sqlUtils = new SqlUtils(database);
            sqlUtils.jdbcDataSource = this.jdbcDataSource;
            sqlUtils.resultLimit = this.resultLimit;
            sqlUtils.isQueryLogEnable = this.isQueryLogEnable;
            sqlUtils.jdbcDataSourceUtils = new JdbcDataSourceUtils(this.jdbcDataSource);

            return sqlUtils;
        }
    }
}
