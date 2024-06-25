package com.tencent.supersonic.headless.chat.parser.llm;

import lombok.Data;

@Data
public class Exemplar {

    private String question;

    private String questionAugmented;

    private String dbSchema;

    private String sql;

}