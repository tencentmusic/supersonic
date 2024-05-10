package com.tencent.supersonic.headless.api.pojo;

import com.google.common.base.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchemaElement implements Serializable {

    private Long dataSet;
    private String dataSetName;
    private Long model;
    private Long id;
    private String name;
    private String bizName;
    private Long useCnt;
    private SchemaElementType type;
    private List<String> alias;
    private List<SchemaValueMap> schemaValueMaps;
    private List<RelatedSchemaElement> relatedSchemaElements;
    private String defaultAgg;
    private String dataFormatType;
    private double order;
    private int isTag;
    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SchemaElement schemaElement = (SchemaElement) o;
        return Objects.equal(dataSet, schemaElement.dataSet) && Objects.equal(id,
                schemaElement.id) && Objects.equal(name, schemaElement.name)
                && Objects.equal(bizName, schemaElement.bizName)
                && Objects.equal(type, schemaElement.type);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dataSet, id, name, bizName, type);
    }

}
