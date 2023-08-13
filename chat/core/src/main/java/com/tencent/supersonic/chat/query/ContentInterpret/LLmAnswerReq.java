package com.tencent.supersonic.chat.query.ContentInterpret;


import lombok.Data;

@Data
public class LLmAnswerReq {

    private String queryText;

    private String pluginOutput;

}
