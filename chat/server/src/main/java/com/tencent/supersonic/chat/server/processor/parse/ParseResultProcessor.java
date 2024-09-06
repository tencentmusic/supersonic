package com.tencent.supersonic.chat.server.processor.parse;

import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.chat.server.processor.ResultProcessor;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;

/** A ParseResultProcessor wraps things up before returning parsing results to the users. */
public interface ParseResultProcessor extends ResultProcessor {

    void process(ParseContext parseContext, ParseResp parseResp);
}
