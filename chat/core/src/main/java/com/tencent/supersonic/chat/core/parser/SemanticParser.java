package com.tencent.supersonic.chat.core.parser;


import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.core.pojo.ChatContext;

/**
 * A semantic parser understands user queries and extracts semantic information.
 * It could leverage either rule-based or LLM-based approach to identify query intent
 * and extract related semantic items from the query.
 */
public interface SemanticParser {

    void parse(QueryContext queryContext, ChatContext chatContext);
}
