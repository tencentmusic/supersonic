package com.tencent.supersonic.semantic.model.domain.utils;

import static com.tencent.supersonic.common.pojo.Constants.AT_SYMBOL;

import com.tencent.supersonic.semantic.api.model.enums.DataTypeEnum;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.model.domain.pojo.JdbcDataSource;
import java.rmi.ServerException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class SqlUtils {

    @Getter
    private DatabaseResp databaseResp;

    @Autowired
    private JdbcDataSource jdbcDataSource;

    @Value("${source.result-limit:1000000}")
    private int resultLimit;

    @Value("${source.enable-query-log:false}")
    private boolean isQueryLogEnable;

    @Getter
    private DataTypeEnum dataTypeEnum;

    @Getter
    private JdbcDataSourceUtils jdbcDataSourceUtils;

    public SqlUtils() {

    }

    public SqlUtils(DatabaseResp databaseResp) {
        this.databaseResp = databaseResp;
        this.dataTypeEnum = DataTypeEnum.urlOf(databaseResp.getUrl());
    }

    public SqlUtils init(DatabaseResp databaseResp) {
        //todo Password decryption
        return SqlUtilsBuilder
                .getBuilder()
                .withName(databaseResp.getId() + AT_SYMBOL + databaseResp.getName())
                .withType(databaseResp.getType())
                .withJdbcUrl(databaseResp.getUrl())
                .withUsername(databaseResp.getUsername())
                .withPassword(databaseResp.getPassword())
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

    public void execute(String sql, QueryResultWithSchemaResp queryResultWithColumns) {
        getResult(sql, queryResultWithColumns, jdbcTemplate());
    }

    public JdbcTemplate jdbcTemplate() throws RuntimeException {
        Connection connection = null;
        try {
            connection = jdbcDataSourceUtils.getConnection(databaseResp);
        } catch (Exception e) {
            log.warn("e:", e);
        } finally {
            JdbcDataSourceUtils.releaseConnection(connection);
        }
        DataSource dataSource = jdbcDataSourceUtils.getDataSource(databaseResp);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setDatabaseProductName(databaseResp.getName());
        jdbcTemplate.setFetchSize(500);
        log.info("jdbcTemplate:{}, dataSource:{}", jdbcTemplate, dataSource);
        return jdbcTemplate;
    }


    public void queryInternal(String sql, QueryResultWithSchemaResp queryResultWithColumns) {
        getResult(sql, queryResultWithColumns, jdbcTemplate());
    }

    private QueryResultWithSchemaResp getResult(String sql, QueryResultWithSchemaResp queryResultWithColumns,
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
            map.put(colName, value instanceof byte[] ? new String((byte[]) value) : value);
        }
        return map;
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
            DatabaseResp databaseResp = DatabaseResp.builder()
                    .name(this.name)
                    .type(this.type)
                    .url(this.jdbcUrl)
                    .username(this.username)
                    .password(this.password)
                    .build();

            SqlUtils sqlUtils = new SqlUtils(databaseResp);
            sqlUtils.jdbcDataSource = this.jdbcDataSource;
            sqlUtils.resultLimit = this.resultLimit;
            sqlUtils.isQueryLogEnable = this.isQueryLogEnable;
            sqlUtils.jdbcDataSourceUtils = new JdbcDataSourceUtils(this.jdbcDataSource);

            return sqlUtils;
        }
    }
}
