package com.tencent.supersonic.chat.parser.sql.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SqlExample {

    @JsonProperty("currentDate")
    private String currentDate;

    @JsonProperty("tableName")
    private String tableName;

    @JsonProperty("fieldsList")
    private String fieldsList;

    @JsonProperty("question")
    private String question;

    @JsonProperty("priorSchemaLinks")
    private String priorSchemaLinks;

    @JsonProperty("analysis")
    private String analysis;

    @JsonProperty("schemaLinks")
    private String schemaLinks;

    @JsonProperty("sql")
    private String sql;
}