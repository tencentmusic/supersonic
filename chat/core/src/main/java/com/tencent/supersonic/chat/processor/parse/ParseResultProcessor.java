package com.tencent.supersonic.chat.processor.parse;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.processor.ResultProcessor;

/**
 * A ParseResultProcessor wraps things up before returning results to users in parse stage.
 */
public interface ParseResultProcessor extends ResultProcessor {

    void process(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext);

}
