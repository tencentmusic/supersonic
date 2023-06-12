package com.tencent.supersonic.chat.api.request;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import lombok.Data;

@Data
public class QueryContextReq {

    private String queryText;
    private Integer chatId;
    private Integer domainId = 0;
    private User user;
    private SemanticParseInfo parseInfo = new SemanticParseInfo();
    private SchemaMapInfo mapInfo = new SchemaMapInfo();
    private boolean saveAnswer = true;
}
