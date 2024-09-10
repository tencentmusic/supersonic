package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class MapFilter {

    public static void filterByDataSetId(ChatQueryContext chatQueryContext) {
        Set<Long> dataSetIds = chatQueryContext.getDataSetIds();
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
                value.removeIf(
                        schemaElementMatch ->
                                StringUtils.length(schemaElementMatch.getDetectWord()) <= 1);
            }
        }
    }

    public static void filterByQueryDataType(
            ChatQueryContext chatQueryContext, Predicate<SchemaElement> needRemovePredicate) {
        chatQueryContext
                .getMapInfo()
                .getDataSetElementMatches()
                .values()
                .forEach(
                        schemaElementMatches -> {
                            schemaElementMatches.removeIf(
                                    schemaElementMatch -> {
                                        SchemaElement element = schemaElementMatch.getElement();
                                        SchemaElementType type = element.getType();

                                        boolean isEntityOrDatasetOrId =
                                                SchemaElementType.ENTITY.equals(type)
                                                        || SchemaElementType.DATASET.equals(type)
                                                        || SchemaElementType.ID.equals(type);

                                        return !isEntityOrDatasetOrId
                                                && needRemovePredicate.test(element);
                                    });
                        });
    }
}
