package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MapFilter {

    public static void filter(ChatQueryContext chatQueryContext) {
        filterByDataSetId(chatQueryContext);
        filterByDetectWordLenLessThanOne(chatQueryContext);
        twoCharactersMustEqual(chatQueryContext);
        switch (chatQueryContext.getRequest().getQueryDataType()) {
            case TAG:
                filterByQueryDataType(chatQueryContext, element -> !(element.getIsTag() > 0));
                break;
            case METRIC:
                filterByQueryDataType(chatQueryContext,
                        element -> !SchemaElementType.METRIC.equals(element.getType()));
                break;
            case DIMENSION:
                filterByQueryDataType(chatQueryContext, element -> {
                    boolean isDimensionOrValue =
                            SchemaElementType.DIMENSION.equals(element.getType())
                                    || SchemaElementType.VALUE.equals(element.getType());
                    return !isDimensionOrValue;
                });
                break;
            case ALL:
            default:
                break;
        }
        filterByRules(chatQueryContext);
    }

    public static void filterByDataSetId(ChatQueryContext chatQueryContext) {
        Set<Long> dataSetIds = chatQueryContext.getRequest().getDataSetIds();
        if (CollectionUtils.isEmpty(dataSetIds)) {
            return;
        }
        Set<Long> dataSetIdInMapInfo =
                new HashSet<>(chatQueryContext.getMapInfo().getDataSetElementMatches().keySet());
        for (Long dataSetId : dataSetIdInMapInfo) {
            if (!dataSetIds.contains(dataSetId)) {
                chatQueryContext.getMapInfo().getDataSetElementMatches().remove(dataSetId);
            }
        }
    }

    public static void filterByDetectWordLenLessThanOne(ChatQueryContext chatQueryContext) {
        Map<Long, List<SchemaElementMatch>> dataSetElementMatches =
                chatQueryContext.getMapInfo().getDataSetElementMatches();
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : dataSetElementMatches.entrySet()) {
            List<SchemaElementMatch> value = entry.getValue();
            if (!CollectionUtils.isEmpty(value)) {
                value.removeIf(schemaElementMatch -> StringUtils
                        .length(schemaElementMatch.getDetectWord()) <= 1
                        && !schemaElementMatch.isLlmMatched());
            }
        }
    }

    private static void twoCharactersMustEqual(ChatQueryContext chatQueryContext) {
        Map<Long, List<SchemaElementMatch>> dataSetElementMatches =
                chatQueryContext.getMapInfo().getDataSetElementMatches();
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : dataSetElementMatches.entrySet()) {
            List<SchemaElementMatch> value = entry.getValue();
            if (!CollectionUtils.isEmpty(value)) {
                value.removeIf(
                        schemaElementMatch -> StringUtils.length(schemaElementMatch.getWord()) <= 2
                                && schemaElementMatch.getSimilarity() < 1);
            }
        }
    }

    public static void filterByQueryDataType(ChatQueryContext chatQueryContext,
            Predicate<SchemaElement> needRemovePredicate) {
        Map<Long, List<SchemaElementMatch>> dataSetElementMatches =
                chatQueryContext.getMapInfo().getDataSetElementMatches();
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : dataSetElementMatches.entrySet()) {
            List<SchemaElementMatch> schemaElementMatches = entry.getValue();
            schemaElementMatches.removeIf(schemaElementMatch -> {
                SchemaElement element = schemaElementMatch.getElement();
                SchemaElementType type = element.getType();

                boolean isEntityOrDatasetOrId =
                        SchemaElementType.DATASET.equals(type) || SchemaElementType.ID.equals(type);

                return !isEntityOrDatasetOrId && needRemovePredicate.test(element);
            });
        }
    }

    public static void filterByRules(ChatQueryContext chatQueryContext) {
        Map<Long, List<SchemaElementMatch>> dataSetElementMatches =
                chatQueryContext.getMapInfo().getDataSetElementMatches();

        for (Map.Entry<Long, List<SchemaElementMatch>> entry : dataSetElementMatches.entrySet()) {
            filterByExactMatch(entry.getValue());
            filterInExactMatch(entry.getValue());
        }
    }

    public static void filterByExactMatch(List<SchemaElementMatch> matches) {
        // Group by detectWord
        Map<String, List<SchemaElementMatch>> groupedByDetectWord =
                matches.stream().collect(Collectors.groupingBy(SchemaElementMatch::getDetectWord));

        List<SchemaElementMatch> result = new ArrayList<>();

        for (Map.Entry<String, List<SchemaElementMatch>> entry : groupedByDetectWord.entrySet()) {
            List<SchemaElementMatch> group = entry.getValue();

            // Filter out objects with similarity=1.0
            List<SchemaElementMatch> fullMatches = group.stream()
                    .filter(SchemaElementMatch::isFullMatched).collect(Collectors.toList());

            if (!fullMatches.isEmpty()) {
                // If there are objects with similarity=1.0, choose the one with the longest
                // detectWord and smallest offset
                SchemaElementMatch bestMatch = fullMatches.stream()
                        .max(Comparator.comparing(
                                (SchemaElementMatch match) -> match.getDetectWord().length()))
                        .orElse(null);
                if (bestMatch != null) {
                    result.add(bestMatch);
                }
            } else {
                // If there are no objects with similarity=1.0, keep all objects with similarity<1.0
                result.addAll(group);
            }
        }
        matches.clear();
        matches.addAll(result);
    }

    public static void filterInExactMatch(List<SchemaElementMatch> matches) {
        Map<String, List<SchemaElementMatch>> fullMatches =
                matches.stream().filter(schemaElementMatch -> schemaElementMatch.isFullMatched())
                        .collect(Collectors.groupingBy(SchemaElementMatch::getWord));
        Set<String> keys = new HashSet<>(fullMatches.keySet());
        for (String key1 : keys) {
            for (String key2 : keys) {
                if (!key1.equals(key2) && key1.contains(key2)) {
                    fullMatches.remove(key2);
                }
            }
        }
        List<SchemaElementMatch> notFullMatches =
                matches.stream().filter(schemaElementMatch -> !schemaElementMatch.isFullMatched())
                        .collect(Collectors.toList());

        List<SchemaElementMatch> mergedMatches = new ArrayList<>();
        fullMatches.values().forEach(mergedMatches::addAll);
        mergedMatches.addAll(notFullMatches);
        matches.clear();
        matches.addAll(mergedMatches);
    }
}
