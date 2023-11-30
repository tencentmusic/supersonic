package com.tencent.supersonic.chat.processor;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;

/**
 * A ParseResultProcessor wraps things up before returning results to users.
 */
public interface ParseResultProcessor {

    void process(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext);

}
