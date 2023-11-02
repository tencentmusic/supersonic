package com.tencent.supersonic.chat.mapper;

import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * base Mapper
 */
@Slf4j
public abstract class BaseMapper implements SchemaMapper {

    @Override
    public void map(QueryContext queryContext) {

        String simpleName = this.getClass().getSimpleName();

        log.debug("before {},mapInfo:{}", simpleName, queryContext.getMapInfo());

        work(queryContext);

        log.debug("after {},mapInfo:{}", simpleName, queryContext.getMapInfo());
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
}
