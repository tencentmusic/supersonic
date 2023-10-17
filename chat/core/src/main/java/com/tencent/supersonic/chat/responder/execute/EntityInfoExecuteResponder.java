package com.tencent.supersonic.chat.responder.execute;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.llm.s2ql.S2QLQuery;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.util.ContextUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class EntityInfoExecuteResponder implements ExecuteResponder {

    @Override
    public void fillResponse(QueryResult queryResult, SemanticParseInfo semanticParseInfo, ExecuteQueryReq queryReq) {
        if (semanticParseInfo == null || semanticParseInfo.getModelId() <= 0L) {
            return;
        }
        String queryMode = semanticParseInfo.getQueryMode();
        if (QueryManager.isPluginQuery(queryMode) && !S2QLQuery.QUERY_MODE.equals(queryMode)) {
            return;
        }
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        User user = queryReq.getUser();
        EntityInfo entityInfo = semanticService.getEntityInfo(semanticParseInfo, user);
        queryResult.setEntityInfo(entityInfo);

        String primaryEntityBizName = semanticService.getPrimaryEntityBizName(entityInfo);
        if (StringUtils.isEmpty(primaryEntityBizName)
                || CollectionUtils.isEmpty(queryResult.getQueryColumns())) {
            return;
        }
        boolean existPrimaryEntityName = queryResult.getQueryColumns().stream()
                .anyMatch(queryColumn -> primaryEntityBizName.equals(queryColumn.getNameEn()));

        semanticParseInfo.setNativeQuery(existPrimaryEntityName);

        if (!existPrimaryEntityName) {
            return;
        }
        List<Map<String, Object>> queryResults = queryResult.getQueryResults();
        List<String> entities = queryResults.stream()
                .map(entry -> entry.get(primaryEntityBizName))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(entities)) {
            return;
        }
    }

}
