package com.tencent.supersonic.chat.server.pojo;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.Data;

@Data
public class ChatExecuteContext {
    private User user;
    private Long queryId;
    private Integer chatId;
    private int parseId;
    private String queryText;
    private boolean saveAnswer;
    private SemanticParseInfo parseInfo;
}
