package com.tencent.supersonic.headless.core.chat.parser;

import com.tencent.supersonic.headless.core.pojo.ChatContext;
import com.tencent.supersonic.headless.core.pojo.QueryContext;

/**
 * A semantic parser understands user queries and generates semantic query statement.
 * SuperSonic leverages a combination of rule-based and LLM-based parsers,
 * each of which deals with specific scenarios.
 */
public interface SemanticParser {

    void parse(QueryContext queryContext, ChatContext chatContext);
}
