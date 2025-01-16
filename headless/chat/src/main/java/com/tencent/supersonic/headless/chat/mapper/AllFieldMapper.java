package com.tencent.supersonic.headless.chat.mapper;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.enums.MapModeEnum;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class AllFieldMapper extends BaseMapper {

    @Override
    public boolean accept(ChatQueryContext chatQueryContext) {
        return MapModeEnum.ALL.equals(chatQueryContext.getRequest().getMapModeEnum());
    }

    @Override
    public void doMap(ChatQueryContext chatQueryContext) {
        Map<Long, DataSetSchema> schemaMap =
                chatQueryContext.getSemanticSchema().getDataSetSchemaMap();
        for (Map.Entry<Long, DataSetSchema> entry : schemaMap.entrySet()) {
            List<SchemaElement> schemaElements = Lists.newArrayList();
            schemaElements.addAll(entry.getValue().getDimensions());
            schemaElements.addAll(entry.getValue().getMetrics());

            List<SchemaElementMatch> allMatches = Lists.newArrayList();
            for (SchemaElement schemaElement : schemaElements) {
                allMatches.add(SchemaElementMatch.builder().word(schemaElement.getName())
                        .element(schemaElement).detectWord(schemaElement.getName()).similarity(0.1)
                        .build());
            }
            chatQueryContext.getMapInfo().setMatchedElements(entry.getKey(), allMatches);
        }
    }

}
