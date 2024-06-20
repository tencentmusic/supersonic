package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.chat.QueryContext;
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
public class HeuristicDataSetResolver implements DataSetResolver {

    protected static Long selectDataSetBySchemaElementMatchScore(Map<Long, SemanticQuery> dataSetQueryModes,
            SchemaMapInfo schemaMap) {
        //dataSet count priority
        Long dataSetIdByDataSetCount = getDataSetIdByMatchDataSetScore(schemaMap);
        if (Objects.nonNull(dataSetIdByDataSetCount)) {
            log.info("selectDataSet by dataSet count:{}", dataSetIdByDataSetCount);
            return dataSetIdByDataSetCount;
        }

        Map<Long, DataSetMatchResult> dataSetTypeMap = getDataSetTypeMap(schemaMap);
        if (dataSetTypeMap.size() == 1) {
            Long dataSetSelect = new ArrayList<>(dataSetTypeMap.entrySet()).get(0).getKey();
            if (dataSetQueryModes.containsKey(dataSetSelect)) {
                log.info("selectDataSet with only one DataSet [{}]", dataSetSelect);
                return dataSetSelect;
            }
        } else {
            Entry<Long, DataSetMatchResult> maxDataSet = dataSetTypeMap.entrySet().stream()
                    .filter(entry -> dataSetQueryModes.containsKey(entry.getKey()))
                    .sorted((o1, o2) -> {
                        int difference = o2.getValue().getCount() - o1.getValue().getCount();
                        if (difference == 0) {
                            return (int) ((o2.getValue().getMaxSimilarity()
                                    - o1.getValue().getMaxSimilarity()) * 100);
                        }
                        return difference;
                    }).findFirst().orElse(null);
            if (maxDataSet != null) {
                log.info("selectDataSet with multiple DataSets [{}]", maxDataSet.getKey());
                return maxDataSet.getKey();
            }
        }
        return null;
    }

    private static Long getDataSetIdByMatchDataSetScore(SchemaMapInfo schemaMap) {
        Map<Long, List<SchemaElementMatch>> dataSetElementMatches = schemaMap.getDataSetElementMatches();
        // calculate dataSet match score, matched element gets 1.0 point, and inherit element gets 0.5 point
        Map<Long, Double> dataSetIdToDataSetScore = new HashMap<>();
        if (Objects.nonNull(dataSetElementMatches)) {
            for (Entry<Long, List<SchemaElementMatch>> dataSetElementMatch : dataSetElementMatches.entrySet()) {
                Long dataSetId = dataSetElementMatch.getKey();
                List<Double> dataSetMatchesScore = dataSetElementMatch.getValue().stream()
                        .filter(elementMatch -> elementMatch.getSimilarity() >= 1)
                        .filter(elementMatch -> SchemaElementType.DATASET.equals(elementMatch.getElement().getType()))
                        .map(elementMatch -> elementMatch.isInherited() ? 0.5 : 1.0).collect(Collectors.toList());

                if (!CollectionUtils.isEmpty(dataSetMatchesScore)) {
                    // get sum of dataSet match score
                    double score = dataSetMatchesScore.stream().mapToDouble(Double::doubleValue).sum();
                    dataSetIdToDataSetScore.put(dataSetId, score);
                }
            }
            Entry<Long, Double> maxDataSetScore = dataSetIdToDataSetScore.entrySet().stream()
                    .max(Comparator.comparingDouble(Entry::getValue)).orElse(null);
            log.info("maxDataSetCount:{},dataSetIdToDataSetCount:{}", maxDataSetScore, dataSetIdToDataSetScore);
            if (Objects.nonNull(maxDataSetScore)) {
                return maxDataSetScore.getKey();
            }
        }
        return null;
    }

    public static Map<Long, DataSetMatchResult> getDataSetTypeMap(SchemaMapInfo schemaMap) {
        Map<Long, DataSetMatchResult> dataSetCount = new HashMap<>();
        for (Entry<Long, List<SchemaElementMatch>> entry : schemaMap.getDataSetElementMatches().entrySet()) {
            List<SchemaElementMatch> schemaElementMatches = schemaMap.getMatchedElements(entry.getKey());
            if (schemaElementMatches != null && schemaElementMatches.size() > 0) {
                if (!dataSetCount.containsKey(entry.getKey())) {
                    dataSetCount.put(entry.getKey(), new DataSetMatchResult());
                }
                DataSetMatchResult dataSetMatchResult = dataSetCount.get(entry.getKey());
                Set<SchemaElementType> schemaElementTypes = new HashSet<>();
                schemaElementMatches.stream()
                        .forEach(schemaElementMatch -> schemaElementTypes.add(
                                schemaElementMatch.getElement().getType()));
                SchemaElementMatch schemaElementMatchMax = schemaElementMatches.stream()
                        .sorted((o1, o2) ->
                                ((int) ((o2.getSimilarity() - o1.getSimilarity()) * 100))
                        ).findFirst().orElse(null);
                if (schemaElementMatchMax != null) {
                    dataSetMatchResult.setMaxSimilarity(schemaElementMatchMax.getSimilarity());
                }
                dataSetMatchResult.setCount(schemaElementTypes.size());

            }
        }
        return dataSetCount;
    }

    public Long resolve(QueryContext queryContext, Set<Long> agentDataSetIds) {
        SchemaMapInfo mapInfo = queryContext.getMapInfo();
        Set<Long> matchedDataSets = mapInfo.getMatchedDataSetInfos();
        if (CollectionUtils.isNotEmpty(agentDataSetIds)) {
            matchedDataSets.retainAll(agentDataSetIds);
        }
        Map<Long, SemanticQuery> dataSetQueryModes = new HashMap<>();
        for (Long dataSetIds : matchedDataSets) {
            dataSetQueryModes.put(dataSetIds, null);
        }
        if (dataSetQueryModes.size() == 1) {
            return dataSetQueryModes.keySet().stream().findFirst().get();
        }
        return selectDataSetBySchemaElementMatchScore(dataSetQueryModes, mapInfo);
    }

}
