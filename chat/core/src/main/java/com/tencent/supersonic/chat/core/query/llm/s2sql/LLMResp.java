package com.tencent.supersonic.chat.core.query.llm.s2sql;

import java.util.List;
import java.util.Map;
import lombok.Data;

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
