package com.tencent.supersonic.headless.api.pojo;

import com.google.common.base.Objects;
import com.tencent.supersonic.common.pojo.DimensionConstants;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
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
    @Builder.Default
    private Integer sensitiveLevel = SensitiveLevelEnum.LOW.getCode();

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
        DimensionType dimensionType = getDimensionType();
        return DimensionType.isPartitionTime(dimensionType);
    }

    public boolean isTimeDimension() {
        DimensionType dimensionType = getDimensionType();
        return DimensionType.isTimeDimension(dimensionType);
    }

    private DimensionType getDimensionType() {
        if (MapUtils.isEmpty(extInfo)) {
            return null;
        }
        Object o = extInfo.get(DimensionConstants.DIMENSION_TYPE);
        if (o instanceof DimensionType) {
            return (DimensionType) o;
        }
        if (o instanceof String) {
            return DimensionType.valueOf((String) o);
        }
        return null;
    }

    public boolean isPrimaryKey() {
        DimensionType dimensionType = getDimensionType();
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
