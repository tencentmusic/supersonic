package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class DataSetSchema implements Serializable {

    private String databaseType;
    private String databaseVersion;
    private SchemaElement dataSet;
    private Set<SchemaElement> metrics = new HashSet<>();
    private Set<SchemaElement> dimensions = new HashSet<>();
    private Set<SchemaElement> tags = new HashSet<>();
    private Set<SchemaElement> dimensionValues = new HashSet<>();
    private Set<SchemaElement> terms = new HashSet<>();
    private QueryConfig queryConfig;

    public Long getDataSetId() {
        return dataSet.getDataSetId();
    }

    public SchemaElement getElement(SchemaElementType elementType, long elementID) {
        Optional<SchemaElement> element = Optional.empty();

        switch (elementType) {
            case DATASET:
                element = Optional.of(dataSet);
                break;
            case METRIC:
                element = metrics.stream().filter(e -> e.getId() == elementID).findFirst();
                break;
            case DIMENSION:
                element = dimensions.stream().filter(e -> e.getId() == elementID).findFirst();
                break;
            case VALUE:
                element = dimensionValues.stream().filter(e -> e.getId() == elementID).findFirst();
                break;
            case TAG:
                element = tags.stream().filter(e -> e.getId() == elementID).findFirst();
                break;
            case TERM:
                element = terms.stream().filter(e -> e.getId() == elementID).findFirst();
                break;
            default:
        }

        return element.orElse(null);
    }

    public Map<String, String> getBizNameToName() {
        List<SchemaElement> allElements = new ArrayList<>();
        allElements.addAll(getDimensions());
        allElements.addAll(getMetrics());
        return allElements.stream().collect(Collectors.toMap(SchemaElement::getBizName,
                SchemaElement::getName, (k1, k2) -> k1));
    }

    public TimeDefaultConfig getDetailTypeTimeDefaultConfig() {
        if (queryConfig == null) {
            return null;
        }
        if (queryConfig.getDetailTypeDefaultConfig() == null) {
            return null;
        }
        return queryConfig.getDetailTypeDefaultConfig().getTimeDefaultConfig();
    }

    public TimeDefaultConfig getMetricTypeTimeDefaultConfig() {
        if (queryConfig == null) {
            return null;
        }
        if (queryConfig.getAggregateTypeDefaultConfig() == null) {
            return null;
        }
        return queryConfig.getAggregateTypeDefaultConfig().getTimeDefaultConfig();
    }

    public boolean containsPartitionDimensions() {
        return dimensions.stream().anyMatch(SchemaElement::isPartitionTime);
    }

    public SchemaElement getPartitionDimension() {
        return dimensions.stream().filter(SchemaElement::isPartitionTime).findFirst().orElse(null);
    }

    public SchemaElement getPrimaryKey() {
        for (SchemaElement dimension : dimensions) {
            if (dimension.isPrimaryKey()) {
                return dimension;
            }
        }
        return null;
    }

    public String getPartitionTimeFormat() {
        for (SchemaElement dimension : dimensions) {
            String partitionTimeFormat = dimension.getPartitionTimeFormat();
            if (StringUtils.isNotBlank(partitionTimeFormat)) {
                return partitionTimeFormat;
            }
        }
        return null;
    }
}
