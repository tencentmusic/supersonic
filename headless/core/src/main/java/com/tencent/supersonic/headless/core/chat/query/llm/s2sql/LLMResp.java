package com.tencent.supersonic.headless.core.chat.query.llm.s2sql;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LLMResp {

    private String query;

    private String modelName;

    private String sqlOutput;

    private List<String> fields;

    private Map<String, LLMSqlResp> sqlRespMap;

    /**
     * Only for compatibility with python code, later deleted
     */
    private Map<String, Double> sqlWeight;

}
