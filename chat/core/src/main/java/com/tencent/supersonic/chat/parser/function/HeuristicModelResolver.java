package com.tencent.supersonic.chat.parser.function;

import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;

@Slf4j
public class HeuristicModelResolver implements ModelResolver {

    protected static Long selectModelBySchemaElementCount(Map<Long, SemanticQuery> ModelQueryModes,
            SchemaMapInfo schemaMap) {
        Map<Long, ModelMatchResult> ModelTypeMap = getModelTypeMap(schemaMap);
        if (ModelTypeMap.size() == 1) {
            Long ModelSelect = ModelTypeMap.entrySet().stream().collect(Collectors.toList()).get(0).getKey();
            if (ModelQueryModes.containsKey(ModelSelect)) {
                log.info("selectModel with only one Model [{}]", ModelSelect);
                return ModelSelect;
            }
        } else {
            Map.Entry<Long, ModelMatchResult> maxModel = ModelTypeMap.entrySet().stream()
                    .filter(entry -> ModelQueryModes.containsKey(entry.getKey()))
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

    /**
     * to check can switch Model if context exit Model
     *
     * @return false will use context Model, true will use other Model , maybe include context Model
     */
    protected static boolean isAllowSwitch(Map<Long, SemanticQuery> ModelQueryModes, SchemaMapInfo schemaMap,
            ChatContext chatCtx, QueryReq searchCtx, Long modelId, Set<Long> restrictiveModels) {
        if (!Objects.nonNull(modelId) || modelId <= 0) {
            return true;
        }
        // except content Model, calculate the number of types for each Model, if numbers<=1 will not switch
        Map<Long, ModelMatchResult> ModelTypeMap = getModelTypeMap(schemaMap);
        log.info("isAllowSwitch ModelTypeMap [{}]", ModelTypeMap);
        long otherModelTypeNumBigOneCount = ModelTypeMap.entrySet().stream()
                .filter(entry -> ModelQueryModes.containsKey(entry.getKey()) && !entry.getKey().equals(modelId))
                .filter(entry -> entry.getValue().getCount() > 1).count();
        if (otherModelTypeNumBigOneCount >= 1) {
            return true;
        }
        // if query text only contain time , will not switch
        if (!CollectionUtils.isEmpty(ModelQueryModes.values())) {
            for (SemanticQuery semanticQuery : ModelQueryModes.values()) {
                if (semanticQuery == null) {
                    continue;
                }
                SemanticParseInfo semanticParseInfo = semanticQuery.getParseInfo();
                if (semanticParseInfo == null) {
                    continue;
                }
                if (searchCtx.getQueryText() != null && semanticParseInfo.getDateInfo() != null) {
                    if (semanticParseInfo.getDateInfo().getDetectWord() != null) {
                        if (semanticParseInfo.getDateInfo().getDetectWord().equalsIgnoreCase(searchCtx.getQueryText())) {
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
        Map<Long, ModelMatchResult> ModelCount = new HashMap<>();
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : schemaMap.getModelElementMatches().entrySet()) {
            List<SchemaElementMatch> schemaElementMatches = schemaMap.getMatchedElements(entry.getKey());
            if (schemaElementMatches != null && schemaElementMatches.size() > 0) {
                if (!ModelCount.containsKey(entry.getKey())) {
                    ModelCount.put(entry.getKey(), new ModelMatchResult());
                }
                ModelMatchResult ModelMatchResult = ModelCount.get(entry.getKey());
                Set<SchemaElementType> schemaElementTypes = new HashSet<>();
                schemaElementMatches.stream()
                        .forEach(schemaElementMatch -> schemaElementTypes.add(
                                schemaElementMatch.getElement().getType()));
                SchemaElementMatch schemaElementMatchMax = schemaElementMatches.stream()
                        .sorted((o1, o2) ->
                                ((int) ((o2.getSimilarity() - o1.getSimilarity()) * 100))
                        ).findFirst().orElse(null);
                if (schemaElementMatchMax != null) {
                    ModelMatchResult.setMaxSimilarity(schemaElementMatchMax.getSimilarity());
                }
                ModelMatchResult.setCount(schemaElementTypes.size());

            }
        }
        return ModelCount;
    }


    public Long resolve(QueryContext queryContext, ChatContext chatCtx, Set<Long> restrictiveModels) {
        Long modelId = queryContext.getRequest().getModelId();
        if (Objects.nonNull(modelId) && modelId > 0) {
            if (CollectionUtils.isNotEmpty(restrictiveModels) && restrictiveModels.contains(modelId)) {
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
        Map<Long, SemanticQuery> ModelQueryModes = new HashMap<>();
        for (Long matchedModel : matchedModels) {
            ModelQueryModes.put(matchedModel, null);
        }
        if(ModelQueryModes.size()==1){
            return ModelQueryModes.keySet().stream().findFirst().get();
        }
        return resolve(ModelQueryModes, queryContext, chatCtx,
                queryContext.getMapInfo(),restrictiveModels);
    }

    public Long resolve(Map<Long, SemanticQuery> ModelQueryModes, QueryContext queryContext,
            ChatContext chatCtx, SchemaMapInfo schemaMap, Set<Long> restrictiveModels) {
        Long selectModel = selectModel(ModelQueryModes, queryContext.getRequest(), chatCtx, schemaMap,restrictiveModels);
        if (selectModel > 0) {
            log.info("selectModel {} ", selectModel);
            return selectModel;
        }
        // get the max SchemaElementType number
        return selectModelBySchemaElementCount(ModelQueryModes, schemaMap);
    }

    public Long selectModel(Map<Long, SemanticQuery> ModelQueryModes, QueryReq queryContext,
            ChatContext chatCtx,
            SchemaMapInfo schemaMap, Set<Long> restrictiveModels) {
        // if QueryContext has modelId and in ModelQueryModes
        if (ModelQueryModes.containsKey(queryContext.getModelId())) {
            log.info("selectModel from QueryContext [{}]", queryContext.getModelId());
            return queryContext.getModelId();
        }
        // if ChatContext has modelId and in ModelQueryModes
        if (chatCtx.getParseInfo().getModelId() > 0) {
            Long modelId = chatCtx.getParseInfo().getModelId();
            if (!isAllowSwitch(ModelQueryModes, schemaMap, chatCtx, queryContext, modelId,restrictiveModels)) {
                log.info("selectModel from ChatContext [{}]", modelId);
                return modelId;
            }
        }
        // default 0
        return 0L;
    }
}
