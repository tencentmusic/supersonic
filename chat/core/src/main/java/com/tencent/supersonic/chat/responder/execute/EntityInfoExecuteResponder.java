package com.tencent.supersonic.chat.responder.execute;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.llm.interpret.MetricInterpretQuery;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.util.ContextUtils;

public class EntityInfoExecuteResponder implements ExecuteResponder {

    @Override
    public void fillResponse(QueryResult queryResult, SemanticParseInfo semanticParseInfo, ExecuteQueryReq queryReq) {
        if (semanticParseInfo == null || semanticParseInfo.getModelId() <= 0L) {
            return;
        }
        String queryMode = semanticParseInfo.getQueryMode();
        if (QueryManager.containsPluginQuery(queryMode)
                || MetricInterpretQuery.QUERY_MODE.equalsIgnoreCase(queryMode)) {
            return;
        }
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        User user = queryReq.getUser();
        EntityInfo entityInfo = semanticService.getEntityInfo(semanticParseInfo, user);
        queryResult.setEntityInfo(entityInfo);

    }

}
