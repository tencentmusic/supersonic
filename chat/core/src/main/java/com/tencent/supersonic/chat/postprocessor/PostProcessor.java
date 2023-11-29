package com.tencent.supersonic.chat.postprocessor;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;

/**
 * A post processor do some logic after parser and corrector
 */

public interface PostProcessor {

    void process(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext);

}
