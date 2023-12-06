package com.tencent.supersonic.common.pojo;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class ModelCluster {

    private static final String split = "_";

    private Set<Long> modelIds = new LinkedHashSet<>();

    private Set<String> modelNames = new LinkedHashSet<>();

    private String key;

    private String name;

    public static ModelCluster build(Set<Long> modelIds) {
        ModelCluster modelCluster = new ModelCluster();
        modelCluster.setModelIds(modelIds);
        modelCluster.setKey(StringUtils.join(modelIds, split));
        return modelCluster;
    }

    public static ModelCluster build(String key) {
        ModelCluster modelCluster = new ModelCluster();
        modelCluster.setModelIds(getModelIdFromKey(key));
        modelCluster.setKey(key);
        return modelCluster;
    }

    public void buildName(Map<Long, String> modelNameMap) {
        modelNames = modelNameMap.entrySet().stream().filter(entry ->
                        modelIds.contains(entry.getKey())).map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        name = String.join(split, modelNames);
    }

    public static Set<Long> getModelIdFromKey(String key) {
        return Arrays.stream(key.split(split))
                .map(Long::parseLong).collect(Collectors.toSet());
    }

    public Long getFirstModel() {
        return modelIds.stream().findFirst().orElse(null);
    }

}
