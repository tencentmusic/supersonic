package com.tencent.supersonic.chat.api.pojo;

import lombok.Data;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Data
public class ModelSchema {

    private SchemaElement model;
    private Set<SchemaElement> metrics = new HashSet<>();
    private Set<SchemaElement> dimensions = new HashSet<>();
    private Set<SchemaElement> dimensionValues = new HashSet<>();
    private SchemaElement entity = new SchemaElement();

    public SchemaElement getElement(SchemaElementType elementType, long elementID) {
        Optional<SchemaElement> element = Optional.empty();

        switch (elementType) {
            case ENTITY:
                element = Optional.ofNullable(entity);
                break;
            case MODEL:
                element = Optional.of(model);
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
            default:
        }

        if (element.isPresent()) {
            return element.get();
        } else {
            return null;
        }
    }

}
