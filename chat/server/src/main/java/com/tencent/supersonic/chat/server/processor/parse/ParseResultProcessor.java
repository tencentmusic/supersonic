package com.tencent.supersonic.chat.server.processor.parse;

import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;

public interface ParseResultProcessor {

    void process(ParseContext parseContext, ParseResp parseResp);

}
