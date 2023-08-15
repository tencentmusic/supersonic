package com.tencent.supersonic.chat.parser.llm;

import com.tencent.supersonic.chat.plugin.PluginParseResult;
import com.tencent.supersonic.chat.query.dsl.LLMResp;
import lombok.Data;

@Data
public class DSLParseResult extends PluginParseResult {

    private LLMResp llmResp;
}
