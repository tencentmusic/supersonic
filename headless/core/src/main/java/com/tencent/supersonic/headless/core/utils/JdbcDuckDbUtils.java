package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.pojo.DuckDbSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** tools functions to duckDb query */
public class JdbcDuckDbUtils {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private static void validateIdentifier(String name, String context) {
        if (name == null || !IDENTIFIER_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Invalid SQL identifier for " + context + ": " + name);
        }
    }

    private static void validatePath(String path, String context) {
        if (path == null || path.contains("'") || path.contains(";") || path.contains("--")
                || path.contains("/*")) {
            throw new IllegalArgumentException(
                    "Invalid path for " + context + ": contains disallowed characters");
        }
    }

    public static void attachMysql(DuckDbSource duckDbSource, String host, Integer port,
            String user, String password, String database) throws Exception {
        validateIdentifier(database, "database");
        if (host != null && (host.contains("'") || host.contains(" "))) {
            throw new IllegalArgumentException("Invalid ATTACH parameter host: " + host);
        }
        if (user != null && (user.contains("'") || user.contains(" "))) {
            throw new IllegalArgumentException("Invalid ATTACH parameter user: " + user);
        }
        if (password != null && password.contains("'")) {
            throw new IllegalArgumentException(
                    "Invalid ATTACH parameter password: contains single quote");
        }
        duckDbSource.execute("INSTALL mysql");
        duckDbSource.execute("load mysql");
        String attachSql =
                "ATTACH 'host=%s port=%s user=%s password=%s database=%s' AS mysqldb (TYPE mysql);";
        duckDbSource.execute(String.format(attachSql, host, port, user, password, database));
        duckDbSource.execute("SET mysql_experimental_filter_pushdown = true;");
    }

    public static List<String> getParquetColumns(DuckDbSource duckDbSource, String parquetPath)
            throws Exception {
        validatePath(parquetPath, "parquetPath");
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
        validatePath(parquetPath, "parquetPath");
        validateIdentifier(partitionName, "partitionName");
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
        validateIdentifier(db, "schema name");
        duckDbSource.execute("CREATE SCHEMA IF NOT EXISTS " + db);
        return true;
    }

    public static boolean createView(DuckDbSource duckDbSource, String view, String sql)
            throws Exception {
        validateIdentifier(view, "view name");
        duckDbSource.execute(String.format("CREATE OR REPLACE VIEW %s AS %s;", view, sql));
        return true;
    }
}
