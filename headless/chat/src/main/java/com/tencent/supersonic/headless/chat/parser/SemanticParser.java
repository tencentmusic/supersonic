package com.tencent.supersonic.headless.chat.parser;

import com.tencent.supersonic.headless.chat.ChatContext;
import com.tencent.supersonic.headless.chat.QueryContext;

/**
 * A semantic parser understands user queries and generates semantic query statement.
 * SuperSonic leverages a combination of rule-based and LLM-based parsers,
 * each of which deals with specific scenarios.
 */
public interface SemanticParser {

    void parse(QueryContext queryContext, ChatContext chatContext);
}
