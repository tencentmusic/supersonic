package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatParseReq {
    private String queryText;
    private Integer chatId;
    private Integer agentId;
    private Long dataSetId;
    private User user;
    private QueryFilters queryFilters;
    private boolean saveAnswer = true;
    private boolean disableLLM = false;
    private Long queryId;
    private SemanticParseInfo selectedParse;
}
