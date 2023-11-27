package com.tencent.supersonic.chat.api.pojo;

import com.google.common.collect.Sets;
import com.tencent.supersonic.common.pojo.ModelRela;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Data
public class ModelSchema {

    private SchemaElement model;
    private Set<SchemaElement> metrics = new HashSet<>();
    private Set<SchemaElement> dimensions = new HashSet<>();
    private Set<SchemaElement> dimensionValues = new HashSet<>();
    private Set<SchemaElement> tags = new HashSet<>();
    private SchemaElement entity = new SchemaElement();
    private List<ModelRela> modelRelas = new ArrayList<>();

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
            case TAG:
                element = tags.stream().filter(e -> e.getId() == elementID).findFirst();
                break;
            default:
        }

        if (element.isPresent()) {
            return element.get();
        } else {
            return null;
        }
    }

    public SchemaElement getElement(SchemaElementType elementType, String name) {
        Optional<SchemaElement> element = Optional.empty();

        switch (elementType) {
            case ENTITY:
                element = Optional.ofNullable(entity);
                break;
            case MODEL:
                element = Optional.of(model);
                break;
            case METRIC:
                element = metrics.stream().filter(e -> name.equals(e.getName())).findFirst();
                break;
            case DIMENSION:
                element = dimensions.stream().filter(e -> name.equals(e.getName())).findFirst();
                break;
            case VALUE:
                element = dimensionValues.stream().filter(e -> name.equals(e.getName())).findFirst();
                break;
            default:
        }

        if (element.isPresent()) {
            return element.get();
        } else {
            return null;
        }
    }

    public Set<Long> getModelClusterSet() {
        if (CollectionUtils.isEmpty(modelRelas)) {
            return Sets.newHashSet();
        }
        Set<Long> modelClusterSet = new HashSet<>();
        modelRelas.forEach(modelRela -> {
            modelClusterSet.add(modelRela.getToModelId());
            modelClusterSet.add(modelRela.getFromModelId());
        });
        return modelClusterSet;
    }

}
