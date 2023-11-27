package com.tencent.supersonic.chat.utils;


import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.common.pojo.ModelCluster;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ModelClusterBuilder {

    public static List<ModelCluster> buildModelClusters(SemanticSchema semanticSchema) {
        Map<Long, ModelSchema> modelMap = semanticSchema.getModelSchemaMap();
        Set<Long> visited = new HashSet<>();
        List<Set<Long>> modelClusters = new ArrayList<>();
        for (ModelSchema model : modelMap.values()) {
            if (!visited.contains(model.getModel().getModel())) {
                Set<Long> modelCluster = new HashSet<>();
                dfs(model, modelMap, visited, modelCluster);
                modelClusters.add(modelCluster);
            }
        }
        return modelClusters.stream().map(ModelCluster::build).collect(Collectors.toList());
    }

    private static void dfs(ModelSchema model, Map<Long, ModelSchema> modelMap,
                     Set<Long> visited, Set<Long> modelCluster) {
        visited.add(model.getModel().getModel());
        modelCluster.add(model.getModel().getModel());
        for (Long neighborId : model.getModelClusterSet()) {
            if (!visited.contains(neighborId)) {
                dfs(modelMap.get(neighborId), modelMap, visited, modelCluster);
            }
        }
    }

}
