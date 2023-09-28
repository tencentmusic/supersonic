package com.tencent.supersonic.chat.api.pojo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SemanticSchema implements Serializable {

    private List<ModelSchema> modelSchemaList;

    public SemanticSchema(List<ModelSchema> modelSchemaList) {
        this.modelSchemaList = modelSchemaList;
    }

    public void add(ModelSchema schema) {
        modelSchemaList.add(schema);
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

    public List<SchemaElement> getDimensions(Long modelId) {
        List<SchemaElement> dimensions = getDimensions();
        return getElementsByModelId(modelId, dimensions);
    }

    public List<SchemaElement> getMetrics() {
        List<SchemaElement> metrics = new ArrayList<>();
        modelSchemaList.stream().forEach(d -> metrics.addAll(d.getMetrics()));
        return metrics;
    }

    public List<SchemaElement> getMetrics(Long modelId) {
        List<SchemaElement> metrics = getMetrics();
        return getElementsByModelId(modelId, metrics);
    }

    private List<SchemaElement> getElementsByModelId(Long modelId, List<SchemaElement> elements) {
        return elements.stream()
                .filter(schemaElement -> modelId.equals(schemaElement.getModel()))
                .collect(Collectors.toList());
    }

    public List<SchemaElement> getModels() {
        List<SchemaElement> models = new ArrayList<>();
        modelSchemaList.stream().forEach(d -> models.add(d.getModel()));
        return models;
    }

    public List<SchemaElement> getEntities() {
        List<SchemaElement> entities = new ArrayList<>();
        modelSchemaList.stream().forEach(d -> entities.add(d.getEntity()));
        return entities;
    }
}
