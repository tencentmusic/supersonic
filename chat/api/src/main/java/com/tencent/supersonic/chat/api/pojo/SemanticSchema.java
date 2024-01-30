package com.tencent.supersonic.chat.api.pojo;

import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class SemanticSchema implements Serializable {

    private List<ViewSchema> viewSchemaList;

    public SemanticSchema(List<ViewSchema> viewSchemaList) {
        this.viewSchemaList = viewSchemaList;
    }

    public void add(ViewSchema schema) {
        viewSchemaList.add(schema);
    }

    public SchemaElement getElement(SchemaElementType elementType, long elementID) {
        Optional<SchemaElement> element = Optional.empty();

        switch (elementType) {
            case ENTITY:
                element = getElementsById(elementID, getEntities());
                break;
            case VIEW:
                element = getElementsById(elementID, getViews());
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
                element = getElementsByNameOrAlias(name, getEntities());
                break;
            case VIEW:
                element = getElementsByNameOrAlias(name, getViews());
                break;
            case METRIC:
                element = getElementsByNameOrAlias(name, getMetrics());
                break;
            case DIMENSION:
                element = getElementsByNameOrAlias(name, getDimensions());
                break;
            case VALUE:
                element = getElementsByNameOrAlias(name, getDimensionValues());
                break;
            default:
        }

        if (element.isPresent()) {
            return element.get();
        } else {
            return null;
        }
    }

    public Map<Long, String> getViewIdToName() {
        return viewSchemaList.stream()
                .collect(Collectors.toMap(a -> a.getView().getId(), a -> a.getView().getName(), (k1, k2) -> k1));
    }

    public List<SchemaElement> getDimensionValues() {
        List<SchemaElement> dimensionValues = new ArrayList<>();
        viewSchemaList.stream().forEach(d -> dimensionValues.addAll(d.getDimensionValues()));
        return dimensionValues;
    }

    public List<SchemaElement> getDimensions() {
        List<SchemaElement> dimensions = new ArrayList<>();
        viewSchemaList.stream().forEach(d -> dimensions.addAll(d.getDimensions()));
        return dimensions;
    }

    public List<SchemaElement> getDimensions(Long viewId) {
        List<SchemaElement> dimensions = getDimensions();
        return getElementsByViewId(viewId, dimensions);
    }

    public SchemaElement getDimension(Long id) {
        List<SchemaElement> dimensions = getDimensions();
        Optional<SchemaElement> dimension = getElementsById(id, dimensions);
        return dimension.orElse(null);
    }

    public List<SchemaElement> getTags() {
        List<SchemaElement> tags = new ArrayList<>();
        viewSchemaList.stream().forEach(d -> tags.addAll(d.getTags()));
        return tags;
    }

    public List<SchemaElement> getTags(Long viewId) {
        List<SchemaElement> tags = new ArrayList<>();
        viewSchemaList.stream().filter(schemaElement ->
                        viewId.equals(schemaElement.getView().getView()))
                .forEach(d -> tags.addAll(d.getTags()));
        return tags;
    }

    public List<SchemaElement> getMetrics() {
        List<SchemaElement> metrics = new ArrayList<>();
        viewSchemaList.stream().forEach(d -> metrics.addAll(d.getMetrics()));
        return metrics;
    }

    public List<SchemaElement> getMetrics(Long viewId) {
        List<SchemaElement> metrics = getMetrics();
        return getElementsByViewId(viewId, metrics);
    }

    public List<SchemaElement> getEntities() {
        List<SchemaElement> entities = new ArrayList<>();
        viewSchemaList.stream().forEach(d -> entities.add(d.getEntity()));
        return entities;
    }

    public List<SchemaElement> getEntities(Long viewId) {
        List<SchemaElement> entities = getEntities();
        return getElementsByViewId(viewId, entities);
    }

    private List<SchemaElement> getElementsByViewId(Long viewId, List<SchemaElement> elements) {
        return elements.stream()
                .filter(schemaElement -> viewId.equals(schemaElement.getView()))
                .collect(Collectors.toList());
    }

    private Optional<SchemaElement> getElementsById(Long id, List<SchemaElement> elements) {
        return elements.stream()
                .filter(schemaElement -> id.equals(schemaElement.getId()))
                .findFirst();
    }

    private Optional<SchemaElement> getElementsByNameOrAlias(String name, List<SchemaElement> elements) {
        return elements.stream()
                .filter(schemaElement ->
                        name.equals(schemaElement.getName()) || (Objects.nonNull(schemaElement.getAlias())
                                && schemaElement.getAlias().contains(name))
                ).findFirst();
    }

    public SchemaElement getView(Long viewId) {
        List<SchemaElement> views = getViews();
        return getElementsById(viewId, views).orElse(null);
    }

    public List<SchemaElement> getViews() {
        List<SchemaElement> views = new ArrayList<>();
        viewSchemaList.stream().forEach(d -> views.add(d.getView()));
        return views;
    }

    public Map<String, String> getBizNameToName(Long viewId) {
        List<SchemaElement> allElements = new ArrayList<>();
        allElements.addAll(getDimensions(viewId));
        allElements.addAll(getMetrics(viewId));
        return allElements.stream()
                .collect(Collectors.toMap(SchemaElement::getBizName, SchemaElement::getName, (k1, k2) -> k1));
    }

    public Map<Long, ViewSchema> getViewSchemaMap() {
        if (CollectionUtils.isEmpty(viewSchemaList)) {
            return new HashMap<>();
        }
        return viewSchemaList.stream().collect(Collectors.toMap(viewSchema
                -> viewSchema.getView().getView(), viewSchema -> viewSchema));
    }
}
