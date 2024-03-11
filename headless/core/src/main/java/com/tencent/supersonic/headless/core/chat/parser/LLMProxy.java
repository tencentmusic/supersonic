package com.tencent.supersonic.headless.core.chat.parser;


import com.tencent.supersonic.headless.core.pojo.QueryContext;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMResp;

/**
 * LLMProxy encapsulates functions performed by LLMs so that multiple
 * orchestration frameworks (e.g. LangChain in python, LangChain4j in java)
 * could be used.
 */
public interface LLMProxy {

    boolean isSkip(QueryContext queryContext);

    LLMResp query2sql(LLMReq llmReq, Long dataSetId);

}
