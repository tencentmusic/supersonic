package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import lombok.Data;

@Data
public class QueryReq {
    private String queryText;
    private Integer chatId;
    private Long modelId = 0L;
    private User user;
    private QueryFilters queryFilters;
    private boolean saveAnswer = true;
    private Integer agentId;
}
