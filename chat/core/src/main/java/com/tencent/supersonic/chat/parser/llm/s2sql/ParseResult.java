package com.tencent.supersonic.chat.parser.llm.s2sql;

import com.tencent.supersonic.chat.agent.tool.CommonAgentTool;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMResp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParseResult {

    private Long modelId;

    private LLMReq llmReq;

    private LLMResp llmResp;

    private QueryReq request;

    private CommonAgentTool commonAgentTool;
}
