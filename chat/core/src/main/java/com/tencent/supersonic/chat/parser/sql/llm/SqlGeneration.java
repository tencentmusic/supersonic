package com.tencent.supersonic.chat.parser.sql.llm;


import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import java.util.Map;

/**
 * Sql Generation interface, generating SQL using a large model.
 */
public interface SqlGeneration {

    /***
     * generate SQL through LLMReq.
     * @param llmReq
     * @param modelClusterKey
     * @return
     */
    Map<String, Double> generation(LLMReq llmReq, String modelClusterKey);

}
