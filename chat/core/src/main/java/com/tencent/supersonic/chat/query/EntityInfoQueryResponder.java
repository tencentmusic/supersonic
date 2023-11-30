package com.tencent.supersonic.chat.query;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.query.llm.analytics.MetricAnalyzeQuery;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.util.ContextUtils;

/**
 * EntityInfoQueryResponder fills core attributes of an entity so that
 * users get to know which entity is returned.
 */
public class EntityInfoQueryResponder implements QueryResponder {

    @Override
    public void fillInfo(QueryResult queryResult, SemanticParseInfo semanticParseInfo, ExecuteQueryReq queryReq) {
        if (semanticParseInfo == null) {
            return;
        }
        String queryMode = semanticParseInfo.getQueryMode();
        if (QueryManager.containsPluginQuery(queryMode)
                || MetricAnalyzeQuery.QUERY_MODE.equalsIgnoreCase(queryMode)) {
            return;
        }
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        User user = queryReq.getUser();
        EntityInfo entityInfo = semanticService.getEntityInfo(semanticParseInfo, user);
        queryResult.setEntityInfo(entityInfo);

    }

}
