package com.tencent.supersonic.chat.mapper;

import com.hankcs.hanlp.algorithm.EditDistance;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.config.OptimizationConfig;
import com.tencent.supersonic.chat.service.AgentService;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.knowledge.utils.NatureHelper;
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

/**
 * Mapper helper
 */

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

    public Integer getStepOffset(List<Integer> termList, Integer index) {
        for (int j = 0; j < termList.size() - 1; j++) {
            if (termList.get(j) <= index && termList.get(j + 1) > index) {
                return termList.get(j);
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

    public Set<Long> getModelIds(QueryReq request) {

        Long modelId = request.getModelId();

        AgentService agentService = ContextUtils.getBean(AgentService.class);

        Set<Long> detectModelIds = agentService.getModelIds(request.getAgentId(), null);
        //contains all
        if (agentService.containsAllModel(detectModelIds)) {
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
