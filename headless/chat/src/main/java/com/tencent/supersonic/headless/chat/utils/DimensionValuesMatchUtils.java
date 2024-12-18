package com.tencent.supersonic.headless.chat.utils;

import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
@Slf4j
public class DimensionValuesMatchUtils {

    private static final String DIMENSION_ID_53 = "53";
    private static final String DIMENSION_ID_54 = "54";
    private static final String DIMENSION_ID_55 = "55";
    private static final String DIMENSION_ID_56 = "56";

    public static void processDimensions(Map<SchemaElementType, List<String>> elementTypeToNatureMap,
                                         ChatQueryContext chatQueryContext) {
        Set<Long> dataSetIds = chatQueryContext.getRequest().getDataSetIds();
        if (!dataSetIds.contains(5L)) {
            return;
        }

        Set<String> dimensionIdsFromElementMap = extractDimensionIds(elementTypeToNatureMap);

        if (Boolean.TRUE.equals(getJudgeByType(dimensionIdsFromElementMap))) {
            return;
        }

        List<Map.Entry<String, String>> dimensionValusAndIdMap = generateDimensionValusAndIdMap(elementTypeToNatureMap);
        storeToChatQueryContext(chatQueryContext, dimensionValusAndIdMap);
    }

    private static Set<String> extractDimensionIds(Map<SchemaElementType, List<String>> elementTypeToNatureMap) {
        return elementTypeToNatureMap.values().stream()
                .flatMap(List::stream)
                .map(value -> {
                    String[] parts = value.split("_");
                    return parts.length == 3 ? parts[2] : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static void storeToChatQueryContext(ChatQueryContext chatQueryContext, List<Map.Entry<String, String>> dimensionValusAndIdMap) {
        chatQueryContext.setIsTip(true);
        chatQueryContext.setSchemaValusByTerm(dimensionValusAndIdMap);
    }

    private static Boolean getJudgeByType(Set<String> dimensionIds) {
        boolean has53 = dimensionIds.contains(DIMENSION_ID_53);
        boolean has54 = dimensionIds.contains(DIMENSION_ID_54);
        boolean has55 = dimensionIds.contains(DIMENSION_ID_55);
        boolean has56 = dimensionIds.contains(DIMENSION_ID_56);

        // 条件判断
        if (dimensionIds.stream().filter(DIMENSION_ID_53::equals).count() == 1 && !has54 && !has55 && !has56) {
            return true;
        }

        // 条件2：当维度ID同时存在53、54、55时，返回true
        if (has53 && has54 && has55 ) {
            return true;
        }

        // 条件3：当维度ID 53、54、55、56 都不存在时，返回true
        if (!has53 && !has54 && !has55 && !has56) {
            return true;
        }

        return false;
    }
    private static List<Map.Entry<String, String>> generateDimensionValusAndIdMap(Map<SchemaElementType, List<String>> elementTypeToNatureMap) {
        return elementTypeToNatureMap.values().stream()
                .flatMap(List::stream)
                .map(value -> value.split("_"))
                .filter(parts -> parts.length >= 3)
                .map(parts -> new AbstractMap.SimpleEntry<>(parts[2], parts[0])) // ID 和名称
                .collect(Collectors.toList());
    }

}
