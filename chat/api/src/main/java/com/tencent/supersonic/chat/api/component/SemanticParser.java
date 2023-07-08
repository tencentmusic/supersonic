package com.tencent.supersonic.chat.api.component;


import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.request.QueryContextReq;

/**
 * This interface defines the contract for a semantic parser that can analyze natural language query
 * and extract meaning from it.
 *
 * The semantic parser uses either rule-based or model-based algorithms to identify query intent
 * and related semantic items described in the query.
 */
public interface SemanticParser {

    void parse(QueryContextReq queryContext, ChatContext chatContext);
}
