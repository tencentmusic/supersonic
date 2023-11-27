package com.tencent.supersonic.chat.llm;

import com.tencent.supersonic.chat.parser.plugin.function.FunctionReq;
import com.tencent.supersonic.chat.parser.plugin.function.FunctionResp;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMResp;

/**
 * Unified interpreter for invoking the llm layer.
 */
public interface LLMInterpreter {


    LLMResp query2sql(LLMReq llmReq, String modelClusterKey);

    FunctionResp requestFunction(FunctionReq functionReq);

}
