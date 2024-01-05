package com.tencent.supersonic.chat.core.pojo;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import lombok.Data;

@Data
public class ChatContext {

    private Integer chatId;
    private Integer agentId;
    private String queryText;
    private SemanticParseInfo parseInfo = new SemanticParseInfo();
    private String user;
}
