package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ExecuteQueryReq {
    private User user;
    private Long queryId;
    private String queryText;
    private SemanticParseInfo parseInfo;
    private boolean saveAnswer;
}
