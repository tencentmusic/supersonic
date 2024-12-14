package com.tencent.supersonic.headless.core.pojo;

import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An ontology comprises a group of data models that can be joined together either in star schema or
 * snowflake schema.
 */
@Data
public class Ontology {

    private Database database;
    private Map<String, DataModel> dataModelMap = new HashMap<>();
    private List<MetricSchemaResp> metrics = new ArrayList<>();
    private Map<String, List<DimSchemaResp>> dimensionMap = new HashMap<>();
    private List<JoinRelation> joinRelations;

    public List<DimSchemaResp> getDimensions() {
        return dimensionMap.values().stream().flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

}
