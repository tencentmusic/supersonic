package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.common.pojo.User;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatExecuteReq {
    private User user;
    @NotNull(message = "agentId must not be null")
    private Integer agentId;
    @NotNull(message = "queryId must not be null")
    private Long queryId;
    @NotNull(message = "chatId must not be null")
    private Integer chatId;
    private int parseId;
    private String queryText;
    private boolean saveAnswer;
    private boolean streamingResult;
}
