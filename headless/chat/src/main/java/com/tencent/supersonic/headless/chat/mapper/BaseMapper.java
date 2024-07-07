package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseMapper implements SchemaMapper {

    @Override
    public void map(ChatQueryContext chatQueryContext) {

        String simpleName = this.getClass().getSimpleName();
        long startTime = System.currentTimeMillis();
        log.debug("before {},mapInfo:{}", simpleName,
                chatQueryContext.getMapInfo().getDataSetElementMatches());

        try {
            doMap(chatQueryContext);
            filter(chatQueryContext);
        } catch (Exception e) {
            log.error("work error", e);
        }

        long cost = System.currentTimeMillis() - startTime;
        log.debug("after {},cost:{},mapInfo:{}", simpleName, cost,
                chatQueryContext.getMapInfo().getDataSetElementMatches());
    }

    private void filter(ChatQueryContext chatQueryContext) {
        filterByDataSetId(chatQueryContext);
        filterByDetectWordLenLessThanOne(chatQueryContext);
        switch (chatQueryContext.getQueryDataType()) {
            case TAG:
                filterByQueryDataType(chatQueryContext, element -> !(element.getIsTag() > 0));
                break;
            case METRIC:
                filterByQueryDataType(chatQueryContext, element -> !SchemaElementType.METRIC.equals(element.getType()));
                break;
            case DIMENSION:
                filterByQueryDataType(chatQueryContext, element -> {
                    boolean isDimensionOrValue = SchemaElementType.DIMENSION.equals(element.getType())
                            || SchemaElementType.VALUE.equals(element.getType());
                    return !isDimensionOrValue;
                });
                break;
            case ALL:
            default:
                break;
        }
    }

    private static void filterByDataSetId(ChatQueryContext chatQueryContext) {
        Set<Long> dataSetIds = chatQueryContext.getDataSetIds();
        if (CollectionUtils.isEmpty(dataSetIds)) {
            return;
        }
        Set<Long> dataSetIdInMapInfo = new HashSet<>(chatQueryContext.getMapInfo().getDataSetElementMatches().keySet());
        for (Long dataSetId : dataSetIdInMapInfo) {
            if (!dataSetIds.contains(dataSetId)) {
                chatQueryContext.getMapInfo().getDataSetElementMatches().remove(dataSetId);
            }
        }
    }

    private static void filterByDetectWordLenLessThanOne(ChatQueryContext chatQueryContext) {
        Map<Long, List<SchemaElementMatch>> dataSetElementMatches =
                chatQueryContext.getMapInfo().getDataSetElementMatches();
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : dataSetElementMatches.entrySet()) {
            List<SchemaElementMatch> value = entry.getValue();
            if (!CollectionUtils.isEmpty(value)) {
                value.removeIf(schemaElementMatch ->
                        StringUtils.length(schemaElementMatch.getDetectWord()) <= 1);
            }
        }
    }

    private static void filterByQueryDataType(ChatQueryContext chatQueryContext,
                                              Predicate<SchemaElement> needRemovePredicate) {
        chatQueryContext.getMapInfo().getDataSetElementMatches().values().stream().forEach(
                schemaElementMatches -> schemaElementMatches.removeIf(
                        schemaElementMatch -> {
                            SchemaElement element = schemaElementMatch.getElement();
                            SchemaElementType type = element.getType();
                            if (SchemaElementType.ENTITY.equals(type) || SchemaElementType.DATASET.equals(type)
                                    || SchemaElementType.ID.equals(type)) {
                                return false;
                            }
                            return needRemovePredicate.test(element);
                        }
                ));
    }

    public abstract void doMap(ChatQueryContext chatQueryContext);

    public void addToSchemaMap(SchemaMapInfo schemaMap, Long dataSetId, SchemaElementMatch newElementMatch) {
        Map<Long, List<SchemaElementMatch>> dataSetElementMatches = schemaMap.getDataSetElementMatches();
        List<SchemaElementMatch> schemaElementMatches = dataSetElementMatches.putIfAbsent(dataSetId, new ArrayList<>());
        if (schemaElementMatches == null) {
            schemaElementMatches = dataSetElementMatches.get(dataSetId);
        }
        //remove duplication
        AtomicBoolean needAddNew = new AtomicBoolean(true);
        schemaElementMatches.removeIf(
                existElementMatch -> {
                    if (isEquals(existElementMatch, newElementMatch)) {
                        if (newElementMatch.getSimilarity() > existElementMatch.getSimilarity()) {
                            return true;
                        } else {
                            needAddNew.set(false);
                        }
                    }
                    return false;
                }
        );
        if (needAddNew.get()) {
            schemaElementMatches.add(newElementMatch);
        }
    }

    private static boolean isEquals(SchemaElementMatch existElementMatch, SchemaElementMatch newElementMatch) {
        SchemaElement existElement = existElementMatch.getElement();
        SchemaElement newElement = newElementMatch.getElement();
        if (!existElement.equals(newElement)) {
            return false;
        }
        if (SchemaElementType.VALUE.equals(newElement.getType())) {
            return existElementMatch.getWord().equalsIgnoreCase(newElementMatch.getWord());
        }
        return true;
    }

    public SchemaElement getSchemaElement(Long dataSetId, SchemaElementType elementType, Long elementID,
                                          SemanticSchema semanticSchema) {
        SchemaElement element = new SchemaElement();
        DataSetSchema dataSetSchema = semanticSchema.getDataSetSchemaMap().get(dataSetId);
        if (Objects.isNull(dataSetSchema)) {
            return null;
        }
        SchemaElement elementDb = dataSetSchema.getElement(elementType, elementID);
        if (Objects.isNull(elementDb)) {
            return null;
        }
        BeanUtils.copyProperties(elementDb, element);
        element.setAlias(getAlias(elementDb));
        return element;
    }

    public List<String> getAlias(SchemaElement element) {
        if (!SchemaElementType.VALUE.equals(element.getType())) {
            return element.getAlias();
        }
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(element.getAlias()) && StringUtils.isNotEmpty(
                element.getName())) {
            return element.getAlias().stream()
                    .filter(aliasItem -> aliasItem.contains(element.getName()))
                    .collect(Collectors.toList());
        }
        return element.getAlias();
    }
}
