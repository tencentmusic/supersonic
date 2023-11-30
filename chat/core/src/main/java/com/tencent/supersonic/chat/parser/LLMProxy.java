package com.tencent.supersonic.chat.parser;

import com.tencent.supersonic.chat.parser.plugin.function.FunctionReq;
import com.tencent.supersonic.chat.parser.plugin.function.FunctionResp;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMResp;

/**
 * LLMProxy encapsulates functions performed by LLMs so that multiple
 * orchestration frameworks (e.g. LangChain in python, LangChain4j in java)
 * could be used.
 */
public interface LLMProxy {

    LLMResp query2sql(LLMReq llmReq, String modelClusterKey);

    FunctionResp requestFunction(FunctionReq functionReq);

}
