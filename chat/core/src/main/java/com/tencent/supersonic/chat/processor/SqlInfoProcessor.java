package com.tencent.supersonic.chat.processor;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.query.QueryManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * SqlInfoProcessor adds S2SQL to the parsing results so that
 * technical users could verify SQL by themselves.
 **/
public class SqlInfoProcessor implements ParseResultProcessor {

    @Override
    public void process(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext) {
        QueryReq queryReq = queryContext.getRequest();
        List<SemanticQuery> semanticQueries = queryContext.getCandidateQueries();
        if (CollectionUtils.isEmpty(semanticQueries)) {
            return;
        }
        List<SemanticParseInfo> selectedParses = semanticQueries.stream().map(SemanticQuery::getParseInfo)
                .collect(Collectors.toList());
        long startTime = System.currentTimeMillis();
        addSqlInfo(queryReq, selectedParses);
        parseResp.getParseTimeCost().setSqlTime(System.currentTimeMillis() - startTime);
    }

    private void addSqlInfo(QueryReq queryReq, List<SemanticParseInfo> semanticParseInfos) {
        if (CollectionUtils.isEmpty(semanticParseInfos)) {
            return;
        }
        semanticParseInfos.forEach(parseInfo -> {
            addSqlInfo(queryReq, parseInfo);
        });
    }

    private void addSqlInfo(QueryReq queryReq, SemanticParseInfo parseInfo) {
        SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());
        if (Objects.isNull(semanticQuery)) {
            return;
        }
        semanticQuery.setParseInfo(parseInfo);
        String explainSql = semanticQuery.explain(queryReq.getUser());
        if (StringUtils.isBlank(explainSql)) {
            return;
        }
        parseInfo.getSqlInfo().setQuerySQL(explainSql);
    }

}
