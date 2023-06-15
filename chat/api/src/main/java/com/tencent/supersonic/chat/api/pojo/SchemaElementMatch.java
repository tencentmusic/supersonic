package com.tencent.supersonic.chat.api.pojo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SchemaElementMatch {

    SchemaElementType elementType;

    int elementID;

    double similarity;

    String detectWord;

    String word;

    Long frequency;

    public SchemaElementMatch() {
    }
}
