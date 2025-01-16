package com.tencent.supersonic.headless.core.pojo;

import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An ontology comprises a group of data models that can be joined together either in star schema or
 * snowflake schema.
 */
@Data
public class Ontology {

    private DatabaseResp database;
    private Map<String, ModelResp> modelMap = new HashMap<>();
    private Map<String, List<MetricSchemaResp>> metricMap = new HashMap<>();
    private Map<String, List<DimSchemaResp>> dimensionMap = new HashMap<>();
    private List<JoinRelation> joinRelations;

    public List<MetricSchemaResp> getMetrics() {
        return metricMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public List<DimSchemaResp> getDimensions() {
        return dimensionMap.values().stream().flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public EngineType getDatabaseType() {
        if (Objects.nonNull(database)) {
            return EngineType.fromString(database.getType().toUpperCase());
        }
        return null;
    }

    public String getDatabaseVersion() {
        if (Objects.nonNull(database)) {
            return database.getVersion();
        }
        return null;
    }

}
