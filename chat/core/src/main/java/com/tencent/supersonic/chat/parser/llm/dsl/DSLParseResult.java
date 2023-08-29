package com.tencent.supersonic.chat.parser.llm.dsl;

import com.tencent.supersonic.chat.agent.tool.DslTool;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.query.llm.dsl.LLMReq;
import com.tencent.supersonic.chat.query.llm.dsl.LLMResp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DSLParseResult {

    private LLMReq llmReq;

    private LLMResp llmResp;

    private QueryReq request;

    private DslTool dslTool;
}
