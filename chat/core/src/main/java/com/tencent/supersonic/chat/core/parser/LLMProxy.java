package com.tencent.supersonic.chat.core.parser;

import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.core.parser.plugin.function.FunctionReq;
import com.tencent.supersonic.chat.core.parser.plugin.function.FunctionResp;
import com.tencent.supersonic.chat.core.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.core.query.llm.s2sql.LLMResp;

/**
 * LLMProxy encapsulates functions performed by LLMs so that multiple
 * orchestration frameworks (e.g. LangChain in python, LangChain4j in java)
 * could be used.
 */
public interface LLMProxy {

    boolean isSkip(QueryContext queryContext);

    LLMResp query2sql(LLMReq llmReq, String modelClusterKey);

    FunctionResp requestFunction(FunctionReq functionReq);

}
