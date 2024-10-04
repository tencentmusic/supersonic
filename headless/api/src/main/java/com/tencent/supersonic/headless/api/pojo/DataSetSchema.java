package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class DataSetSchema {

    private String databaseType;
    private SchemaElement dataSet;
    private Set<SchemaElement> metrics = new HashSet<>();
    private Set<SchemaElement> dimensions = new HashSet<>();
    private Set<SchemaElement> tags = new HashSet<>();
    private Set<SchemaElement> dimensionValues = new HashSet<>();
    private Set<SchemaElement> terms = new HashSet<>();
    private SchemaElement entity = new SchemaElement();
    private QueryConfig queryConfig;

    public SchemaElement getElement(SchemaElementType elementType, long elementID) {
        Optional<SchemaElement> element = Optional.empty();

        switch (elementType) {
            case ENTITY:
                element = Optional.ofNullable(entity);
                break;
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

        if (element.isPresent()) {
            return element.get();
        } else {
            return null;
        }
    }

    public Map<String, String> getBizNameToName() {
        List<SchemaElement> allElements = new ArrayList<>();
        allElements.addAll(getDimensions());
        allElements.addAll(getMetrics());
        return allElements.stream().collect(Collectors.toMap(SchemaElement::getBizName,
                SchemaElement::getName, (k1, k2) -> k1));
    }

    public TimeDefaultConfig getTagTypeTimeDefaultConfig() {
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

    public DetailTypeDefaultConfig getTagTypeDefaultConfig() {
        if (queryConfig == null) {
            return null;
        }
        return queryConfig.getDetailTypeDefaultConfig();
    }

    public List<SchemaElement> getTagDefaultDimensions() {
        DetailTypeDefaultConfig detailTypeDefaultConfig = getTagTypeDefaultConfig();
        if (Objects.isNull(detailTypeDefaultConfig)
                || Objects.isNull(detailTypeDefaultConfig.getDefaultDisplayInfo())) {
            return new ArrayList<>();
        }
        if (CollectionUtils
                .isNotEmpty(detailTypeDefaultConfig.getDefaultDisplayInfo().getMetricIds())) {
            return detailTypeDefaultConfig.getDefaultDisplayInfo().getMetricIds().stream()
                    .map(id -> {
                        SchemaElement metric = getElement(SchemaElementType.METRIC, id);
                        return metric;
                    }).filter(Objects::nonNull).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public List<SchemaElement> getTagDefaultMetrics() {
        DetailTypeDefaultConfig detailTypeDefaultConfig = getTagTypeDefaultConfig();
        if (Objects.isNull(detailTypeDefaultConfig)
                || Objects.isNull(detailTypeDefaultConfig.getDefaultDisplayInfo())) {
            return new ArrayList<>();
        }
        if (CollectionUtils
                .isNotEmpty(detailTypeDefaultConfig.getDefaultDisplayInfo().getDimensionIds())) {
            return detailTypeDefaultConfig.getDefaultDisplayInfo().getDimensionIds().stream()
                    .map(id -> getElement(SchemaElementType.DIMENSION, id)).filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public boolean containsPartitionDimensions() {
        return dimensions.stream().anyMatch(SchemaElement::isPartitionTime);
    }

    public SchemaElement getPartitionDimension() {
        for (SchemaElement dimension : dimensions) {
            if (dimension.isPartitionTime()) {
                return dimension;
            }
        }
        return null;
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
