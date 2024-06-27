package com.tencent.supersonic.headless.server.utils;


import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.response.ModelSchemaResp;
import com.tencent.supersonic.headless.server.pojo.ModelCluster;
import com.tencent.supersonic.headless.server.web.service.SchemaService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ModelClusterBuilder {

    public static Map<String, ModelCluster> buildModelClusters(List<Long> modelIds) {
        SchemaService schemaService = ContextUtils.getBean(SchemaService.class);
        List<ModelSchemaResp> modelSchemaResps = schemaService.fetchModelSchemaResps(modelIds);
        Map<Long, ModelSchemaResp> modelIdToModelSchema = modelSchemaResps.stream()
                .collect(Collectors.toMap(ModelSchemaResp::getId, value -> value, (k1, k2) -> k1));

        Set<Long> visited = new HashSet<>();
        List<Set<Long>> modelClusters = new ArrayList<>();
        for (ModelSchemaResp model : modelSchemaResps) {
            if (!visited.contains(model.getId())) {
                Set<Long> modelCluster = new HashSet<>();
                dfs(model, modelIdToModelSchema, visited, modelCluster);
                modelClusters.add(modelCluster);
            }
        }
        return modelClusters.stream().map(ModelCluster::build)
                .collect(Collectors.toMap(ModelCluster::getKey, value -> value, (k1, k2) -> k1));
    }

    private static void dfs(ModelSchemaResp model, Map<Long, ModelSchemaResp> modelMap,
            Set<Long> visited, Set<Long> modelCluster) {
        visited.add(model.getId());
        modelCluster.add(model.getId());
        for (Long neighborId : model.getModelClusterSet()) {
            if (!visited.contains(neighborId)) {
                dfs(modelMap.get(neighborId), modelMap, visited, modelCluster);
            }
        }
    }

}