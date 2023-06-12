package com.tencent.supersonic.chat.api.pojo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SchemaElementMatch {

    SchemaElementType elementType;
    int elementID;
    double similarity;
    String word;

    public SchemaElementMatch() {
    }

    public SchemaElementMatch(SchemaElementType schemaElementType, int elementID, double similarity, String word) {
        this.elementID = elementID;
        this.elementType = schemaElementType;
        this.similarity = similarity;
        this.word = word;
    }
}
