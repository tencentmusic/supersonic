package com.tencent.supersonic.headless.chat.parser.llm;

import lombok.Data;

@Data
public class DataSetMatchResult {
    private Integer count = 0;
    private double maxSimilarity;
    private double totalSimilarity;
    private Integer countOfOneSimilarity = 0;

    public void incrementCountOfOneSimilarity() {
        this.countOfOneSimilarity++;
    }

    public void addSimilarity(double similarity) {
        this.totalSimilarity += similarity;
    }
}
