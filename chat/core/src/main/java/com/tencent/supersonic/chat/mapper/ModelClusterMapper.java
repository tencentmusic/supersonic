package com.tencent.supersonic.chat.mapper;

import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SchemaModelClusterMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.utils.ModelClusterBuilder;
import com.tencent.supersonic.common.pojo.ModelCluster;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.knowledge.service.SchemaService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ModelClusterMapper implements SchemaMapper {

    @Override
    public void map(QueryContext queryContext) {
        SchemaService schemaService = ContextUtils.getBean(SchemaService.class);
        SemanticSchema semanticSchema = schemaService.getSemanticSchema();
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
