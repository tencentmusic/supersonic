package com.tencent.supersonic.chat.server.pojo;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import lombok.Data;

@Data
public class ChatParseContext {
    private String queryText;
    private Integer chatId;
    private Agent agent;
    private User user;
    private QueryFilters queryFilters;
    private boolean saveAnswer = true;
    private SchemaMapInfo mapInfo = new SchemaMapInfo();

    public boolean enableNL2SQL() {
        if (agent == null) {
            return true;
        }
        return agent.containsNL2SQLTool();
    }
}
