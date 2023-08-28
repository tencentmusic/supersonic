package com.tencent.supersonic.chat.mapper;

import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import java.io.Serializable;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ModelWithSemanticType implements Serializable {

    private Long model;
    private SchemaElementType semanticType;

    public ModelWithSemanticType(Long model, SchemaElementType semanticType) {
        this.model = model;
        this.semanticType = semanticType;
    }
}