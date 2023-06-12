package com.tencent.supersonic.semantic.core.domain.adaptor.engineadapter;


import com.tencent.supersonic.semantic.core.domain.pojo.EngineTypeEnum;
import java.util.HashMap;
import java.util.Map;


public class EngineAdaptorFactory {

    private static Map<String, EngineAdaptor> engineAdaptorMap;

    static {
        engineAdaptorMap = new HashMap<>();
        engineAdaptorMap.put(EngineTypeEnum.CLICKHOUSE.getName(), new ClickHouseAdaptor());
        engineAdaptorMap.put(EngineTypeEnum.MYSQL.getName(), new MysqlAdaptor());
        engineAdaptorMap.put(EngineTypeEnum.H2.getName(), new MysqlAdaptor());
    }


    public static EngineAdaptor getEngineAdaptor(String engineType) {
        return engineAdaptorMap.get(engineType);
    }


}
