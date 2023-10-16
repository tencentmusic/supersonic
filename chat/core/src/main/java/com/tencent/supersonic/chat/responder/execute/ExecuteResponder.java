package com.tencent.supersonic.chat.responder.execute;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;

public interface ExecuteResponder {

    void fillResponse(QueryResult queryResult, SemanticParseInfo semanticParseInfo, ExecuteQueryReq queryReq);

}