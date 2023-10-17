package com.tencent.supersonic.chat.parser.llm.s2ql;

import com.tencent.supersonic.chat.agent.tool.CommonAgentTool;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.query.llm.s2ql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2ql.LLMResp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParseResult {

    private LLMReq llmReq;

    private LLMResp llmResp;

    private QueryReq request;

    private CommonAgentTool commonAgentTool;
}
