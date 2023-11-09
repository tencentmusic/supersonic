package com.tencent.supersonic.chat.query.llm.s2sql;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class LLMResp {

    private String query;

    private String modelName;

    private String sqlOutput;

    private List<String> fields;

    private String schemaLinkingOutput;

    private String schemaLinkStr;

    private Map<String, Double> sqlWeight;
}
