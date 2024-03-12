package com.tencent.supersonic.headless.core.chat.parser;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.core.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.core.pojo.ChatContext;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * QueryTypeParser resolves query type as either METRIC or TAG, or ID.
 */
@Slf4j
public class QueryTypeParser implements SemanticParser {

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {

        List<SemanticQuery> candidateQueries = queryContext.getCandidateQueries();
        User user = queryContext.getUser();

        for (SemanticQuery semanticQuery : candidateQueries) {
            // 1.init S2SQL
            semanticQuery.initS2Sql(queryContext.getSemanticSchema(), user);
            // 2.set queryType
            SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
            parseInfo.setQueryType(queryContext.getQueryType(parseInfo.getDataSetId()));
        }
    }

}
