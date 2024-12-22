package com.tencent.supersonic.headless.core.pojo;

import com.google.common.collect.Sets;
import com.tencent.supersonic.common.pojo.ColumnOrder;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import lombok.Data;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An ontology query comprises metrics/dimensions that are relevant to the semantic query. Note that
 * metrics/dimensions in the ontology query must be a subset of an ontology.
 */
@Data
public class OntologyQuery {

    private Set<ModelResp> models = Sets.newHashSet();
    private Set<MetricSchemaResp> metrics = Sets.newHashSet();
    private Set<DimSchemaResp> dimensions = Sets.newHashSet();
    private Set<String> fields = Sets.newHashSet();
    private Long limit;
    private List<ColumnOrder> order;
    private boolean nativeQuery = true;
    private AggOption aggOption = AggOption.NATIVE;

    public Set<MetricSchemaResp> getMetricsByModel(Long modelId) {
        return metrics.stream().filter(m -> m.getModelId().equals(modelId))
                .collect(Collectors.toSet());
    }

    public Set<DimSchemaResp> getDimensionsByModel(Long modelId) {
        return dimensions.stream().filter(m -> m.getModelId().equals(modelId))
                .collect(Collectors.toSet());
    }
}
