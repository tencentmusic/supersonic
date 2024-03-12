package com.tencent.supersonic.headless.core.chat.mapper;

import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

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