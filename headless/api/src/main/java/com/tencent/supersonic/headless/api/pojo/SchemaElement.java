package com.tencent.supersonic.headless.api.pojo;

import com.google.common.base.Objects;
import com.tencent.supersonic.common.pojo.DimensionConstants;
import com.tencent.supersonic.headless.api.pojo.enums.DimensionType;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.MapUtils;

@Data
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchemaElement implements Serializable {

    private Long dataSetId;
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
    private boolean descriptionMapped;
    @Builder.Default
    private Map<String, Object> extInfo = new HashMap<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SchemaElement schemaElement = (SchemaElement) o;
        return Objects.equal(dataSetId, schemaElement.dataSetId) && Objects.equal(id,
                schemaElement.id) && Objects.equal(name, schemaElement.name)
                && Objects.equal(bizName, schemaElement.bizName)
                && Objects.equal(type, schemaElement.type);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dataSetId, id, name, bizName, type);
    }

    public boolean containsPartitionTime() {
        if (MapUtils.isEmpty(extInfo)) {
            return false;
        }
        DimensionType dimensionTYpe = (DimensionType) extInfo.get(DimensionConstants.DIMENSION_TYPE);
        return DimensionType.isPartitionTime(dimensionTYpe);
    }

}
