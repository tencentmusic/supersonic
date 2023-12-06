package com.tencent.supersonic.chat.processor;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.llm.analytics.MetricAnalyzeQuery;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.util.ContextUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * EntityInfoProcessor fills core attributes of an entity so that
 * users get to know which entity is parsed out.
 */
public class EntityInfoProcessor implements ParseResultProcessor {

    @Override
    public void process(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext) {
        List<SemanticQuery> semanticQueries = queryContext.getCandidateQueries();
        if (CollectionUtils.isEmpty(semanticQueries)) {
            return;
        }
        List<SemanticParseInfo> selectedParses = semanticQueries.stream().map(SemanticQuery::getParseInfo)
                .collect(Collectors.toList());
        QueryReq queryReq = queryContext.getRequest();
        selectedParses.forEach(parseInfo -> {
            String queryMode = parseInfo.getQueryMode();
            if (QueryManager.containsPluginQuery(queryMode)
                    || MetricAnalyzeQuery.QUERY_MODE.equalsIgnoreCase(queryMode)) {
                return;
            }
            //1. set entity info
            SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
            EntityInfo entityInfo = semanticService.getEntityInfo(parseInfo, queryReq.getUser());
            if (QueryManager.isTagQuery(queryMode)
                    || QueryManager.isMetricQuery(queryMode)) {
                parseInfo.setEntityInfo(entityInfo);
            }
        });
    }
}
