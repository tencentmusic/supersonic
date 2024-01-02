package com.tencent.supersonic.chat.query.llm.s2sql;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LLMSqlResp {

    private double sqlWeight;

    private List<Map<String, String>> fewShots;

}
