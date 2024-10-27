package com.tencent.supersonic.chat.server.pojo;

import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.server.agent.Agent;
import lombok.Data;

@Data
public class ParseContext {
    private ChatParseReq request;
    private Agent agent;

    public ParseContext(ChatParseReq request) {
        this.request = request;
    }

    public boolean enableNL2SQL() {
        if (agent == null) {
            return false;
        }
        return agent.containsDatasetTool();
    }
}
