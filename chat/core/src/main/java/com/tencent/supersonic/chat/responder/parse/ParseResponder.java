package com.tencent.supersonic.chat.responder.parse;

import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;

public interface ParseResponder {

    void fillResponse(ParseResp parseResp, QueryContext queryContext);

}