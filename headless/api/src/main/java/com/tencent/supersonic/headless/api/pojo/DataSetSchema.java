package com.tencent.supersonic.headless.api.pojo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;

@Data
public class DataSetSchema {

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

    public TimeDefaultConfig getTagTypeTimeDefaultConfig() {
        if (queryConfig == null) {
            return null;
        }
        if (queryConfig.getTagTypeDefaultConfig() == null) {
            return null;
        }
        return queryConfig.getTagTypeDefaultConfig().getTimeDefaultConfig();
    }

    public TimeDefaultConfig getMetricTypeTimeDefaultConfig() {
        if (queryConfig == null) {
            return null;
        }
        if (queryConfig.getMetricTypeDefaultConfig() == null) {
            return null;
        }
        return queryConfig.getMetricTypeDefaultConfig().getTimeDefaultConfig();
    }

    public TagTypeDefaultConfig getTagTypeDefaultConfig() {
        if (queryConfig == null) {
            return null;
        }
        return queryConfig.getTagTypeDefaultConfig();
    }

    public List<SchemaElement> getTagDefaultDimensions() {
        TagTypeDefaultConfig tagTypeDefaultConfig = getTagTypeDefaultConfig();
        if (Objects.isNull(tagTypeDefaultConfig) || Objects.isNull(tagTypeDefaultConfig.getDefaultDisplayInfo())) {
            return new ArrayList<>();
        }
        if (CollectionUtils.isNotEmpty(tagTypeDefaultConfig.getDefaultDisplayInfo().getMetricIds())) {
            return tagTypeDefaultConfig.getDefaultDisplayInfo().getMetricIds()
                    .stream().map(id -> {
                        SchemaElement metric = getElement(SchemaElementType.METRIC, id);
                        return metric;
                    }).filter(Objects::nonNull).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public List<SchemaElement> getTagDefaultMetrics() {
        TagTypeDefaultConfig tagTypeDefaultConfig = getTagTypeDefaultConfig();
        if (Objects.isNull(tagTypeDefaultConfig) || Objects.isNull(tagTypeDefaultConfig.getDefaultDisplayInfo())) {
            return new ArrayList<>();
        }
        if (CollectionUtils.isNotEmpty(tagTypeDefaultConfig.getDefaultDisplayInfo().getDimensionIds())) {
            return tagTypeDefaultConfig.getDefaultDisplayInfo().getDimensionIds().stream()
                    .map(id -> getElement(SchemaElementType.DIMENSION, id))
                    .filter(Objects::nonNull).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

}
