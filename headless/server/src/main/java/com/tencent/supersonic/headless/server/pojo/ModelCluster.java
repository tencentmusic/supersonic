package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
public class ModelCluster {

    private static final String split = "_";
    private Set<Long> modelIds = new LinkedHashSet<>();
    private String key;
    private boolean containsPartitionDimensions;

    public static ModelCluster build(Set<Long> modelIds, Boolean containsPartitionDimensions) {
        ModelCluster modelCluster = new ModelCluster();
        modelCluster.setModelIds(modelIds);
        modelCluster.setKey(StringUtils.join(modelIds, split));
        modelCluster.setContainsPartitionDimensions(containsPartitionDimensions);
        return modelCluster;
    }
}
