package com.tencent.supersonic.headless.chat.query.llm.s2sql;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LLMResp {

    private String query;

    private String sideInfo;

    private String dataSet;

    private String schema;

    private String sqlOutput;

    private List<String> fields;

    private Map<String, LLMSqlResp> sqlRespMap;
}
