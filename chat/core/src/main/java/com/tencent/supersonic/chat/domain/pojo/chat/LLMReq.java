package com.tencent.supersonic.chat.domain.pojo.chat;

import lombok.Data;

@Data
public class LLMReq {

    private String queryText;
    private LLMSchema schema;

}
