package com.tencent.supersonic.headless.api.pojo.enums;


import com.tencent.supersonic.common.pojo.Constants;
import java.util.HashSet;
import java.util.Set;

public enum DataType {

    MYSQL("mysql", "mysql", "com.mysql.cj.jdbc.Driver", "`", "`", "'", "'"),

    HIVE2("hive2", "hive", "org.apache.hive.jdbc.HiveDriver", "`", "`", "`", "`"),

    ORACLE("oracle", "oracle", "oracle.jdbc.driver.OracleDriver", "\"", "\"", "\"", "\""),

    SQLSERVER("sqlserver", "sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "\"", "\"", "\"", "\""),

    H2("h2", "h2", "org.h2.Driver", "`", "`", "\"", "\""),

    PHOENIX("phoenix", "hbase phoenix", "org.apache.phoenix.jdbc.PhoenixDriver", "", "", "\"", "\""),

    MONGODB("mongo", "mongodb", "mongodb.jdbc.MongoDriver", "`", "`", "\"", "\""),

    ELASTICSEARCH("elasticsearch", "elasticsearch", "com.amazon.opendistroforelasticsearch.jdbc.Driver", "", "", "'",
            "'"),

    PRESTO("presto", "presto", "com.facebook.presto.jdbc.PrestoDriver", "\"", "\"", "\"", "\""),

    MOONBOX("moonbox", "moonbox", "moonbox.jdbc.MbDriver", "`", "`", "`", "`"),

    CASSANDRA("cassandra", "cassandra", "com.github.adejanovski.cassandra.jdbc.CassandraDriver", "", "", "'", "'"),

    CLICKHOUSE("clickhouse", "clickhouse", "ru.yandex.clickhouse.ClickHouseDriver", "", "", "\"", "\""),

    KYLIN("kylin", "kylin", "org.apache.kylin.jdbc.Driver", "\"", "\"", "\"", "\""),

    VERTICA("vertica", "vertica", "com.vertica.jdbc.Driver", "", "", "'", "'"),

    HANA("sap", "sap hana", "com.sap.db.jdbc.Driver", "", "", "'", "'"),

    IMPALA("impala", "impala", "com.cloudera.impala.jdbc41.Driver", "", "", "'", "'"),

    TDENGINE("TAOS", "TAOS", "com.taosdata.jdbc.TSDBDriver", "'", "'", "\"", "\""),

    POSTGRESQL("postgresql", "postgresql", "org.postgresql.Driver", "'", "'", "\"", "\"");

    private String feature;
    private String desc;
    private String driver;
    private String keywordPrefix;
    private String keywordSuffix;
    private String aliasPrefix;
    private String aliasSuffix;

    DataType(String feature, String desc, String driver, String keywordPrefix, String keywordSuffix,
             String aliasPrefix, String aliasSuffix) {
        this.feature = feature;
        this.desc = desc;
        this.driver = driver;
        this.keywordPrefix = keywordPrefix;
        this.keywordSuffix = keywordSuffix;
        this.aliasPrefix = aliasPrefix;
        this.aliasSuffix = aliasSuffix;
    }

    public static DataType urlOf(String jdbcUrl) throws RuntimeException {
        String url = jdbcUrl.toLowerCase().trim();
        for (DataType dataTypeEnum : values()) {
            if (url.startsWith(String.format(Constants.JDBC_PREFIX_FORMATTER, dataTypeEnum.feature))) {
                return dataTypeEnum;
            }
        }
        return null;
    }

    public static Set<String> getAllSupportedDatasourceNameSet() {
        Set<String> datasourceSet = new HashSet<>();
        for (DataType datasource : values()) {
            datasourceSet.add(datasource.getFeature());
        }
        return datasourceSet;
    }

    public String getFeature() {
        return feature;
    }

    public String getDesc() {
        return desc;
    }

    public String getDriver() {
        return driver;
    }

    public String getKeywordPrefix() {
        return keywordPrefix;
    }

    public String getKeywordSuffix() {
        return keywordSuffix;
    }

    public String getAliasPrefix() {
        return aliasPrefix;
    }

    public String getAliasSuffix() {
        return aliasSuffix;
    }
}