package com.tencent.supersonic.chat.processor;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;

/**
 * A response processor wraps things up before responding to users.
 */
public interface ResponseProcessor {

    void process(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext);

}
