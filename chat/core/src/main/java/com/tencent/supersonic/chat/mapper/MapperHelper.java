package com.tencent.supersonic.chat.mapper;

import com.hankcs.hanlp.algorithm.EditDistance;
import com.tencent.supersonic.chat.utils.NatureHelper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Mapper helper
 */

@Data
@Service
@Slf4j
public class MapperHelper {

    @Value("${one.detection.size:6}")
    private Integer oneDetectionSize;
    @Value("${one.detection.max.size:20}")
    private Integer oneDetectionMaxSize;
    @Value("${metric.dimension.threshold:0.3}")
    private Double metricDimensionThresholdConfig;

    @Value("${metric.dimension.min.threshold:0.3}")
    private Double metricDimensionMinThresholdConfig;
    @Value("${dimension.value.threshold:0.5}")
    private Double dimensionValueThresholdConfig;

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
            return dimensionValueThresholdConfig;
        }
        return metricDimensionThresholdConfig;
    }

    /***
     * exist dimension values
     * @param natures
     * @return
     */
    public boolean existDimensionValues(List<String> natures) {
        for (String nature : natures) {
            if (NatureHelper.isDimensionValueClassId(nature)) {
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


}