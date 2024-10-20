package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.common.pojo.User;
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
    private Integer agentId;
    private Long queryId;
    private Integer chatId;
    private int parseId;
    private String queryText;
    private boolean saveAnswer;
}
