package com.tencent.supersonic.chat.server.pojo;

import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import lombok.Data;

@Data
public class ParseContext {
    private User user;
    private String queryText;
    private Agent agent;
    private Integer chatId;
    private QueryFilters queryFilters;
    private boolean saveAnswer = true;
    private SchemaMapInfo mapInfo;
    private boolean disableLLM = false;

    public boolean enableNL2SQL() {
        if (agent == null) {
            return false;
        }
        return agent.containsDatasetTool();
    }
}
