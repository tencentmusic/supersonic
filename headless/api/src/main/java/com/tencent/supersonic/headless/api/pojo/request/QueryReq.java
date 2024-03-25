package com.tencent.supersonic.headless.api.pojo.request;

import com.google.common.collect.Sets;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import lombok.Data;

import java.util.Set;

@Data
public class QueryReq {
    private String queryText;
    private Integer chatId;
    private Set<Long> dataSetIds = Sets.newHashSet();
    private User user;
    private QueryFilters queryFilters;
    private boolean saveAnswer = true;
}
