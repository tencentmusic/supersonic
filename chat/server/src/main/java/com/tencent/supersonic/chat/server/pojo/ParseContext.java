package com.tencent.supersonic.chat.server.pojo;

import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import lombok.Data;

@Data
public class ParseContext {
    private ChatParseReq request;
    private ParseResp response;
    private Agent agent;

    public ParseContext(ChatParseReq request) {
        this.request = request;
        response = new ParseResp(request.getQueryText());
    }

    public boolean enableNL2SQL() {
        if (agent == null) {
            return false;
        }
        return agent.containsDatasetTool();
    }
}
