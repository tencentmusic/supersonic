package com.tencent.supersonic.chat.responder.execute;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.util.ContextUtils;

public class EntityInfoExecuteResponder implements ExecuteResponder {

    @Override
    public void fillResponse(QueryResult queryResult, SemanticParseInfo semanticParseInfo, ExecuteQueryReq queryReq) {
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        EntityInfo entityInfo = semanticService.getEntityInfo(semanticParseInfo, queryReq.getUser());
        queryResult.setEntityInfo(entityInfo);
    }

}