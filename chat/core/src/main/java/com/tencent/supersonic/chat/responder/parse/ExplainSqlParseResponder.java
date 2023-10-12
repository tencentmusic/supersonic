package com.tencent.supersonic.chat.responder.parse;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.semantic.api.model.response.ExplainResp;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Objects;

public class ExplainSqlParseResponder implements ParseResponder {

    @Override
    public void fillResponse(ParseResp parseResp, QueryContext queryContext) {
        QueryReq queryReq = queryContext.getRequest();
        addExplainSql(queryReq, parseResp.getSelectedParses());
        addExplainSql(queryReq, parseResp.getCandidateParses());
    }

    private void addExplainSql(QueryReq queryReq, List<SemanticParseInfo> semanticParseInfos) {
        if (CollectionUtils.isEmpty(semanticParseInfos)) {
            return;
        }
        semanticParseInfos.forEach(parseInfo -> {
            addExplainSql(queryReq, parseInfo);
        });
    }

    private void addExplainSql(QueryReq queryReq, SemanticParseInfo parseInfo) {
        SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());
        if (Objects.isNull(semanticQuery)) {
            return;
        }
        semanticQuery.setParseInfo(parseInfo);
        ExplainResp explain = semanticQuery.explain(queryReq.getUser());
        if (Objects.isNull(explain)) {
            return;
        }
        parseInfo.getSqlInfo().setQuerySql(explain.getSql());
    }

}
