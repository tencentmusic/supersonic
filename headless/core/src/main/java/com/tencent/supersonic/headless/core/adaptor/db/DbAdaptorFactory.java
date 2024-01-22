package com.tencent.supersonic.headless.core.adaptor.db;

import com.tencent.supersonic.headless.api.pojo.enums.EngineType;

import java.util.HashMap;
import java.util.Map;


public class DbAdaptorFactory {

    private static Map<String, DbAdaptor> dbAdaptorMap;

    static {
        dbAdaptorMap = new HashMap<>();
        dbAdaptorMap.put(EngineType.CLICKHOUSE.getName(), new ClickHouseAdaptor());
        dbAdaptorMap.put(EngineType.MYSQL.getName(), new MysqlAdaptor());
        dbAdaptorMap.put(EngineType.H2.getName(), new H2Adaptor());
        dbAdaptorMap.put(EngineType.POSTGRESQL.getName(), new PostgresqlAdaptor());
    }

    public static DbAdaptor getEngineAdaptor(String engineType) {
        return dbAdaptorMap.get(engineType);
    }

}
