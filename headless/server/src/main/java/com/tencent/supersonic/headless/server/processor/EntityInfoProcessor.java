package com.tencent.supersonic.headless.server.processor;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.EntityInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.core.chat.query.QueryManager;
import com.tencent.supersonic.headless.core.pojo.ChatContext;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import com.tencent.supersonic.headless.server.service.impl.SemanticService;
import org.springframework.util.CollectionUtils;
import java.util.List;

/**
 * EntityInfoProcessor fills core attributes of an entity so that
 * users get to know which entity is parsed out.
 */
public class EntityInfoProcessor implements ResultProcessor {

    @Override
    public void process(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext) {
        List<SemanticParseInfo> selectedParses = parseResp.getSelectedParses();
        if (CollectionUtils.isEmpty(selectedParses)) {
            return;
        }
        selectedParses.forEach(parseInfo -> {
            String queryMode = parseInfo.getQueryMode();
            if (QueryManager.containsRuleQuery(queryMode)) {
                return;
            }
            //1. set entity info
            DataSetSchema dataSetSchema =
                    queryContext.getSemanticSchema().getDataSetSchemaMap().get(parseInfo.getDataSetId());
            SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
            EntityInfo entityInfo = semanticService.getEntityInfo(parseInfo, dataSetSchema, queryContext.getUser());
            if (QueryManager.isTagQuery(queryMode)
                    || QueryManager.isMetricQuery(queryMode)) {
                parseInfo.setEntityInfo(entityInfo);
            }
        });
    }
}
