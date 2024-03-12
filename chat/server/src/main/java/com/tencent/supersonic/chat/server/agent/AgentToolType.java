package com.tencent.supersonic.chat.server.agent;

import java.util.HashMap;
import java.util.Map;

public enum AgentToolType {
    NL2SQL_RULE("基于规则Text-to-SQL"),
    NL2SQL_LLM("基于大模型Text-to-SQL"),
    PLUGIN("第三方插件");

    private String title;

    AgentToolType(String title) {
        this.title = title;
    }

    public static Map<AgentToolType, String> getToolTypes() {
        Map<AgentToolType, String> map = new HashMap<>();
        map.put(NL2SQL_RULE, NL2SQL_RULE.title);
        map.put(NL2SQL_LLM, NL2SQL_LLM.title);
        map.put(PLUGIN, PLUGIN.title);
        return map;
    }

}
