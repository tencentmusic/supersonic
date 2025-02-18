package com.tencent.supersonic.headless.api.pojo;

import lombok.*;

import java.io.Serializable;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchemaElementMatch implements Serializable {
    private SchemaElement element;
    private double offset;
    private double similarity;
    private String detectWord;
    private String word;
    private Long frequency;
    private boolean isInherited;
    private boolean llmMatched;

    public boolean isFullMatched() {
        return 1.0 == similarity;
    }
}
