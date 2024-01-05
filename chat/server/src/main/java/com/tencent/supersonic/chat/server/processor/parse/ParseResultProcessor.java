package com.tencent.supersonic.chat.server.processor.parse;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.server.processor.ResultProcessor;

/**
 * A ParseResultProcessor wraps things up before returning results to users in parse stage.
 */
public interface ParseResultProcessor extends ResultProcessor {

    void process(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext);

}
