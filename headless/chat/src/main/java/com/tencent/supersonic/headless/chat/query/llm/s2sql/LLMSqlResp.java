package com.tencent.supersonic.headless.chat.query.llm.s2sql;

import com.tencent.supersonic.common.pojo.SqlExemplar;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LLMSqlResp {

    private double sqlWeight;

    private List<SqlExemplar> fewShots;

}
