package com.tencent.supersonic.semantic.model.domain.adaptor.engineadapter;


import com.tencent.supersonic.semantic.model.domain.pojo.EngineTypeEnum;
import java.util.HashMap;
import java.util.Map;


public class EngineAdaptorFactory {

    private static Map<String, EngineAdaptor> engineAdaptorMap;

    static {
        engineAdaptorMap = new HashMap<>();
        engineAdaptorMap.put(EngineTypeEnum.CLICKHOUSE.getName(), new ClickHouseAdaptor());
        engineAdaptorMap.put(EngineTypeEnum.MYSQL.getName(), new MysqlAdaptor());
        engineAdaptorMap.put(EngineTypeEnum.H2.getName(), new H2Adaptor());
    }

    public static EngineAdaptor getEngineAdaptor(String engineType) {
        return engineAdaptorMap.get(engineType);
    }

}
