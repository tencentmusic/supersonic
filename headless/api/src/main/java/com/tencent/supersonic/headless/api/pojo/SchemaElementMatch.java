package com.tencent.supersonic.headless.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchemaElementMatch {
    SchemaElement element;
    double offset;
    double similarity;
    String detectWord;
    String word;
    Long frequency;
    boolean isInherited;

    public boolean isFullMatched() {
        return 1.0 == similarity;
    }
}
