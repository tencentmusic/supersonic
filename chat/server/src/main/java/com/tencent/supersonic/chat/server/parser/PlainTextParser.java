package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;

public class PlainTextParser implements ChatQueryParser {

    public boolean accept(ParseContext parseContext) {
        return !parseContext.getAgent().containsAnyTool();
    }

    @Override
    public void parse(ParseContext parseContext) {
        SemanticParseInfo parseInfo = new SemanticParseInfo();
        parseInfo.setQueryMode("PLAIN_TEXT");
        parseInfo.setId(1);
        parseContext.getResponse().getSelectedParses().add(parseInfo);
        parseContext.getResponse().setState(ParseResp.ParseState.COMPLETED);
    }
}
