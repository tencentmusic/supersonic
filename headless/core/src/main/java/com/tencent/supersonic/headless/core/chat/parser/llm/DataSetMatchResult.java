package com.tencent.supersonic.headless.core.chat.parser.llm;

import lombok.Data;

@Data
public class DataSetMatchResult {
    private Integer count = 0;
    private double maxSimilarity;
}
