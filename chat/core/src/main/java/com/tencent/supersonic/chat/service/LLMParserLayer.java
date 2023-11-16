package com.tencent.supersonic.chat.service;

import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMResp;

/**
 * Unified wrapper for invoking the llmparser Python service layer.
 */
public interface LLMParserLayer {

    LLMResp query2sql(LLMReq llmReq, Long modelId);

}
