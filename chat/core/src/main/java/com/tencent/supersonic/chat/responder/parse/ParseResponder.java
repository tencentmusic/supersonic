package com.tencent.supersonic.chat.responder.parse;

import com.tencent.supersonic.chat.api.pojo.response.QueryResp;

public interface ParseResponder {

    void fillResponse(QueryResp queryResp);

}
