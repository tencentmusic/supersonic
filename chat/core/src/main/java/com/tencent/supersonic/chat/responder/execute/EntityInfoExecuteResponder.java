package com.tencent.supersonic.chat.responder.execute;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.util.ContextUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;

public class EntityInfoExecuteResponder implements ExecuteResponder {

    @Override
    public void fillResponse(QueryResult queryResult, SemanticParseInfo semanticParseInfo, ExecuteQueryReq queryReq) {
        if (semanticParseInfo == null || semanticParseInfo.getModelId() <= 0L) {
            return;
        }
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        User user = queryReq.getUser();
        queryResult.setEntityInfo(semanticService.getEntityInfo(semanticParseInfo, user));

        EntityInfo entityInfo = semanticService.getEntityInfo(semanticParseInfo.getModelId());
        if (Objects.isNull(entityInfo) || Objects.isNull(entityInfo.getModelInfo())
                || Objects.isNull(entityInfo.getModelInfo().getPrimaryEntityName())) {
            return;
        }
        String primaryEntityBizName = entityInfo.getModelInfo().getPrimaryEntityBizName();
        if (CollectionUtils.isEmpty(queryResult.getQueryColumns())) {
            return;
        }
        boolean existPrimaryEntityName = queryResult.getQueryColumns().stream()
                .anyMatch(queryColumn -> primaryEntityBizName.equals(queryColumn.getNameEn()));

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