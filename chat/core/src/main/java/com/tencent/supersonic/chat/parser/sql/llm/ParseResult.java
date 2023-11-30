package com.tencent.supersonic.chat.parser.sql.llm;

import com.tencent.supersonic.chat.agent.NL2SQLTool;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq.ElementValue;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.common.pojo.ModelCluster;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParseResult {

    private ModelCluster modelCluster;

    private LLMReq llmReq;

    private LLMResp llmResp;

    private QueryReq request;

    private NL2SQLTool commonAgentTool;

    private List<ElementValue> linkingValues;
}
