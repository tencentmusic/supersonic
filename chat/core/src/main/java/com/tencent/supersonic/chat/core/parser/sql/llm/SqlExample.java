package com.tencent.supersonic.chat.core.parser.sql.llm;

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