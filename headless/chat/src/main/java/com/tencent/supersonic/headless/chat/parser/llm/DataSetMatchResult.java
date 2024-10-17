package com.tencent.supersonic.headless.chat.parser.llm;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataSetMatchResult {
    private double maxMetricSimilarity;
    private double maxDatesetSimilarity;
    private double totalSimilarity;
    private Long maxMetricUseCnt;
}
