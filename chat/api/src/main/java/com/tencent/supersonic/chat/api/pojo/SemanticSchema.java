package com.tencent.supersonic.chat.api.pojo;

import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SemanticSchema implements Serializable {

    private List<ModelSchema> modelSchemaList;

    public SemanticSchema(List<ModelSchema> modelSchemaList) {
        this.modelSchemaList = modelSchemaList;
    }

    public void add(ModelSchema schema) {
        modelSchemaList.add(schema);
    }

    public SchemaElement getElement(SchemaElementType elementType, long elementID) {
        Optional<SchemaElement> element = Optional.empty();

        switch (elementType) {
            case ENTITY:
                element = getElementsById(elementID, getEntities());
                break;
            case MODEL:
                element = getElementsById(elementID, getModels());
                break;
            case METRIC:
                element = getElementsById(elementID, getMetrics());
                break;
            case DIMENSION:
                element = getElementsById(elementID, getDimensions());
                break;
            case VALUE:
                element = getElementsById(elementID, getDimensionValues());
                break;
            default:
        }

        if (element.isPresent()) {
            return element.get();
        } else {
            return null;
        }
    }

    public SchemaElement getElementByName(SchemaElementType elementType, String name) {
        Optional<SchemaElement> element = Optional.empty();

        switch (elementType) {
            case ENTITY:
                element = getElementsByName(name, getEntities());
                break;
            case MODEL:
                element = getElementsByName(name, getModels());
                break;
            case METRIC:
                element = getElementsByName(name, getMetrics());
                break;
            case DIMENSION:
                element = getElementsByName(name, getDimensions());
                break;
            case VALUE:
                element = getElementsByName(name, getDimensionValues());
                break;
            default:
        }

        if (element.isPresent()) {
            return element.get();
        } else {
            return null;
        }
    }

    public Map<Long, String> getModelIdToName() {
        return modelSchemaList.stream()
                .collect(Collectors.toMap(a -> a.getModel().getId(), a -> a.getModel().getName(), (k1, k2) -> k1));
    }

    public List<SchemaElement> getDimensionValues() {
        List<SchemaElement> dimensionValues = new ArrayList<>();
        modelSchemaList.stream().forEach(d -> dimensionValues.addAll(d.getDimensionValues()));
        return dimensionValues;
    }

    public List<SchemaElement> getDimensions() {
        List<SchemaElement> dimensions = new ArrayList<>();
        modelSchemaList.stream().forEach(d -> dimensions.addAll(d.getDimensions()));
        return dimensions;
    }

    public List<SchemaElement> getDimensions(Set<Long> modelIds) {
        List<SchemaElement> dimensions = getDimensions();
        return getElementsByModelId(modelIds, dimensions);
    }

    public SchemaElement getDimensions(Long id) {
        List<SchemaElement> dimensions = getDimensions();
        Optional<SchemaElement> dimension = getElementsById(id, dimensions);
        return dimension.orElse(null);
    }

    public List<SchemaElement> getTags() {
        List<SchemaElement> tags = new ArrayList<>();
        modelSchemaList.stream().forEach(d -> tags.addAll(d.getTags()));
        return tags;
    }

    public List<SchemaElement> getTags(Set<Long> modelIds) {
        List<SchemaElement> tags = new ArrayList<>();
        modelSchemaList.stream().filter(schemaElement -> modelIds.contains(schemaElement.getModel()))
                .forEach(d -> tags.addAll(d.getTags()));
        return tags;
    }

    public List<SchemaElement> getMetrics() {
        List<SchemaElement> metrics = new ArrayList<>();
        modelSchemaList.stream().forEach(d -> metrics.addAll(d.getMetrics()));
        return metrics;
    }

    public List<SchemaElement> getMetrics(Set<Long> modelIds) {
        List<SchemaElement> metrics = getMetrics();
        return getElementsByModelId(modelIds, metrics);
    }

    public List<SchemaElement> getEntities() {
        List<SchemaElement> entities = new ArrayList<>();
        modelSchemaList.stream().forEach(d -> entities.add(d.getEntity()));
        return entities;
    }

    private List<SchemaElement> getElementsByModelId(Set<Long> modelIds, List<SchemaElement> elements) {
        return elements.stream()
                .filter(schemaElement -> modelIds.contains(schemaElement.getModel()))
                .collect(Collectors.toList());
    }

    private Optional<SchemaElement> getElementsById(Long id, List<SchemaElement> elements) {
        return elements.stream()
                .filter(schemaElement -> id.equals(schemaElement.getId()))
                .findFirst();
    }

    private Optional<SchemaElement> getElementsByName(String name, List<SchemaElement> elements) {
        return elements.stream()
                .filter(schemaElement -> name.equals(schemaElement.getName()))
                .findFirst();
    }

    public List<SchemaElement> getModels() {
        List<SchemaElement> models = new ArrayList<>();
        modelSchemaList.stream().forEach(d -> models.add(d.getModel()));
        return models;
    }

    public Map<String, String> getBizNameToName(Set<Long> modelIds) {
        List<SchemaElement> allElements = new ArrayList<>();
        allElements.addAll(getDimensions(modelIds));
        allElements.addAll(getMetrics(modelIds));
        return allElements.stream()
                .collect(Collectors.toMap(SchemaElement::getBizName, SchemaElement::getName, (k1, k2) -> k1));
    }

    public Map<Long, ModelSchema> getModelSchemaMap() {
        if (CollectionUtils.isEmpty(modelSchemaList)) {
            return new HashMap<>();
        }
        return modelSchemaList.stream().collect(Collectors.toMap(modelSchema
                -> modelSchema.getModel().getModel(), modelSchema -> modelSchema));
    }
}
