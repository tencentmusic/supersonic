package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Schema Match Helper
 */
@Slf4j
public class SchemaMatchHelper {

    public static void removeSamePrefixDetectWord(List<SchemaElementMatch> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return;
        }

        Set<String> metricDimensionDetectWordSet = matches.stream()
                .filter(SchemaMatchHelper::isMetricOrDimension)
                .map(SchemaElementMatch::getDetectWord).collect(Collectors.toSet());

        matches.removeIf(elementMatch -> {
            if (!isMetricOrDimension(elementMatch)) {
                return false;
            }
            for (String detectWord : metricDimensionDetectWordSet) {
                if (detectWord.startsWith(elementMatch.getDetectWord())
                        && detectWord.length() > elementMatch.getDetectWord().length()) {
                    return true;
                }
            }
            return false;
        });
    }

    private static boolean isMetricOrDimension(SchemaElementMatch elementMatch) {
        return SchemaElementType.METRIC.equals(elementMatch.getElement().getType())
                || SchemaElementType.DIMENSION.equals(elementMatch.getElement().getType());
    }
}
