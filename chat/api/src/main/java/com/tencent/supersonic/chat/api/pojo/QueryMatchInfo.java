package com.tencent.supersonic.chat.api.pojo;

import lombok.Data;

@Data
public class QueryMatchInfo {

    SchemaElementType elementType;
    String detectWord;
    private Integer count = 0;
    private double maxSimilarity;
}
