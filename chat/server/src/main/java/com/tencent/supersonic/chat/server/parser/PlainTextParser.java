package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;


public class PlainTextParser implements ChatParser {

    @Override
    public void parse(ChatParseContext chatParseContext, ParseResp parseResp) {
        if (chatParseContext.getAgent().containsAnyTool()) {
            return;
        }

        SemanticParseInfo parseInfo = new SemanticParseInfo();
        parseInfo.setQueryMode("PLAIN_TEXT");
        parseResp.getSelectedParses().add(parseInfo);
    }

}
