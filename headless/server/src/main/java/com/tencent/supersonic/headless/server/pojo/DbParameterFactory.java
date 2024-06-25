package com.tencent.supersonic.headless.server.pojo;

import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import java.util.LinkedHashMap;
import java.util.Map;


public class DbParameterFactory {

    private static Map<String, DbParametersBuilder> parametersBuilder;

    static {
        parametersBuilder = new LinkedHashMap<>();
        parametersBuilder.put(EngineType.H2.getName(), new H2ParametersBuilder());
        parametersBuilder.put(EngineType.CLICKHOUSE.getName(), new ClickHouseParametersBuilder());
        parametersBuilder.put(EngineType.MYSQL.getName(), new MysqlParametersBuilder());
        parametersBuilder.put(EngineType.POSTGRESQL.getName(), new PostgresqlParametersBuilder());
    }

    public static DbParametersBuilder get(String engineType) {
        return parametersBuilder.get(engineType);
    }

    public static Map<String, DbParametersBuilder> getMap() {
        return parametersBuilder;
    }

}