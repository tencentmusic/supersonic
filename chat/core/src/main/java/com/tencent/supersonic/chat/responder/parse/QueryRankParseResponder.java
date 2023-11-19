package com.tencent.supersonic.chat.responder.parse;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.query.QueryRanker;
import com.tencent.supersonic.common.util.ContextUtils;

import java.util.List;

/**
 * Rank queries by score.
 */
public class QueryRankParseResponder implements ParseResponder {

    @Override
    public void fillResponse(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext) {
        List<SemanticQuery> candidateQueries = queryContext.getCandidateQueries();
        QueryRanker queryRanker = ContextUtils.getBean(QueryRanker.class);
        candidateQueries = queryRanker.rank(candidateQueries);
        queryContext.setCandidateQueries(candidateQueries);
    }
}
