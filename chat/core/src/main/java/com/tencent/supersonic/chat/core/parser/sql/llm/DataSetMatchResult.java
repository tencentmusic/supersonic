package com.tencent.supersonic.chat.core.parser.sql.llm;

import lombok.Data;

@Data
public class DataSetMatchResult {
    private Integer count = 0;
    private double maxSimilarity;
}
