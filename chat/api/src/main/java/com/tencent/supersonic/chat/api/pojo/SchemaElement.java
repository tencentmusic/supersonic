package com.tencent.supersonic.chat.api.pojo;

import com.google.common.base.Objects;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Data
@Getter
@Builder
@NoArgsConstructor
public class SchemaElement implements Serializable {

    private Long model;
    private Long id;
    private String name;
    private String bizName;
    private Long useCnt;
    private SchemaElementType type;
    private List<String> alias;

    public SchemaElement(Long model, Long id, String name, String bizName,
                         Long useCnt, SchemaElementType type, List<String> alias) {
        this.model = model;
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
        return Objects.equal(model, schemaElement.model) && Objects.equal(id,
                schemaElement.id) && Objects.equal(name, schemaElement.name)
                && Objects.equal(bizName, schemaElement.bizName) && Objects.equal(
                useCnt, schemaElement.useCnt) && Objects.equal(type, schemaElement.type);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(model, id, name, bizName, useCnt, type);
    }
}
