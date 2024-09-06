package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/** * A mapper that map the description of the term. */
@Slf4j
public class TermDescMapper extends BaseMapper {

    @Override
    public void doMap(ChatQueryContext chatQueryContext) {
        List<SchemaElement> termDescriptionToMap =
                chatQueryContext.getMapInfo().getTermDescriptionToMap();
        if (CollectionUtils.isEmpty(termDescriptionToMap)) {
            return;
        }
        if (StringUtils.isBlank(chatQueryContext.getOriQueryText())) {
            chatQueryContext.setOriQueryText(chatQueryContext.getQueryText());
        }
        for (SchemaElement schemaElement : termDescriptionToMap) {
            if (schemaElement.isDescriptionMapped()) {
                continue;
            }
            if (chatQueryContext.getQueryText().equals(schemaElement.getDescription())) {
                schemaElement.setDescriptionMapped(true);
                continue;
            }
            chatQueryContext.setQueryText(schemaElement.getDescription());
            break;
        }
        if (CollectionUtils.isEmpty(chatQueryContext.getMapInfo().getTermDescriptionToMap())) {
            chatQueryContext.setQueryText(chatQueryContext.getOriQueryText());
        }
    }
}
