package com.tencent.supersonic.chat.parser.llm.s2sql;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

@Slf4j
public class HeuristicModelResolver implements ModelResolver {

    protected static Long selectModelBySchemaElementMatchScore(Map<Long, SemanticQuery> modelQueryModes,
            SchemaMapInfo schemaMap) {
        //model count priority
        Long modelIdByModelCount = getModelIdByMatchModelScore(schemaMap);
        if (Objects.nonNull(modelIdByModelCount)) {
            log.info("selectModel by model count:{}", modelIdByModelCount);
            return modelIdByModelCount;
        }

        Map<Long, ModelMatchResult> modelTypeMap = getModelTypeMap(schemaMap);
        if (modelTypeMap.size() == 1) {
            Long modelSelect = modelTypeMap.entrySet().stream().collect(Collectors.toList()).get(0).getKey();
            if (modelQueryModes.containsKey(modelSelect)) {
                log.info("selectModel with only one Model [{}]", modelSelect);
                return modelSelect;
            }
        } else {

            Map.Entry<Long, ModelMatchResult> maxModel = modelTypeMap.entrySet().stream()
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
        return 0L;
    }

    private static Long getModelIdByMatchModelScore(SchemaMapInfo schemaMap) {
        Map<Long, List<SchemaElementMatch>> modelElementMatches = schemaMap.getModelElementMatches();
        // calculate model match score, matched element gets 1.0 point, and inherit element gets 0.5 point
        Map<Long, Double> modelIdToModelScore = new HashMap<>();
        if (Objects.nonNull(modelElementMatches)) {
            for (Entry<Long, List<SchemaElementMatch>> modelElementMatch : modelElementMatches.entrySet()) {
                Long modelId = modelElementMatch.getKey();
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
            Entry<Long, Double> maxModelScore = modelIdToModelScore.entrySet().stream()
                    .max(Comparator.comparingDouble(o -> o.getValue())).orElse(null);
            log.info("maxModelCount:{},modelIdToModelCount:{}", maxModelScore, modelIdToModelScore);
            if (Objects.nonNull(maxModelScore)) {
                return maxModelScore.getKey();
            }
        }
        return null;
    }

    /**
     * to check can switch Model if context exit Model
     *
     * @return false will use context Model, true will use other Model , maybe include context Model
     */
    protected static boolean isAllowSwitch(Map<Long, SemanticQuery> modelQueryModes, SchemaMapInfo schemaMap,
            ChatContext chatCtx, QueryReq searchCtx,
            Long modelId, Set<Long> restrictiveModels) {
        if (!Objects.nonNull(modelId) || modelId <= 0) {
            return true;
        }
        // except content Model, calculate the number of types for each Model, if numbers<=1 will not switch
        Map<Long, ModelMatchResult> modelTypeMap = getModelTypeMap(schemaMap);
        log.info("isAllowSwitch ModelTypeMap [{}]", modelTypeMap);
        long otherModelTypeNumBigOneCount = modelTypeMap.entrySet().stream()
                .filter(entry -> modelQueryModes.containsKey(entry.getKey()) && !entry.getKey().equals(modelId))
                .filter(entry -> entry.getValue().getCount() > 1).count();
        if (otherModelTypeNumBigOneCount >= 1) {
            return true;
        }
        // if query text only contain time , will not switch
        if (!CollectionUtils.isEmpty(modelQueryModes.values())) {
            for (SemanticQuery semanticQuery : modelQueryModes.values()) {
                if (semanticQuery == null) {
                    continue;
                }
                SemanticParseInfo semanticParseInfo = semanticQuery.getParseInfo();
                if (semanticParseInfo == null) {
                    continue;
                }
                if (searchCtx.getQueryText() != null && semanticParseInfo.getDateInfo() != null) {
                    if (semanticParseInfo.getDateInfo().getDetectWord() != null) {
                        if (semanticParseInfo.getDateInfo().getDetectWord()
                                .equalsIgnoreCase(searchCtx.getQueryText())) {
                            log.info("timeParseResults is not null , can not switch context , timeParseResults:{},",
                                    semanticParseInfo.getDateInfo());
                            return false;
                        }
                    }
                }
            }
        }

        if (CollectionUtils.isNotEmpty(restrictiveModels) && !restrictiveModels.contains(modelId)) {
            return true;
        }
        // if context Model not  in schemaMap , will switch
        if (schemaMap.getMatchedElements(modelId) == null || schemaMap.getMatchedElements(modelId).size() <= 0) {
            log.info("modelId not in schemaMap ");
            return true;
        }
        // other will not switch
        return false;
    }

    public static Map<Long, ModelMatchResult> getModelTypeMap(SchemaMapInfo schemaMap) {
        Map<Long, ModelMatchResult> modelCount = new HashMap<>();
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : schemaMap.getModelElementMatches().entrySet()) {
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


    public Long resolve(QueryContext queryContext, ChatContext chatCtx, Set<Long> restrictiveModels) {
        Long modelId = queryContext.getRequest().getModelId();
        if (Objects.nonNull(modelId) && modelId > 0) {
            if (CollectionUtils.isEmpty(restrictiveModels)) {
                return modelId;
            }
            if (restrictiveModels.contains(modelId)) {
                return modelId;
            } else {
                return null;
            }
        }
        SchemaMapInfo mapInfo = queryContext.getMapInfo();
        Set<Long> matchedModels = mapInfo.getMatchedModels();
        if (CollectionUtils.isNotEmpty(restrictiveModels)) {
            matchedModels = matchedModels.stream()
                    .filter(restrictiveModels::contains)
                    .collect(Collectors.toSet());
        }
        Map<Long, SemanticQuery> modelQueryModes = new HashMap<>();
        for (Long matchedModel : matchedModels) {
            modelQueryModes.put(matchedModel, null);
        }
        if (modelQueryModes.size() == 1) {
            return modelQueryModes.keySet().stream().findFirst().get();
        }
        return resolve(modelQueryModes, queryContext, chatCtx,
                queryContext.getMapInfo(), restrictiveModels);
    }

    public Long resolve(Map<Long, SemanticQuery> modelQueryModes, QueryContext queryContext,
            ChatContext chatCtx, SchemaMapInfo schemaMap, Set<Long> restrictiveModels) {
        Long selectModel = selectModel(modelQueryModes, queryContext.getRequest(),
                chatCtx, schemaMap, restrictiveModels);
        if (selectModel > 0) {
            log.info("selectModel {} ", selectModel);
            return selectModel;
        }
        // get the max SchemaElementType match score
        return selectModelBySchemaElementMatchScore(modelQueryModes, schemaMap);
    }

    public Long selectModel(Map<Long, SemanticQuery> modelQueryModes, QueryReq queryContext,
            ChatContext chatCtx,
            SchemaMapInfo schemaMap, Set<Long> restrictiveModels) {
        // if QueryContext has modelId and in ModelQueryModes
        if (modelQueryModes.containsKey(queryContext.getModelId())) {
            log.info("selectModel from QueryContext [{}]", queryContext.getModelId());
            return queryContext.getModelId();
        }
        // if ChatContext has modelId and in ModelQueryModes
        if (chatCtx.getParseInfo().getModelId() > 0) {
            Long modelId = chatCtx.getParseInfo().getModelId();
            if (!isAllowSwitch(modelQueryModes, schemaMap, chatCtx, queryContext, modelId, restrictiveModels)) {
                log.info("selectModel from ChatContext [{}]", modelId);
                return modelId;
            }
        }
        // default 0
        return 0L;
    }
}
