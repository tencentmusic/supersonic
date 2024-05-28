package com.tencent.supersonic.headless.core.pojo;


import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * duckDb connection session object
 */
@Component
@Slf4j
public class DuckDbSource {

    @Value("${accelerator.duckDb.temp:/data1/duck/tmp/}")
    protected String duckDbTemp;

    @Value("${accelerator.duckDb.maximumPoolSize:10}")
    protected Integer duckDbMaximumPoolSize;

    @Value("${accelerator.duckDb.MaxLifetime:3}")
    protected Integer duckDbMaxLifetime;

    @Value("${accelerator.duckDb.memoryLimit:31}")
    protected Integer memoryLimit;

    @Value("${accelerator.duckDb.threads:32}")
    protected Integer threads;


    @Autowired
    @Qualifier("duckDbDataSource")
    protected DataSource duckDbDataSource;

    @Autowired
    @Qualifier("duckDbJdbcTemplate")
    protected JdbcTemplate duckDbJdbcTemplate;

    @Bean(name = "duckDbConfig")
    public HikariConfig getHikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.duckdb.DuckDBDriver");
        config.setMaximumPoolSize(duckDbMaximumPoolSize);
        config.setMaxLifetime(duckDbMaxLifetime);
        config.setJdbcUrl("jdbc:duckdb:");
        return config;
    }

    @Bean(name = "duckDbDataSource")
    @DependsOn("duckDbConfig")
    public DataSource getDuckDbDataSource(@Qualifier("duckDbConfig") HikariConfig config) {
        HikariDataSource ds = new HikariDataSource(config);
        return ds;
    }

    @Bean("duckDbJdbcTemplate")
    @DependsOn("duckDbDataSource")
    public JdbcTemplate getDuckDbTemplate(@Qualifier("duckDbDataSource") DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        jdbcTemplate.setDataSource(dataSource);
        init(jdbcTemplate);
        return jdbcTemplate;
    }

    protected void init(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute(String.format("SET memory_limit = '%sGB';", memoryLimit));
        jdbcTemplate.execute(String.format("SET temp_directory='%s';", duckDbTemp));
        jdbcTemplate.execute(String.format("SET threads TO %s;", threads));
        jdbcTemplate.execute("SET enable_object_cache = true;");
    }

    public JdbcTemplate getDuckDbJdbcTemplate() {
        return duckDbJdbcTemplate;
    }

    public void setDuckDbJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.duckDbJdbcTemplate = jdbcTemplate;
    }

    public void execute(String sql) {
        duckDbJdbcTemplate.execute(sql);
    }

    public void query(String sql, SemanticQueryResp queryResultWithColumns) {
        duckDbJdbcTemplate.query(sql, rs -> {
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
            List<Map<String, Object>> resultList = buildResult(rs);
            queryResultWithColumns.setResultList(resultList);
            return queryResultWithColumns;
        });
    }

    public static List<Map<String, Object>> buildResult(ResultSet resultSet) {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            ResultSetMetaData rsMeta = resultSet.getMetaData();
            int columnCount = rsMeta.getColumnCount();
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String column = rsMeta.getColumnName(i);
                    switch (rsMeta.getColumnType(i)) {
                        case java.sql.Types.BOOLEAN:
                            row.put(column, resultSet.getBoolean(i));
                            break;
                        case java.sql.Types.INTEGER:
                            row.put(column, resultSet.getInt(i));
                            break;
                        case java.sql.Types.BIGINT:
                            row.put(column, resultSet.getLong(i));
                            break;
                        case java.sql.Types.DOUBLE:
                            row.put(column, resultSet.getDouble(i));
                            break;
                        case java.sql.Types.VARCHAR:
                            row.put(column, resultSet.getString(i));
                            break;
                        case java.sql.Types.NUMERIC:
                            row.put(column, resultSet.getBigDecimal(i));
                            break;
                        case java.sql.Types.TINYINT:
                            row.put(column, (int) resultSet.getByte(i));
                            break;
                        case java.sql.Types.SMALLINT:
                            row.put(column, resultSet.getShort(i));
                            break;
                        case java.sql.Types.REAL:
                            row.put(column, resultSet.getFloat(i));
                            break;
                        case java.sql.Types.DATE:
                            row.put(column, resultSet.getDate(i));
                            break;
                        case java.sql.Types.TIME:
                            row.put(column, resultSet.getTime(i));
                            break;
                        case java.sql.Types.TIMESTAMP:
                            row.put(column, resultSet.getTimestamp(i));
                            break;
                        case java.sql.Types.JAVA_OBJECT:
                            row.put(column, resultSet.getObject(i));
                            break;
                        default:
                            throw new Exception("get result row type not found :" + rsMeta.getColumnType(i));
                    }
                }
                list.add(row);
            }
        } catch (Exception e) {
            log.error("buildResult error {}", e);
        }
        return list;
    }
}
