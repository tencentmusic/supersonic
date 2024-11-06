package com.tencent.supersonic.headless.api.pojo;

import com.google.common.base.Objects;
import com.tencent.supersonic.common.pojo.DimensionConstants;
import com.tencent.supersonic.headless.api.pojo.enums.DimensionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Builder.Default
    private Map<String, Object> extInfo = new HashMap<>();
    private DimensionTimeTypeParams typeParams;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SchemaElement schemaElement = (SchemaElement) o;
        return Objects.equal(dataSetId, schemaElement.dataSetId)
                && Objects.equal(id, schemaElement.id) && Objects.equal(name, schemaElement.name)
                && Objects.equal(bizName, schemaElement.bizName)
                && Objects.equal(type, schemaElement.type);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dataSetId, id, name, bizName, type);
    }

    public boolean isPartitionTime() {
        if (MapUtils.isEmpty(extInfo)) {
            return false;
        }
        Object o = extInfo.get(DimensionConstants.DIMENSION_TYPE);
        DimensionType dimensionTYpe = null;
        if (o instanceof DimensionType) {
            dimensionTYpe = (DimensionType) o;
        }
        if (o instanceof String) {
            dimensionTYpe = DimensionType.valueOf((String) o);
        }
        return DimensionType.isPartitionTime(dimensionTYpe);
    }

    public boolean isPrimaryKey() {
        if (MapUtils.isEmpty(extInfo)) {
            return false;
        }
        Object o = extInfo.get(DimensionConstants.DIMENSION_TYPE);
        DimensionType dimensionType = null;
        if (o instanceof DimensionType) {
            dimensionType = (DimensionType) o;
        }
        if (o instanceof String) {
            dimensionType = DimensionType.valueOf((String) o);
        }
        return DimensionType.isPrimaryKey(dimensionType);
    }

    public String getTimeFormat() {
        if (MapUtils.isEmpty(extInfo)) {
            return null;
        }
        return (String) extInfo.get(DimensionConstants.DIMENSION_TIME_FORMAT);
    }

    public String getPartitionTimeFormat() {
        String timeFormat = getTimeFormat();
        if (StringUtils.isNotBlank(timeFormat) && isPartitionTime()) {
            return timeFormat;
        }
        return "";
    }
}
