package com.tencent.supersonic.chat.responder.parse;

import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.util.ContextUtils;
import org.springframework.util.CollectionUtils;
import java.util.List;

public class EntityInfoParseResponder implements ParseResponder {

    @Override
    public void fillResponse(ParseResp parseResp, QueryContext queryContext) {
        List<SemanticParseInfo> selectedParses = parseResp.getSelectedParses();
        if (CollectionUtils.isEmpty(selectedParses)) {
            return;
        }
        QueryReq queryReq = queryContext.getRequest();
        selectedParses.forEach(parseInfo -> {
            String queryMode = parseInfo.getQueryMode();
            if (QueryManager.isEntityQuery(queryMode)) {
                EntityInfo entityInfo = ContextUtils.getBean(SemanticService.class)
                        .getEntityInfo(parseInfo, queryReq.getUser());
                parseInfo.setEntityInfo(entityInfo);
            }
        });
    }

}