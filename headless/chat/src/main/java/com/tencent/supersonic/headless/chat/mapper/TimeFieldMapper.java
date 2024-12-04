package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.common.pojo.enums.Text2SQLType;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class TimeFieldMapper extends BaseMapper {

    @Override
    public void doMap(ChatQueryContext chatQueryContext) {
        if (chatQueryContext.getRequest().getText2SQLType().equals(Text2SQLType.ONLY_RULE)) {
            return;
        }

        Map<Long, DataSetSchema> schemaMap =
                chatQueryContext.getSemanticSchema().getDataSetSchemaMap();
        for (Map.Entry<Long, DataSetSchema> entry : schemaMap.entrySet()) {
            List<SchemaElement> timeDims = entry.getValue().getDimensions().stream()
                    .filter(dim -> dim.getTimeFormat() != null).collect(Collectors.toList());
            for (SchemaElement schemaElement : timeDims) {
                chatQueryContext.getMapInfo().getMatchedElements(entry.getKey())
                        .add(SchemaElementMatch.builder().word(schemaElement.getName())
                                .element(schemaElement).detectWord(schemaElement.getName())
                                .similarity(1.0).build());
            }
        }
    }

}
