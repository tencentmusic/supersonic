package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;

public class PlainTextParser implements ChatQueryParser {

    @Override
    public void parse(ParseContext parseContext, ParseResp parseResp) {
        if (parseContext.getAgent().containsAnyTool()) {
            return;
        }

        SemanticParseInfo parseInfo = new SemanticParseInfo();
        parseInfo.setQueryMode("PLAIN_TEXT");
        parseResp.getSelectedParses().add(parseInfo);
        parseResp.setState(ParseResp.ParseState.COMPLETED);
    }
}
