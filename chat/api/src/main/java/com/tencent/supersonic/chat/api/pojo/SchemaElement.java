package com.tencent.supersonic.chat.api.pojo;

import com.google.common.base.Objects;

import java.io.Serializable;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SchemaElement implements Serializable {

    private Long domain;
    private Long id;
    private String name;
    private String bizName;
    private Long useCnt;
    private SchemaElementType type;
    private List<String> alias;

    public SchemaElement() {
    }

    public SchemaElement(Long domain, Long id, String name, String bizName,
                         Long useCnt, SchemaElementType type, List<String> alias) {
        this.domain = domain;
        this.id = id;
        this.name = name;
        this.bizName = bizName;
        this.useCnt = useCnt;
        this.type = type;
        this.alias = alias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SchemaElement schemaElement = (SchemaElement) o;
        return Objects.equal(domain, schemaElement.domain) && Objects.equal(id,
                schemaElement.id) && Objects.equal(name, schemaElement.name)
                && Objects.equal(bizName, schemaElement.bizName) && Objects.equal(
                useCnt, schemaElement.useCnt) && Objects.equal(type, schemaElement.type);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(domain, id, name, bizName, useCnt, type);
    }
}
