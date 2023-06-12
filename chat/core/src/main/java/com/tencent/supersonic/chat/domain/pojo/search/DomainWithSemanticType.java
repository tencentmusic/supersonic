package com.tencent.supersonic.chat.domain.pojo.search;

import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import java.io.Serializable;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class DomainWithSemanticType implements Serializable {

    private Integer domain;
    private SchemaElementType semanticType;

    public DomainWithSemanticType(Integer domain, SchemaElementType semanticType) {
        this.domain = domain;
        this.semanticType = semanticType;
    }
}