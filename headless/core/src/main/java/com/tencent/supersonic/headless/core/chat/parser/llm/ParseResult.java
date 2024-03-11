package com.tencent.supersonic.headless.core.chat.parser.llm;

import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq.ElementValue;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMResp;
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

    private Long dataSetId;

    private LLMReq llmReq;

    private LLMResp llmResp;

    private QueryReq request;

    private List<ElementValue> linkingValues;
}
