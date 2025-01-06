package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.enums.SchemaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SemanticSchemaResp {

    private Long dataSetId;
    private List<Long> modelIds;
    private SchemaType schemaType;
    private List<MetricSchemaResp> metrics = Lists.newArrayList();
    private List<DimSchemaResp> dimensions = Lists.newArrayList();
    private List<ModelRela> modelRelas = Lists.newArrayList();
    private List<ModelResp> modelResps = Lists.newArrayList();
    private DataSetResp dataSetResp;
    private DatabaseResp databaseResp;
    private QueryType queryType;

    public MetricSchemaResp getMetric(String bizName) {
        return metrics.stream().filter(metric -> bizName.equalsIgnoreCase(metric.getBizName()))
                .findFirst().orElse(null);
    }

    public MetricSchemaResp getMetric(Long id) {
        return metrics.stream().filter(metric -> id.equals(metric.getId())).findFirst()
                .orElse(null);
    }

    public DimSchemaResp getDimension(String bizName) {
        return dimensions.stream()
                .filter(dimension -> bizName.equalsIgnoreCase(dimension.getBizName())).findFirst()
                .orElse(null);
    }

    public DimSchemaResp getDimension(Long id) {
        return dimensions.stream().filter(dimension -> id.equals(dimension.getId())).findFirst()
                .orElse(null);
    }

    public Set<String> getNameFromBizNames(Set<String> bizNames) {
        Set<String> names = new HashSet<>();
        for (String bizName : bizNames) {
            DimSchemaResp dimSchemaResp = getDimension(bizName);
            if (dimSchemaResp != null) {
                names.add(dimSchemaResp.getName());
                continue;
            }
            MetricSchemaResp metricSchemaResp = getMetric(bizName);
            if (metricSchemaResp != null) {
                names.add(metricSchemaResp.getName());
            }
        }
        return names;
    }

}
