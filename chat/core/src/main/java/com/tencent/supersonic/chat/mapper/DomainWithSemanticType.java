package com.tencent.supersonic.chat.mapper;

import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import java.io.Serializable;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class DomainWithSemanticType implements Serializable {

    private Long domain;
    private SchemaElementType semanticType;

    public DomainWithSemanticType(Long domain, SchemaElementType semanticType) {
        this.domain = domain;
        this.semanticType = semanticType;
    }
}