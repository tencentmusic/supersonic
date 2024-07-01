package com.tencent.supersonic.chat.server.processor.parse;

import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.EntityInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.chat.query.QueryManager;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * EntityInfoProcessor fills core attributes of an entity so that
 * users get to know which entity is parsed out.
 */
public class EntityInfoProcessor implements ParseResultProcessor {

    @Override
    public void process(ChatParseContext chatParseContext, ParseResp parseResp) {
        List<SemanticParseInfo> selectedParses = parseResp.getSelectedParses();
        if (CollectionUtils.isEmpty(selectedParses)) {
            return;
        }
        selectedParses.forEach(parseInfo -> {
            String queryMode = parseInfo.getQueryMode();
            if (QueryManager.containsRuleQuery(queryMode) || "PLAIN".equals(queryMode)) {
                return;
            }

            //1. set entity info
            SemanticLayerService semanticService = ContextUtils.getBean(SemanticLayerService.class);
            DataSetSchema dataSetSchema = semanticService.getDataSetSchema(parseInfo.getDataSetId());
            EntityInfo entityInfo = semanticService.getEntityInfo(parseInfo, dataSetSchema, chatParseContext.getUser());
            if (QueryManager.isTagQuery(queryMode)
                    || QueryManager.isMetricQuery(queryMode)) {
                parseInfo.setEntityInfo(entityInfo);
            }
        });
    }
}
