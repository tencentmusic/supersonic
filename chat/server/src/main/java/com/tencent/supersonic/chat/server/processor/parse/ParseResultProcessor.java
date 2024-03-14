package com.tencent.supersonic.chat.server.processor.parse;

import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;

public interface ParseResultProcessor {

    void process(ChatParseContext chatParseContext, ParseResp parseResp);

}
