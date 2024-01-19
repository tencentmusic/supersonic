package com.tencent.supersonic.chat.core.mapper;

import com.hankcs.hanlp.algorithm.EditDistance;
import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.core.agent.Agent;
import com.tencent.supersonic.chat.core.config.OptimizationConfig;
import com.tencent.supersonic.chat.core.utils.NatureHelper;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Data
@Service
@Slf4j
public class MapperHelper {

    @Autowired
    private OptimizationConfig optimizationConfig;

    public Integer getStepIndex(Map<Integer, Integer> regOffsetToLength, Integer index) {
        Integer subRegLength = regOffsetToLength.get(index);
        if (Objects.nonNull(subRegLength)) {
            index = index + subRegLength;
        } else {
            index++;
        }
        return index;
    }

    public Integer getStepOffset(List<Term> termList, Integer index) {
        List<Integer> offsetList = termList.stream().sorted(Comparator.comparing(Term::getOffset))
                .map(term -> term.getOffset()).collect(Collectors.toList());

        for (int j = 0; j < termList.size() - 1; j++) {
            if (offsetList.get(j) <= index && offsetList.get(j + 1) > index) {
                return offsetList.get(j);
            }
        }
        return index;
    }

    public double getThresholdMatch(List<String> natures) {
        if (existDimensionValues(natures)) {
            return optimizationConfig.getDimensionValueThresholdConfig();
        }
        return optimizationConfig.getMetricDimensionThresholdConfig();
    }

    /***
     * exist dimension values
     * @param natures
     * @return
     */
    public boolean existDimensionValues(List<String> natures) {
        for (String nature : natures) {
            if (NatureHelper.isDimensionValueModelId(nature)) {
                return true;
            }
        }
        return false;
    }

    /***
     * get similarity
     * @param detectSegment
     * @param matchName
     * @return
     */
    public double getSimilarity(String detectSegment, String matchName) {
        String detectSegmentLower = detectSegment == null ? null : detectSegment.toLowerCase();
        String matchNameLower = matchName == null ? null : matchName.toLowerCase();
        return 1 - (double) EditDistance.compute(detectSegmentLower, matchNameLower) / Math.max(matchName.length(),
                detectSegment.length());
    }

    public Set<Long> getModelIds(Long modelId, Agent agent) {

        Set<Long> detectModelIds = new HashSet<>();
        if (Objects.nonNull(agent)) {
            detectModelIds = agent.getModelIds(null);
        }
        //contains all
        if (Agent.containsAllModel(detectModelIds)) {
            if (Objects.nonNull(modelId) && modelId > 0) {
                Set<Long> result = new HashSet<>();
                result.add(modelId);
                return result;
            }
            return new HashSet<>();
        }

        if (Objects.nonNull(detectModelIds)) {
            detectModelIds = detectModelIds.stream().filter(entry -> entry > 0).collect(Collectors.toSet());
        }

        if (Objects.nonNull(modelId) && modelId > 0 && Objects.nonNull(detectModelIds)) {
            if (detectModelIds.contains(modelId)) {
                Set<Long> result = new HashSet<>();
                result.add(modelId);
                return result;
            }
        }
        return detectModelIds;
    }
}
