package com.tencent.supersonic.chat.server.processor.execute;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.processor.ResultProcessor;

/**
 * A ExecuteResultProcessor wraps things up before returning results to users in execute stage.
 */
public interface ExecuteResultProcessor extends ResultProcessor {

    void process(QueryResult queryResult, SemanticParseInfo semanticParseInfo, ExecuteQueryReq queryReq);

}