package com.tencent.supersonic.headless.core.chat.parser.llm;

import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMResp;

/**
 * Sql Generation interface, generating SQL using a large model.
 */
public interface SqlGeneration {

    /***
     * generate llmResp (sql, schemaLink, prompt, etc.) through LLMReq.
     * @param llmReq
     * @param dataSetId
     * @return
     */
    LLMResp generation(LLMReq llmReq, Long dataSetId);

}
