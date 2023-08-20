package com.tencent.supersonic.chat.parser.llm.dsl;

import com.tencent.supersonic.chat.agent.tool.DslTool;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.plugin.PluginParseResult;
import com.tencent.supersonic.chat.query.dsl.LLMResp;
import lombok.Data;

@Data
public class DSLParseResult {

    private LLMResp llmResp;

    private QueryReq request;

    private DslTool dslTool;
}
