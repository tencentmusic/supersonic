package com.tencent.supersonic.chat.query.metricinterpret;


import lombok.Data;

@Data
public class LLmAnswerReq {

    private String queryText;

    private String pluginOutput;

}
