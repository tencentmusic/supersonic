package com.tencent.supersonic.headless.api.pojo.request;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ExecuteQueryReq {
    private User user;
    private Long queryId;
    private Integer chatId;
    private String queryText;
    private SemanticParseInfo parseInfo;
    private boolean saveAnswer;
}
