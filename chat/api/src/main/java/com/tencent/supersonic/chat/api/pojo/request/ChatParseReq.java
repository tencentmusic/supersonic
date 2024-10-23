package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
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
    private Integer topN = 10;
    private User user;
    private QueryFilters queryFilters;
    private boolean saveAnswer = true;
    private SchemaMapInfo mapInfo = new SchemaMapInfo();
    private boolean disableLLM = false;
}
