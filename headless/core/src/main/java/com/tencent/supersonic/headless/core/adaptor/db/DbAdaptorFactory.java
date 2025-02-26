package com.tencent.supersonic.headless.core.adaptor.db;

import com.tencent.supersonic.common.pojo.enums.EngineType;

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
        dbAdaptorMap.put(EngineType.OTHER.getName(), new DefaultDbAdaptor());
        dbAdaptorMap.put(EngineType.DUCKDB.getName(), new DuckdbAdaptor());
        dbAdaptorMap.put(EngineType.HANADB.getName(), new HanadbAdaptor());
        dbAdaptorMap.put(EngineType.STARROCKS.getName(), new StarrocksAdaptor());
        dbAdaptorMap.put(EngineType.KYUUBI.getName(), new KyuubiAdaptor());
        dbAdaptorMap.put(EngineType.PRESTO.getName(), new PrestoAdaptor());
        dbAdaptorMap.put(EngineType.TRINO.getName(), new TrinoAdaptor());
    }

    public static DbAdaptor getEngineAdaptor(String engineType) {
        return dbAdaptorMap.get(engineType.toUpperCase());
    }
}
