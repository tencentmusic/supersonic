package com.tencent.supersonic.chat.server.pojo;

import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.server.agent.Agent;
import lombok.Data;

import java.util.Objects;

@Data
public class ParseContext {
    private ChatParseReq request;
    private ChatParseResp response;
    private Agent agent;

    public ParseContext(ChatParseReq request, ChatParseResp response) {
        this.request = request;
        this.response = response;
    }

    public boolean enableNL2SQL() {
        return Objects.nonNull(agent) && agent.containsDatasetTool();
    }

    public boolean enableLLM() {
        return !request.isDisableLLM();
    }

    public boolean needFeedback() {
        return agent.enableFeedback() && (Objects.isNull(request.getSelectedParse())
                && response.getSelectedParses().size() > 1);
    }

    public boolean needLLMParse() {
        return enableLLM() && (Objects.nonNull(request.getSelectedParse())
                || !response.getSelectedParses().isEmpty());
    }
}
