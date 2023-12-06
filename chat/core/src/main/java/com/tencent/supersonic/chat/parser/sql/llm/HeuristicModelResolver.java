package com.tencent.supersonic.chat.parser.sql.llm;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaModelClusterMapInfo;
import com.tencent.supersonic.common.pojo.ModelCluster;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class HeuristicModelResolver implements ModelResolver {

    protected static String selectModelBySchemaElementMatchScore(Map<String, SemanticQuery> modelQueryModes,
            SchemaModelClusterMapInfo schemaMap) {
        //model count priority
        String modelIdByModelCount = getModelIdByMatchModelScore(schemaMap);
        if (Objects.nonNull(modelIdByModelCount)) {
            log.info("selectModel by model count:{}", modelIdByModelCount);
            return modelIdByModelCount;
        }

        Map<String, ModelMatchResult> modelTypeMap = getModelTypeMap(schemaMap);
        if (modelTypeMap.size() == 1) {
            String modelSelect = modelTypeMap.entrySet().stream().collect(Collectors.toList()).get(0).getKey();
            if (modelQueryModes.containsKey(modelSelect)) {
                log.info("selectModel with only one Model [{}]", modelSelect);
                return modelSelect;
            }
        } else {
            Map.Entry<String, ModelMatchResult> maxModel = modelTypeMap.entrySet().stream()
                    .filter(entry -> modelQueryModes.containsKey(entry.getKey()))
                    .sorted((o1, o2) -> {
                        int difference = o2.getValue().getCount() - o1.getValue().getCount();
                        if (difference == 0) {
                            return (int) ((o2.getValue().getMaxSimilarity()
                                    - o1.getValue().getMaxSimilarity()) * 100);
                        }
                        return difference;
                    }).findFirst().orElse(null);
            if (maxModel != null) {
                log.info("selectModel with multiple Models [{}]", maxModel.getKey());
                return maxModel.getKey();
            }
        }
        return null;
    }

    private static String getModelIdByMatchModelScore(SchemaModelClusterMapInfo schemaMap) {
        Map<String, List<SchemaElementMatch>> modelElementMatches = schemaMap.getModelElementMatches();
        // calculate model match score, matched element gets 1.0 point, and inherit element gets 0.5 point
        Map<String, Double> modelIdToModelScore = new HashMap<>();
        if (Objects.nonNull(modelElementMatches)) {
            for (Entry<String, List<SchemaElementMatch>> modelElementMatch : modelElementMatches.entrySet()) {
                String modelId = modelElementMatch.getKey();
                List<Double> modelMatchesScore = modelElementMatch.getValue().stream()
                        .filter(elementMatch -> elementMatch.getSimilarity() >= 1)
                        .filter(elementMatch -> SchemaElementType.MODEL.equals(elementMatch.getElement().getType()))
                        .map(elementMatch -> elementMatch.isInherited() ? 0.5 : 1.0).collect(Collectors.toList());

                if (!CollectionUtils.isEmpty(modelMatchesScore)) {
                    // get sum of model match score
                    double score = modelMatchesScore.stream().mapToDouble(Double::doubleValue).sum();
                    modelIdToModelScore.put(modelId, score);
                }
            }
            Entry<String, Double> maxModelScore = modelIdToModelScore.entrySet().stream()
                    .max(Comparator.comparingDouble(o -> o.getValue())).orElse(null);
            log.info("maxModelCount:{},modelIdToModelCount:{}", maxModelScore, modelIdToModelScore);
            if (Objects.nonNull(maxModelScore)) {
                return maxModelScore.getKey();
            }
        }
        return null;
    }

    public static Map<String, ModelMatchResult> getModelTypeMap(SchemaModelClusterMapInfo schemaMap) {
        Map<String, ModelMatchResult> modelCount = new HashMap<>();
        for (Map.Entry<String, List<SchemaElementMatch>> entry : schemaMap.getModelElementMatches().entrySet()) {
            List<SchemaElementMatch> schemaElementMatches = schemaMap.getMatchedElements(entry.getKey());
            if (schemaElementMatches != null && schemaElementMatches.size() > 0) {
                if (!modelCount.containsKey(entry.getKey())) {
                    modelCount.put(entry.getKey(), new ModelMatchResult());
                }
                ModelMatchResult modelMatchResult = modelCount.get(entry.getKey());
                Set<SchemaElementType> schemaElementTypes = new HashSet<>();
                schemaElementMatches.stream()
                        .forEach(schemaElementMatch -> schemaElementTypes.add(
                                schemaElementMatch.getElement().getType()));
                SchemaElementMatch schemaElementMatchMax = schemaElementMatches.stream()
                        .sorted((o1, o2) ->
                                ((int) ((o2.getSimilarity() - o1.getSimilarity()) * 100))
                        ).findFirst().orElse(null);
                if (schemaElementMatchMax != null) {
                    modelMatchResult.setMaxSimilarity(schemaElementMatchMax.getSimilarity());
                }
                modelMatchResult.setCount(schemaElementTypes.size());

            }
        }
        return modelCount;
    }

    public String resolve(QueryContext queryContext, ChatContext chatCtx, Set<Long> restrictiveModels) {
        SchemaModelClusterMapInfo mapInfo = queryContext.getModelClusterMapInfo();
        Set<String> matchedModelClusters = mapInfo.getElementMatchesByModelIds(restrictiveModels).keySet();
        Long modelId = queryContext.getRequest().getModelId();
        if (Objects.nonNull(modelId) && modelId > 0) {
            if (CollectionUtils.isEmpty(restrictiveModels) || restrictiveModels.contains(modelId)) {
                return getModelClusterByModelId(modelId, matchedModelClusters);
            }
            return null;
        }

        Map<String, SemanticQuery> modelQueryModes = new HashMap<>();
        for (String matchedModel : matchedModelClusters) {
            modelQueryModes.put(matchedModel, null);
        }
        if (modelQueryModes.size() == 1) {
            return modelQueryModes.keySet().stream().findFirst().get();
        }
        return selectModelBySchemaElementMatchScore(modelQueryModes, mapInfo);
    }

    private String getModelClusterByModelId(Long modelId, Set<String> modelClusterKeySet) {
        for (String modelClusterKey : modelClusterKeySet) {
            if (ModelCluster.build(modelClusterKey).getModelIds().contains(modelId)) {
                return modelClusterKey;
            }
        }
        return null;
    }

}
