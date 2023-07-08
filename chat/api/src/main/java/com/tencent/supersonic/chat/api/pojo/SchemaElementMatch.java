package com.tencent.supersonic.chat.api.pojo;

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

    SchemaElementType elementType;

    int elementID;

    double similarity;

    String detectWord;

    String word;

    Long frequency;
}
