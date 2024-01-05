package com.tencent.supersonic.chat.core.parser.sql.llm;


import com.tencent.supersonic.chat.core.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.core.query.llm.s2sql.LLMResp;

/**
 * Sql Generation interface, generating SQL using a large model.
 */
public interface SqlGeneration {

    /***
     * generate llmResp (sql, schemaLink, prompt, etc.) through LLMReq.
     * @param llmReq
     * @param modelClusterKey
     * @return
     */
    LLMResp generation(LLMReq llmReq, String modelClusterKey);

}
