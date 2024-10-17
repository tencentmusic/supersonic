package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class DataSetWithSemanticType implements Serializable {

    private Long dataSetId;
    private SchemaElementType schemaElementType;

    public DataSetWithSemanticType(Long dataSetId, SchemaElementType schemaElementType) {
        this.dataSetId = dataSetId;
        this.schemaElementType = schemaElementType;
    }
}
