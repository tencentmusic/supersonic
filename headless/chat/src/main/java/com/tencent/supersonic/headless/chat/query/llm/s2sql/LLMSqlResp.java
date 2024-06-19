package com.tencent.supersonic.headless.chat.query.llm.s2sql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LLMSqlResp {

    private double sqlWeight;

    private List<Map<String, String>> fewShots;

}
