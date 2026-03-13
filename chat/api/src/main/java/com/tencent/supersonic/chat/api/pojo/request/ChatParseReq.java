package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatParseReq {
    @NotBlank(message = "queryText must not be blank")
    private String queryText;
    @NotNull(message = "chatId must not be null")
    private Integer chatId;
    @NotNull(message = "agentId must not be null")
    private Integer agentId;
    private Long dataSetId;
    private User user;
    private QueryFilters queryFilters;
    private boolean saveAnswer = true;
    private boolean disableLLM = false;
    private Long queryId;
    private SemanticParseInfo selectedParse;
}
