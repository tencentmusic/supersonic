package com.tencent.supersonic.chat.parser.function;

import lombok.Data;

@Data
public class DomainMatchResult {
    private Integer count = 0;
    private double maxSimilarity;
}
