package com.tencent.supersonic.headless.chat;


import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.Data;

@Data
public class ChatContext {

    private Integer chatId;
    private String queryText;
    private SemanticParseInfo parseInfo = new SemanticParseInfo();
    private String user;
}
