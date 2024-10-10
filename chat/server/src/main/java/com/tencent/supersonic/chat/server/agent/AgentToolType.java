package com.tencent.supersonic.chat.server.agent;

import java.util.HashMap;
import java.util.Map;

public enum AgentToolType {
    DATASET("Text2SQL数据集"), PLUGIN("第三方插件");

    private final String title;

    AgentToolType(String title) {
        this.title = title;
    }

    public static Map<AgentToolType, String> getToolTypes() {
        Map<AgentToolType, String> map = new HashMap<>();
        map.put(DATASET, DATASET.title);
        map.put(PLUGIN, PLUGIN.title);
        return map;
    }
}
