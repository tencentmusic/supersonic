package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.pojo.DuckDbSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** tools functions to duckDb query */
public class JdbcDuckDbUtils {

    public static void attachMysql(DuckDbSource duckDbSource, String host, Integer port,
            String user, String password, String database) throws Exception {
        try {
            duckDbSource.execute("INSTALL mysql");
            duckDbSource.execute("load mysql");
            String attachSql =
                    "ATTACH 'host=%s port=%s user=%s password=%s database=%s' AS mysqldb (TYPE mysql);";
            duckDbSource.execute(String.format(attachSql, host, port, user, password, database));
            duckDbSource.execute("SET mysql_experimental_filter_pushdown = true;");
        } catch (Exception e) {
            throw e;
        }
    }

    public static List<String> getParquetColumns(DuckDbSource duckDbSource, String parquetPath)
            throws Exception {
        SemanticQueryResp queryResultWithColumns = new SemanticQueryResp();
        duckDbSource.query(
                String.format("SELECT distinct name FROM parquet_schema('%s')", parquetPath),
                queryResultWithColumns);
        if (!queryResultWithColumns.getResultList().isEmpty()) {
            return queryResultWithColumns.getResultList().stream()
                    .filter(l -> l.containsKey("name") && Objects.nonNull(l.get("name")))
                    .map(l -> (String) l.get("name")).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public static List<String> getParquetPartition(DuckDbSource duckDbSource, String parquetPath,
            String partitionName) throws Exception {
        SemanticQueryResp queryResultWithColumns = new SemanticQueryResp();
        duckDbSource.query(String.format("SELECT distinct %s as partition FROM read_parquet('%s')",
                partitionName, parquetPath), queryResultWithColumns);
        if (!queryResultWithColumns.getResultList().isEmpty()) {
            return queryResultWithColumns.getResultList().stream()
                    .filter(l -> l.containsKey("partition") && Objects.nonNull(l.get("partition")))
                    .map(l -> (String) l.get("partition")).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public static boolean createDatabase(DuckDbSource duckDbSource, String db) throws Exception {
        duckDbSource.execute("CREATE SCHEMA IF NOT EXISTS " + db);
        return true;
    }

    public static boolean createView(DuckDbSource duckDbSource, String view, String sql)
            throws Exception {
        duckDbSource.execute(String.format("CREATE OR REPLACE VIEW %s AS %s;", view, sql));
        return true;
    }
}
