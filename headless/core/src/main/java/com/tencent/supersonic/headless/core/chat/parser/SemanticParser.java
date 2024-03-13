package com.tencent.supersonic.headless.core.chat.parser;

import com.tencent.supersonic.headless.core.pojo.ChatContext;
import com.tencent.supersonic.headless.core.pojo.QueryContext;

/**
 * A semantic parser understands user queries and extracts semantic information.
 * It could leverage either rule-based or LLM-based approach to identify query intent
 * and extract related semantic items from the query.
 */
public interface SemanticParser {

    void parse(QueryContext queryContext, ChatContext chatContext);
}
