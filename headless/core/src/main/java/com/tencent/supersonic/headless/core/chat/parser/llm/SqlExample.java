package com.tencent.supersonic.headless.core.chat.parser.llm;

import lombok.Data;

@Data
public class SqlExample {

    private String question;

    private String questionAugmented;

    private String dbSchema;

    private String sql;

    private String generatedSchemaLinkingCoT;

    private String generatedSchemaLinkings;
}