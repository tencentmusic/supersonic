package com.tencent.supersonic.chat.server.processor.parse;

import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;

public interface ParseResultProcessor {

    void process(ParseResp parseResp, ChatParseReq chatParseReq);

}
