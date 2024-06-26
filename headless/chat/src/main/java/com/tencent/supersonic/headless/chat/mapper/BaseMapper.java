package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.chat.QueryContext;
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
    public void map(QueryContext queryContext) {

        String simpleName = this.getClass().getSimpleName();
        long startTime = System.currentTimeMillis();
        log.debug("before {},mapInfo:{}", simpleName,
                queryContext.getMapInfo().getDataSetElementMatches());

        try {
            doMap(queryContext);
            filter(queryContext);
        } catch (Exception e) {
            log.error("work error", e);
        }

        long cost = System.currentTimeMillis() - startTime;
        log.debug("after {},cost:{},mapInfo:{}", simpleName, cost,
                queryContext.getMapInfo().getDataSetElementMatches());
    }

    private void filter(QueryContext queryContext) {
        filterByDataSetId(queryContext);
        filterByDetectWordLenLessThanOne(queryContext);
        switch (queryContext.getQueryDataType()) {
            case TAG:
                filterByQueryDataType(queryContext, element -> !(element.getIsTag() > 0));
                break;
            case METRIC:
                filterByQueryDataType(queryContext, element -> !SchemaElementType.METRIC.equals(element.getType()));
                break;
            case DIMENSION:
                filterByQueryDataType(queryContext, element -> {
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

    private static void filterByDataSetId(QueryContext queryContext) {
        Set<Long> dataSetIds = queryContext.getDataSetIds();
        if (CollectionUtils.isEmpty(dataSetIds)) {
            return;
        }
        Set<Long> dataSetIdInMapInfo = new HashSet<>(queryContext.getMapInfo().getDataSetElementMatches().keySet());
        for (Long dataSetId : dataSetIdInMapInfo) {
            if (!dataSetIds.contains(dataSetId)) {
                queryContext.getMapInfo().getDataSetElementMatches().remove(dataSetId);
            }
        }
    }

    private static void filterByDetectWordLenLessThanOne(QueryContext queryContext) {
        Map<Long, List<SchemaElementMatch>> dataSetElementMatches =
                queryContext.getMapInfo().getDataSetElementMatches();
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : dataSetElementMatches.entrySet()) {
            List<SchemaElementMatch> value = entry.getValue();
            if (!CollectionUtils.isEmpty(value)) {
                value.removeIf(schemaElementMatch ->
                        StringUtils.length(schemaElementMatch.getDetectWord()) <= 1);
            }
        }
    }

    private static void filterByQueryDataType(QueryContext queryContext, Predicate<SchemaElement> needRemovePredicate) {
        queryContext.getMapInfo().getDataSetElementMatches().values().stream().forEach(
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

    public abstract void doMap(QueryContext queryContext);

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
