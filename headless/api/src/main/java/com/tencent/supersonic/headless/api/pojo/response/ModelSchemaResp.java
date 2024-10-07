package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Sets;
import com.tencent.supersonic.common.pojo.ModelRela;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModelSchemaResp extends ModelResp {

    private List<MetricSchemaResp> metrics;
    private List<DimSchemaResp> dimensions;
    private List<ModelRela> modelRelas;

    public Set<Long> getModelClusterSet() {
        if (CollectionUtils.isEmpty(this.modelRelas)) {
            return Sets.newHashSet();
        } else {
            Set<Long> modelClusterSet = new HashSet();
            this.modelRelas.forEach((modelRela) -> {
                modelClusterSet.add(modelRela.getToModelId());
                modelClusterSet.add(modelRela.getFromModelId());
            });
            return modelClusterSet;
        }
    }
}
