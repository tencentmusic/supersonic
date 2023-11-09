package com.tencent.supersonic.chat.mapper;

import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.util.ContextUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

/**
 * base Mapper
 */
@Slf4j
public abstract class BaseMapper implements SchemaMapper {

    @Override
    public void map(QueryContext queryContext) {

        String simpleName = this.getClass().getSimpleName();
        long startTime = System.currentTimeMillis();
        log.info("before {},mapInfo:{}", simpleName, queryContext.getMapInfo().getModelElementMatches());

        try {
            work(queryContext);
        } catch (Exception e) {
            log.error("work error", e);
        }

        long cost = System.currentTimeMillis() - startTime;
        log.info("after {},cost:{},mapInfo:{}", simpleName, cost, queryContext.getMapInfo().getModelElementMatches());
    }

    public abstract void work(QueryContext queryContext);


    public void addToSchemaMap(SchemaMapInfo schemaMap, Long modelId, SchemaElementMatch schemaElementMatch) {
        Map<Long, List<SchemaElementMatch>> modelElementMatches = schemaMap.getModelElementMatches();
        List<SchemaElementMatch> schemaElementMatches = modelElementMatches.putIfAbsent(modelId, new ArrayList<>());
        if (schemaElementMatches == null) {
            schemaElementMatches = modelElementMatches.get(modelId);
        }
        schemaElementMatches.add(schemaElementMatch);
    }

    public SchemaElement getSchemaElement(Long modelId, SchemaElementType elementType, Long elementID) {
        SchemaElement element = new SchemaElement();
        SemanticService schemaService = ContextUtils.getBean(SemanticService.class);
        ModelSchema modelSchema = schemaService.getModelSchema(modelId);
        if (Objects.isNull(modelSchema)) {
            return null;
        }
        SchemaElement elementDb = modelSchema.getElement(elementType, elementID);
        if (Objects.isNull(elementDb)) {
            log.info("element is null, elementType:{},elementID:{}", elementType, elementID);
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
