package com.tencent.supersonic.chat.core.parser.sql.llm;

import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.core.query.SemanticQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
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
public class HeuristicViewResolver implements ViewResolver {

    protected static Long selectViewBySchemaElementMatchScore(Map<Long, SemanticQuery> viewQueryModes,
            SchemaMapInfo schemaMap) {
        //view count priority
        Long viewIdByViewCount = getViewIdByMatchViewScore(schemaMap);
        if (Objects.nonNull(viewIdByViewCount)) {
            log.info("selectView by view count:{}", viewIdByViewCount);
            return viewIdByViewCount;
        }

        Map<Long, ViewMatchResult> viewTypeMap = getViewTypeMap(schemaMap);
        if (viewTypeMap.size() == 1) {
            Long viewSelect = new ArrayList<>(viewTypeMap.entrySet()).get(0).getKey();
            if (viewQueryModes.containsKey(viewSelect)) {
                log.info("selectView with only one View [{}]", viewSelect);
                return viewSelect;
            }
        } else {
            Map.Entry<Long, ViewMatchResult> maxView = viewTypeMap.entrySet().stream()
                    .filter(entry -> viewQueryModes.containsKey(entry.getKey()))
                    .sorted((o1, o2) -> {
                        int difference = o2.getValue().getCount() - o1.getValue().getCount();
                        if (difference == 0) {
                            return (int) ((o2.getValue().getMaxSimilarity()
                                    - o1.getValue().getMaxSimilarity()) * 100);
                        }
                        return difference;
                    }).findFirst().orElse(null);
            if (maxView != null) {
                log.info("selectView with multiple Views [{}]", maxView.getKey());
                return maxView.getKey();
            }
        }
        return null;
    }

    private static Long getViewIdByMatchViewScore(SchemaMapInfo schemaMap) {
        Map<Long, List<SchemaElementMatch>> viewElementMatches = schemaMap.getViewElementMatches();
        // calculate view match score, matched element gets 1.0 point, and inherit element gets 0.5 point
        Map<Long, Double> viewIdToViewScore = new HashMap<>();
        if (Objects.nonNull(viewElementMatches)) {
            for (Entry<Long, List<SchemaElementMatch>> viewElementMatch : viewElementMatches.entrySet()) {
                Long viewId = viewElementMatch.getKey();
                List<Double> viewMatchesScore = viewElementMatch.getValue().stream()
                        .filter(elementMatch -> elementMatch.getSimilarity() >= 1)
                        .filter(elementMatch -> SchemaElementType.VIEW.equals(elementMatch.getElement().getType()))
                        .map(elementMatch -> elementMatch.isInherited() ? 0.5 : 1.0).collect(Collectors.toList());

                if (!CollectionUtils.isEmpty(viewMatchesScore)) {
                    // get sum of view match score
                    double score = viewMatchesScore.stream().mapToDouble(Double::doubleValue).sum();
                    viewIdToViewScore.put(viewId, score);
                }
            }
            Entry<Long, Double> maxViewScore = viewIdToViewScore.entrySet().stream()
                    .max(Comparator.comparingDouble(Entry::getValue)).orElse(null);
            log.info("maxViewCount:{},viewIdToViewCount:{}", maxViewScore, viewIdToViewScore);
            if (Objects.nonNull(maxViewScore)) {
                return maxViewScore.getKey();
            }
        }
        return null;
    }

    public static Map<Long, ViewMatchResult> getViewTypeMap(SchemaMapInfo schemaMap) {
        Map<Long, ViewMatchResult> viewCount = new HashMap<>();
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : schemaMap.getViewElementMatches().entrySet()) {
            List<SchemaElementMatch> schemaElementMatches = schemaMap.getMatchedElements(entry.getKey());
            if (schemaElementMatches != null && schemaElementMatches.size() > 0) {
                if (!viewCount.containsKey(entry.getKey())) {
                    viewCount.put(entry.getKey(), new ViewMatchResult());
                }
                ViewMatchResult viewMatchResult = viewCount.get(entry.getKey());
                Set<SchemaElementType> schemaElementTypes = new HashSet<>();
                schemaElementMatches.stream()
                        .forEach(schemaElementMatch -> schemaElementTypes.add(
                                schemaElementMatch.getElement().getType()));
                SchemaElementMatch schemaElementMatchMax = schemaElementMatches.stream()
                        .sorted((o1, o2) ->
                                ((int) ((o2.getSimilarity() - o1.getSimilarity()) * 100))
                        ).findFirst().orElse(null);
                if (schemaElementMatchMax != null) {
                    viewMatchResult.setMaxSimilarity(schemaElementMatchMax.getSimilarity());
                }
                viewMatchResult.setCount(schemaElementTypes.size());

            }
        }
        return viewCount;
    }

    public Long resolve(QueryContext queryContext, Set<Long> agentViewIds) {
        SchemaMapInfo mapInfo = queryContext.getMapInfo();
        Set<Long> matchedViews = mapInfo.getMatchedViewInfos();
        Long viewId = queryContext.getViewId();
        if (Objects.nonNull(viewId) && viewId > 0) {
            if (CollectionUtils.isEmpty(agentViewIds) || agentViewIds.contains(viewId)) {
                return viewId;
            }
            return null;
        }
        if (CollectionUtils.isNotEmpty(agentViewIds)) {
            matchedViews.retainAll(agentViewIds);
        }
        Map<Long, SemanticQuery> viewQueryModes = new HashMap<>();
        for (Long viewIds : matchedViews) {
            viewQueryModes.put(viewIds, null);
        }
        if (viewQueryModes.size() == 1) {
            return viewQueryModes.keySet().stream().findFirst().get();
        }
        return selectViewBySchemaElementMatchScore(viewQueryModes, mapInfo);
    }

}
