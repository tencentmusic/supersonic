package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.enums.SchemaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static com.tencent.supersonic.common.pojo.Constants.UNDERLINE;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SemanticSchemaResp {

    private Long dataSetId;
    private List<Long> modelIds;
    private SchemaType schemaType;
    private List<MetricSchemaResp> metrics = Lists.newArrayList();
    private List<DimSchemaResp> dimensions = Lists.newArrayList();
    private List<TagResp> tags = Lists.newArrayList();
    private List<ModelRela> modelRelas = Lists.newArrayList();
    private List<ModelResp> modelResps = Lists.newArrayList();
    private DataSetResp dataSetResp;
    private DatabaseResp databaseResp;
    private QueryType queryType;

    public String getSchemaKey() {
        if (dataSetId == null) {
            return String.format("%s_%s", schemaType, StringUtils.join(modelIds, UNDERLINE));
        }
        return String.format("%s_%s", schemaType, dataSetId);

    }

    public MetricSchemaResp getMetric(String bizName) {
        return metrics.stream().filter(metric -> bizName.equalsIgnoreCase(metric.getBizName()))
                .findFirst().orElse(null);
    }

    public MetricSchemaResp getMetric(Long id) {
        return metrics.stream().filter(metric -> id.equals(metric.getId()))
                .findFirst().orElse(null);
    }

    public DimSchemaResp getDimension(String bizName) {
        return dimensions.stream().filter(dimension -> bizName.equalsIgnoreCase(dimension.getBizName()))
                .findFirst().orElse(null);
    }

    public DimSchemaResp getDimension(Long id) {
        return dimensions.stream().filter(dimension -> id.equals(dimension.getId()))
                .findFirst().orElse(null);
    }

}