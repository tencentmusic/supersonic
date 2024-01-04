package com.tencent.supersonic.chat.core.mapper;

import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import java.io.Serializable;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ModelWithSemanticType implements Serializable {

    private Long model;
    private SchemaElementType schemaElementType;

    public ModelWithSemanticType(Long model, SchemaElementType schemaElementType) {
        this.model = model;
        this.schemaElementType = schemaElementType;
    }
}