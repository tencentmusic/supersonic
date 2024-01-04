package com.tencent.supersonic.chat.core.mapper;

import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SchemaModelClusterMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.core.utils.ModelClusterBuilder;
import com.tencent.supersonic.common.pojo.ModelCluster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/***
 * ModelClusterMapper build a cluster from
 * connectable data models based on model-rela configuration
 * and generate SchemaModelClusterMapInfo
 */
public class ModelClusterMapper implements SchemaMapper {

    @Override
    public void map(QueryContext queryContext) {
        SemanticSchema semanticSchema = queryContext.getSemanticSchema();
        SchemaMapInfo schemaMapInfo = queryContext.getMapInfo();
        List<ModelCluster> modelClusters = buildModelClusterMatched(schemaMapInfo, semanticSchema);
        Map<String, List<SchemaElementMatch>> modelClusterElementMatches = new HashMap<>();
        for (ModelCluster modelCluster : modelClusters) {
            for (Long modelId : schemaMapInfo.getMatchedModels()) {
                if (modelCluster.getModelIds().contains(modelId)) {
                    modelClusterElementMatches.computeIfAbsent(modelCluster.getKey(), k -> new ArrayList<>())
                            .addAll(schemaMapInfo.getMatchedElements(modelId));
                }
            }
        }
        SchemaModelClusterMapInfo modelClusterMapInfo = new SchemaModelClusterMapInfo();
        modelClusterMapInfo.setModelElementMatches(modelClusterElementMatches);
        queryContext.setModelClusterMapInfo(modelClusterMapInfo);
    }

    private List<ModelCluster> buildModelClusterMatched(SchemaMapInfo schemaMapInfo,
            SemanticSchema semanticSchema) {
        Set<Long> matchedModels = schemaMapInfo.getMatchedModels();
        List<ModelCluster> modelClusters = ModelClusterBuilder.buildModelClusters(semanticSchema);
        return modelClusters.stream().map(ModelCluster::getModelIds).peek(modelCluster -> {
            modelCluster.removeIf(model -> !matchedModels.contains(model));
        }).filter(modelCluster -> modelCluster.size() > 0).map(ModelCluster::build).collect(Collectors.toList());
    }

}
