package com.tencent.supersonic.chat.responder.parse;

import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.llm.interpret.MetricInterpretQuery;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.util.ContextUtils;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

public class EntityInfoParseResponder implements ParseResponder {

    @Override
    public void fillResponse(ParseResp parseResp, QueryContext queryContext,
            List<ChatParseDO> chatParseDOS) {
        List<SemanticParseInfo> selectedParses = parseResp.getSelectedParses();
        if (CollectionUtils.isEmpty(selectedParses)) {
            return;
        }
        QueryReq queryReq = queryContext.getRequest();
        selectedParses.forEach(parseInfo -> {
            String queryMode = parseInfo.getQueryMode();
            if (QueryManager.containsPluginQuery(queryMode) || MetricInterpretQuery.QUERY_MODE.equalsIgnoreCase(
                    queryMode)) {
                return;
            }
            //1. set entity info
            SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
            EntityInfo entityInfo = semanticService.getEntityInfo(parseInfo, queryReq.getUser());
            if (QueryManager.isEntityQuery(queryMode)
                    || QueryManager.isMetricQuery(queryMode)) {
                parseInfo.setEntityInfo(entityInfo);
            }
            //2. set native value
            String primaryEntityBizName = semanticService.getPrimaryEntityBizName(entityInfo);
            if (StringUtils.isNotEmpty(primaryEntityBizName)) {
                //if exist primaryEntityBizName in parseInfo's dimensions, set nativeQuery to true
                boolean existPrimaryEntityBizName = parseInfo.getDimensions().stream()
                        .anyMatch(schemaElement -> primaryEntityBizName.equalsIgnoreCase(schemaElement.getBizName()));
                parseInfo.setNativeQuery(existPrimaryEntityBizName);
            }
        });
    }
}
