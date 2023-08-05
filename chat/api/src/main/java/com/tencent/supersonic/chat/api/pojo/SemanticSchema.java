package com.tencent.supersonic.chat.api.pojo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SemanticSchema implements Serializable {
    private List<DomainSchema> domainSchemaList;

    public SemanticSchema(List<DomainSchema> domainSchemaList) {
        this.domainSchemaList = domainSchemaList;
    }

    public void add(DomainSchema schema) {
        domainSchemaList.add(schema);
    }

    public Map<Long, String> getDomainIdToName() {
        return domainSchemaList.stream()
                .collect(Collectors.toMap(a -> a.getDomain().getId(), a -> a.getDomain().getName(), (k1, k2) -> k1));
    }

    public List<SchemaElement> getDimensionValues() {
        List<SchemaElement> dimensionValues = new ArrayList<>();
        domainSchemaList.stream().forEach(d -> dimensionValues.addAll(d.getDimensionValues()));
        return dimensionValues;
    }

    public List<SchemaElement> getDimensions() {
        List<SchemaElement> dimensions = new ArrayList<>();
        domainSchemaList.stream().forEach(d -> dimensions.addAll(d.getDimensions()));
        return dimensions;
    }

    public List<SchemaElement> getMetrics() {
        List<SchemaElement> metrics = new ArrayList<>();
        domainSchemaList.stream().forEach(d -> metrics.addAll(d.getMetrics()));
        return metrics;
    }

    public List<SchemaElement> getDomains() {
        List<SchemaElement> domains = new ArrayList<>();
        domainSchemaList.stream().forEach(d -> domains.add(d.getDomain()));
        return domains;
    }

    public List<SchemaElement> getEntities() {
        List<SchemaElement> entities = new ArrayList<>();
        domainSchemaList.stream().forEach(d -> entities.add(d.getEntity()));
        return entities;
    }
}
