package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;

public interface ChatParser {

    void parse(ChatParseContext chatParseContext, ParseResp parseResp);

}
