package com.tencent.supersonic.chat.core.query.llm.analytics;


import lombok.Data;

@Data
public class LLMAnswerReq {

    private String queryText;

    private String pluginOutput;

}
