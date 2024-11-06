package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

/**
 * HeuristicDataSetResolver select ONE most suitable data set out of matched data sets. The
 * selection is based on similarity comparison rule and the priority is like: 1.
 * maxSimilarity(matched dataset) 2. maxSimilarity(all matched metrics) 3. totalSimilarity(all
 * matched elements)
 */
@Slf4j
public class HeuristicDataSetResolver implements DataSetResolver {

    public Long resolve(ChatQueryContext chatQueryContext, Set<Long> agentDataSetIds) {
        SchemaMapInfo mapInfo = chatQueryContext.getMapInfo();
        Set<Long> matchedDataSets = mapInfo.getMatchedDataSetInfos();
        if (CollectionUtils.isNotEmpty(agentDataSetIds)) {
            matchedDataSets.retainAll(agentDataSetIds);
        }
        if (matchedDataSets.size() == 1) {
            return matchedDataSets.stream().findFirst().get();
        }
        return selectDataSetByMatchSimilarity(mapInfo);
    }

    protected Long selectDataSetByMatchSimilarity(SchemaMapInfo schemaMap) {
        Map<Long, SemanticParseInfo.DataSetMatchResult> dataSetMatchRet =
                getDataSetMatchResult(schemaMap);
        Entry<Long, SemanticParseInfo.DataSetMatchResult> selectedDataset =
                dataSetMatchRet.entrySet().stream().sorted((o1, o2) -> {
                    double difference = o1.getValue().getMaxDatesetSimilarity()
                            - o2.getValue().getMaxDatesetSimilarity();
                    if (difference == 0) {
                        difference = o1.getValue().getMaxMetricSimilarity()
                                - o2.getValue().getMaxMetricSimilarity();
                        if (difference == 0) {
                            difference = o1.getValue().getTotalSimilarity()
                                    - o2.getValue().getTotalSimilarity();
                        }
                        if (difference == 0) {
                            difference = o1.getValue().getMaxMetricUseCnt()
                                    - o2.getValue().getMaxMetricUseCnt();
                        }
                    }
                    return difference >= 0 ? -1 : 1;
                }).findFirst().orElse(null);
        if (selectedDataset != null) {
            log.info("selectDataSet with multiple DataSets [{}]", selectedDataset.getKey());
            return selectedDataset.getKey();
        }

        return null;
    }

    protected Map<Long, SemanticParseInfo.DataSetMatchResult> getDataSetMatchResult(
            SchemaMapInfo schemaMap) {
        Map<Long, SemanticParseInfo.DataSetMatchResult> dateSetMatchRet = new HashMap<>();
        for (Entry<Long, List<SchemaElementMatch>> entry : schemaMap.getDataSetElementMatches()
                .entrySet()) {
            double maxMetricSimilarity = 0;
            double maxDatasetSimilarity = 0;
            double totalSimilarity = 0;
            long maxMetricUseCnt = 0L;
            for (SchemaElementMatch match : entry.getValue()) {
                if (SchemaElementType.DATASET.equals(match.getElement().getType())) {
                    maxDatasetSimilarity = Math.max(maxDatasetSimilarity, match.getSimilarity());
                }
                if (SchemaElementType.METRIC.equals(match.getElement().getType())) {
                    maxMetricSimilarity = Math.max(maxMetricSimilarity, match.getSimilarity());
                    if (Objects.nonNull(match.getElement().getUseCnt())) {
                        maxMetricUseCnt = Math.max(maxMetricUseCnt, match.getElement().getUseCnt());
                    }
                }
                totalSimilarity += match.getSimilarity();
            }
            dateSetMatchRet.put(entry.getKey(),
                    SemanticParseInfo.DataSetMatchResult.builder()
                            .maxMetricSimilarity(maxMetricSimilarity)
                            .maxDatesetSimilarity(maxDatasetSimilarity)
                            .totalSimilarity(totalSimilarity).build());
        }

        return dateSetMatchRet;
    }
}
