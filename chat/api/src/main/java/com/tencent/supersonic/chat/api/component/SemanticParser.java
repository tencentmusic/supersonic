package com.tencent.supersonic.chat.api.component;


import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;

/**
 * A semantic parser understands user queries and extracts semantic information.
 * It could leverage either rule-based or LLM-based approach to identify query intent
 * and extract related semantic items from the query.
 */
public interface SemanticParser {

    void parse(QueryContext queryContext, ChatContext chatContext);
}
