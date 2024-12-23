package com.tencent.supersonic.chat.server.pojo;

import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.Data;

@Data
public class ExecuteContext {
    private ChatExecuteReq request;
    private QueryResult response;
    private Agent agent;
    private SemanticParseInfo parseInfo;

    public ExecuteContext(ChatExecuteReq request) {
        this.request = request;
    }
}
