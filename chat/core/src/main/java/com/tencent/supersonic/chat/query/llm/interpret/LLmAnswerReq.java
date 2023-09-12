package com.tencent.supersonic.chat.query.llm.interpret;


import lombok.Data;

@Data
public class LLmAnswerReq {

    private String queryText;

    private String pluginOutput;

}
