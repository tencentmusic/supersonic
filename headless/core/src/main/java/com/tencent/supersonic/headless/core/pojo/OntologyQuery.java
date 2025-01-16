package com.tencent.supersonic.headless.core.pojo;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.supersonic.common.pojo.ColumnOrder;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An ontology query comprises metrics/dimensions that are relevant to the semantic query. Note that
 * metrics/dimensions in the ontology query must be a subset of an ontology.
 */
@Data
public class OntologyQuery {

    private Map<String, ModelResp> modelMap = Maps.newHashMap();
    private Map<String, Set<MetricSchemaResp>> metricMap = Maps.newHashMap();
    private Map<String, Set<DimSchemaResp>> dimensionMap = Maps.newHashMap();
    private Set<String> fields = Sets.newHashSet();
    private Long limit;
    private List<ColumnOrder> order;
    private boolean nativeQuery = true;
    private AggOption aggOption = AggOption.NATIVE;
    private String sql;

    public Set<ModelResp> getModels() {
        return modelMap.values().stream().collect(Collectors.toSet());
    }

    public Set<DimSchemaResp> getDimensions() {
        Set<DimSchemaResp> dimensions = Sets.newHashSet();
        dimensionMap.entrySet().forEach(entry -> {
            dimensions.addAll(entry.getValue());
        });
        return dimensions;
    }

    public Set<MetricSchemaResp> getMetrics() {
        Set<MetricSchemaResp> metrics = Sets.newHashSet();
        metricMap.entrySet().forEach(entry -> {
            metrics.addAll(entry.getValue());
        });
        return metrics;
    }

    public Set<MetricSchemaResp> getMetricsByModel(String modelName) {
        return metricMap.get(modelName);
    }

    public Set<DimSchemaResp> getDimensionsByModel(String modelName) {
        return dimensionMap.get(modelName);
    }
}
