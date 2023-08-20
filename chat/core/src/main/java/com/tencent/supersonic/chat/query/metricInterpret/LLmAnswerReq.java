package com.tencent.supersonic.chat.query.metricInterpret;


import lombok.Data;

@Data
public class LLmAnswerReq {

    private String queryText;

    private String pluginOutput;

}
