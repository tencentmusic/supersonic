package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.common.util.DeepCopyUtil;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.utils.ComponentFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * A mapper that map the description of the term.
 */
@Slf4j
public class TermDescMapper extends BaseMapper {

    @Override
    public boolean accept(ChatQueryContext chatQueryContext) {
        return !(CollectionUtils.isEmpty(chatQueryContext.getMapInfo().getTermDescriptionToMap())
                || chatQueryContext.getRequest().isDescriptionMapped());
    }

    @Override
    public void doMap(ChatQueryContext chatQueryContext) {
        List<SchemaElement> termElements = chatQueryContext.getMapInfo().getTermDescriptionToMap();
        for (SchemaElement schemaElement : termElements) {
            ChatQueryContext queryCtx =
                    buildQueryContext(chatQueryContext, schemaElement.getDescription());
            ComponentFactory.getSchemaMappers().forEach(mapper -> mapper.map(queryCtx));
            chatQueryContext.getMapInfo().addMatchedElements(queryCtx.getMapInfo());
        }
    }

    private static ChatQueryContext buildQueryContext(ChatQueryContext chatQueryContext,
            String queryText) {
        ChatQueryContext queryContext = DeepCopyUtil.deepCopy(chatQueryContext);
        queryContext.getRequest().setQueryText(queryText);
        queryContext.setMapInfo(new SchemaMapInfo());
        queryContext.setSemanticSchema(chatQueryContext.getSemanticSchema());
        queryContext.setModelIdToDataSetIds(chatQueryContext.getModelIdToDataSetIds());
        queryContext.setChatWorkflowState(chatQueryContext.getChatWorkflowState());
        queryContext.getRequest().setDescriptionMapped(true);
        return queryContext;
    }
}
