package com.tencent.supersonic.chat.core.query.llm.s2sql;

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LLMSqlResp {

    private double sqlWeight;

    private List<Map<String, String>> fewShots;

}
