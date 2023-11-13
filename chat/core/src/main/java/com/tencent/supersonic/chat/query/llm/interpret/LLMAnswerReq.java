package com.tencent.supersonic.chat.query.llm.interpret;


import lombok.Data;

@Data
public class LLMAnswerReq {

    private String queryText;

    private String pluginOutput;

}
