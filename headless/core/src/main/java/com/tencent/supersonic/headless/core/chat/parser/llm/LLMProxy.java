package com.tencent.supersonic.headless.core.chat.parser.llm;


import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMResp;

/**
 * LLMProxy encapsulates functions performed by LLMs so that multiple
 * orchestration frameworks (e.g. LangChain in python, LangChain4j in java)
 * could be used.
 */
public interface LLMProxy {

    LLMResp text2sql(LLMReq llmReq);

}
