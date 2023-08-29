package com.tencent.supersonic.chat.utils;

import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

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
                .filter(elementMatch -> isMetricOrDimension(elementMatch))
                .map(entry -> entry.getDetectWord()).collect(Collectors.toSet());

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
