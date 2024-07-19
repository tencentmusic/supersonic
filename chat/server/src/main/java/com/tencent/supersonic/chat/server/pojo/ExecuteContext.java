package com.tencent.supersonic.chat.server.pojo;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.Data;

@Data
public class ExecuteContext {
    private User user;
    private String queryText;
    private Agent agent;
    private Integer chatId;
    private Long queryId;
    private boolean saveAnswer;
    private SemanticParseInfo parseInfo;
}
