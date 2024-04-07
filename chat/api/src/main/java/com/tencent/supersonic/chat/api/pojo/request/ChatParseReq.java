package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import lombok.Data;

@Data
public class ChatParseReq {
    private String queryText;
    private Integer chatId;
    private Integer agentId;
    private Integer topN = 10;
    private User user;
    private QueryFilters queryFilters;
    private boolean saveAnswer = true;
    private SchemaMapInfo mapInfo = new SchemaMapInfo();

}
