package com.tencent.supersonic.chat.api.pojo.request;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ExecuteQueryReq {
    private User user;
    private Integer agentId;
    private Integer chatId;
    private String queryText;
    private Long queryId = 7L;
    private Integer parseId = 2;
    private SemanticParseInfo parseInfo;
    private boolean saveAnswer = true;
}
