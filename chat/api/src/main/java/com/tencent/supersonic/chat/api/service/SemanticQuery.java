package com.tencent.supersonic.chat.api.service;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementCount;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.response.QueryResultResp;

import java.io.Serializable;
import java.util.List;

/**
 * This interface defines the contract for a semantic query that executes specific type of
 * query based on the results of semantic parsing.
 */
public interface SemanticQuery extends Serializable {

    String getQueryMode();

    QueryResultResp execute(QueryContextReq queryCtx, ChatContext chatCtx) throws Exception;

    SchemaElementCount match(List<SchemaElementMatch> elementMatches, QueryContextReq queryCtx);

    void updateContext(QueryResultResp queryResponse, ChatContext chatCtx, QueryContextReq queryCtx);

    SemanticParseInfo getContext(ChatContext chatCtx, QueryContextReq queryCtx);

}
