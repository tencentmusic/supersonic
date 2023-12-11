package com.tencent.supersonic.chat.parser.sql.llm;


import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;

/**
 * Sql Generation
 */
public interface SqlGeneration {

    String generation(LLMReq llmReq, String modelClusterKey);

}
