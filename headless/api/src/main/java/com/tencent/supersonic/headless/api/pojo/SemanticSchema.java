package com.tencent.supersonic.headless.api.pojo;

import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SemanticSchema implements Serializable {

    private List<DataSetSchema> dataSetSchemaList;

    public SemanticSchema(List<DataSetSchema> dataSetSchemaList) {
        this.dataSetSchemaList = dataSetSchemaList;
    }

    public void add(DataSetSchema schema) {
        dataSetSchemaList.add(schema);
    }

    public SchemaElement getElement(SchemaElementType elementType, long elementID) {
        Optional<SchemaElement> element = Optional.empty();

        switch (elementType) {
            case ENTITY:
                element = getElementsById(elementID, getEntities());
                break;
            case DATASET:
                element = getElementsById(elementID, getDataSets());
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
            case TAG:
                element = getElementsById(elementID, getTags());
                break;
            case TERM:
                element = getElementsById(elementID, getTerms());
                break;
            default:
        }

        if (element.isPresent()) {
            return element.get();
        } else {
            return null;
        }
    }

    public Map<Long, String> getDataSetIdToName() {
        return dataSetSchemaList.stream()
                .collect(Collectors.toMap(a -> a.getDataSet().getId(), a -> a.getDataSet().getName(), (k1, k2) -> k1));
    }

    public List<SchemaElement> getDimensionValues() {
        List<SchemaElement> dimensionValues = new ArrayList<>();
        dataSetSchemaList.stream().forEach(d -> dimensionValues.addAll(d.getDimensionValues()));
        return dimensionValues;
    }

    public List<SchemaElement> getDimensions() {
        List<SchemaElement> dimensions = new ArrayList<>();
        dataSetSchemaList.stream().forEach(d -> dimensions.addAll(d.getDimensions()));
        return dimensions;
    }

    public List<SchemaElement> getDimensions(Long dataSetId) {
        List<SchemaElement> dimensions = getDimensions();
        return getElementsByDataSetId(dataSetId, dimensions);
    }

    public SchemaElement getDimension(Long id) {
        List<SchemaElement> dimensions = getDimensions();
        Optional<SchemaElement> dimension = getElementsById(id, dimensions);
        return dimension.orElse(null);
    }

    public List<SchemaElement> getMetrics() {
        List<SchemaElement> metrics = new ArrayList<>();
        dataSetSchemaList.stream().forEach(d -> metrics.addAll(d.getMetrics()));
        return metrics;
    }

    public List<SchemaElement> getMetrics(Long dataSetId) {
        List<SchemaElement> metrics = getMetrics();
        return getElementsByDataSetId(dataSetId, metrics);
    }

    public List<SchemaElement> getEntities() {
        List<SchemaElement> entities = new ArrayList<>();
        dataSetSchemaList.stream().forEach(d -> entities.add(d.getEntity()));
        return entities;
    }

    public List<SchemaElement> getEntities(Long dataSetId) {
        List<SchemaElement> entities = getEntities();
        return getElementsByDataSetId(dataSetId, entities);
    }

    public List<SchemaElement> getTags() {
        List<SchemaElement> tags = new ArrayList<>();
        dataSetSchemaList.stream().forEach(d -> tags.addAll(d.getTags()));
        return tags;
    }

    public List<SchemaElement> getTags(Long dataSetId) {
        List<SchemaElement> tags = new ArrayList<>();
        dataSetSchemaList.stream().filter(schemaElement ->
                        dataSetId.equals(schemaElement.getDataSet().getDataSet()))
                .forEach(d -> tags.addAll(d.getTags()));
        return tags;
    }

    public List<SchemaElement> getTerms() {
        List<SchemaElement> terms = new ArrayList<>();
        dataSetSchemaList.stream().forEach(d -> terms.addAll(d.getTerms()));
        return terms;
    }

    private List<SchemaElement> getElementsByDataSetId(Long dataSetId, List<SchemaElement> elements) {
        return elements.stream()
                .filter(schemaElement -> dataSetId.equals(schemaElement.getDataSet()))
                .collect(Collectors.toList());
    }

    private Optional<SchemaElement> getElementsById(Long id, List<SchemaElement> elements) {
        return elements.stream()
                .filter(schemaElement -> id.equals(schemaElement.getId()))
                .findFirst();
    }

    public SchemaElement getDataSet(Long dataSetId) {
        List<SchemaElement> dataSets = getDataSets();
        return getElementsById(dataSetId, dataSets).orElse(null);
    }

    public List<SchemaElement> getDataSets() {
        List<SchemaElement> dataSets = new ArrayList<>();
        dataSetSchemaList.stream().forEach(d -> dataSets.add(d.getDataSet()));
        return dataSets;
    }

    public Map<String, String> getBizNameToName(Long dataSetId) {
        List<SchemaElement> allElements = new ArrayList<>();
        allElements.addAll(getDimensions(dataSetId));
        allElements.addAll(getMetrics(dataSetId));
        return allElements.stream()
                .collect(Collectors.toMap(SchemaElement::getBizName, SchemaElement::getName, (k1, k2) -> k1));
    }

    public Map<Long, DataSetSchema> getDataSetSchemaMap() {
        if (CollectionUtils.isEmpty(dataSetSchemaList)) {
            return new HashMap<>();
        }
        return dataSetSchemaList.stream().collect(Collectors.toMap(dataSetSchema
                -> dataSetSchema.getDataSet().getDataSet(), dataSetSchema -> dataSetSchema));
    }
}
