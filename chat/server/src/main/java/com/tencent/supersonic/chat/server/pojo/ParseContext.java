package com.tencent.supersonic.chat.server.pojo;

import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import lombok.Data;

import java.util.Objects;

@Data
public class ParseContext {
    private ChatParseReq request;
    private ParseResp response;
    private Agent agent;
    private SemanticParseInfo selectedParseInfo;

    public ParseContext(ChatParseReq request) {
        this.request = request;
        response = new ParseResp(request.getQueryText());
    }

    public boolean enableNL2SQL() {
        return agent.containsDatasetTool();
    }

    public boolean enableFeedback() {
        return agent.enableFeedback() && Objects.isNull(request.getParseId());
    }

    public boolean enableLLM() {
        return !(enableFeedback() || request.isDisableLLM());
    }
}
