package com.tencent.supersonic.chat.parser.plugin.function;

import lombok.Data;

@Data
public class ModelMatchResult {
    private Integer count = 0;
    private double maxSimilarity;
}
