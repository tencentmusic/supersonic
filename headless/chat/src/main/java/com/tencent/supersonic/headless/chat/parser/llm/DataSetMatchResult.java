package com.tencent.supersonic.headless.chat.parser.llm;

import lombok.Data;

@Data
public class DataSetMatchResult {
    private Integer count = 0;
    private double maxSimilarity;
}
