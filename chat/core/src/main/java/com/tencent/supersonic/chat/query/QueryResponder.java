package com.tencent.supersonic.chat.query;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;

/**
 * A query responder fill some auxiliary information into query results.
 */
public interface QueryResponder {

    void fillInfo(QueryResult queryResult, SemanticParseInfo semanticParseInfo, ExecuteQueryReq queryReq);

}