package com.tencent.supersonic.headless.server.adaptor.db;


import com.tencent.supersonic.headless.server.pojo.EngineTypeEnum;
import java.util.HashMap;
import java.util.Map;


public class DbAdaptorFactory {

    private static Map<String, DbAdaptor> dbAdaptorMap;

    static {
        dbAdaptorMap = new HashMap<>();
        dbAdaptorMap.put(EngineTypeEnum.CLICKHOUSE.getName(), new ClickHouseAdaptor());
        dbAdaptorMap.put(EngineTypeEnum.MYSQL.getName(), new MysqlAdaptor());
        dbAdaptorMap.put(EngineTypeEnum.H2.getName(), new H2Adaptor());
    }

    public static DbAdaptor getEngineAdaptor(String engineType) {
        return dbAdaptorMap.get(engineType);
    }

}
