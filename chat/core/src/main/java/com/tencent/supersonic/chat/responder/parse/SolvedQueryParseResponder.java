package com.tencent.supersonic.chat.responder.parse;

import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.SolvedQueryRecallResp;
import com.tencent.supersonic.chat.utils.SolvedQueryManager;
import com.tencent.supersonic.common.util.ContextUtils;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
public class SolvedQueryParseResponder implements ParseResponder {

    @Override
    public void fillResponse(ParseResp parseResp, QueryContext queryContext) {
        SolvedQueryManager solvedQueryManager = ContextUtils.getBean(SolvedQueryManager.class);
        List<SolvedQueryRecallResp> solvedQueryRecallResps =
                solvedQueryManager.recallSolvedQuery(queryContext.getRequest().getQueryText(),
                        queryContext.getRequest().getAgentId());
        parseResp.setSimilarSolvedQuery(solvedQueryRecallResps);
    }

}