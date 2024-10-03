package com.tencent.supersonic.headless.server.processor;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.EntityInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.query.QueryManager;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;

/**
 * EntityInfoProcessor fills core attributes of an entity so that users get to know which entity is
 * parsed out.
 */
public class EntityInfoProcessor implements ResultProcessor {

    @Override
    public void process(ParseResp parseResp, ChatQueryContext chatQueryContext) {
        parseResp.getSelectedParses().forEach(parseInfo -> {
            String queryMode = parseInfo.getQueryMode();
            if (!QueryManager.isDetailQuery(queryMode) && !QueryManager.isMetricQuery(queryMode)) {
                return;
            }

            SemanticLayerService semanticService = ContextUtils.getBean(SemanticLayerService.class);
            DataSetSchema dataSetSchema =
                    semanticService.getDataSetSchema(parseInfo.getDataSetId());
            EntityInfo entityInfo = semanticService.getEntityInfo(parseInfo, dataSetSchema,
                    chatQueryContext.getUser());
            parseInfo.setEntityInfo(entityInfo);
        });
    }
}
