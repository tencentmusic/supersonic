package com.tencent.supersonic.headless.server.processor;

import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.chat.ChatQueryContext;

/** A ParseResultProcessor wraps things up before returning results to users in parse stage. */
public interface ResultProcessor {

    void process(ParseResp parseResp, ChatQueryContext chatQueryContext);
}
