package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import lombok.Data;

@Data
public class QueryRequest {

    private String queryText;
    private Integer chatId;
    private Long domainId = 0L;
    private User user;
    private QueryFilters queryFilters;
    private boolean saveAnswer = true;
}
