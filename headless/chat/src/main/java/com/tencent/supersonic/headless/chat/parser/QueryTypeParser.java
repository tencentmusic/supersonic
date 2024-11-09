package com.tencent.supersonic.headless.chat.parser;

import com.tencent.supersonic.common.jsqlparser.SqlSelectFunctionHelper;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import lombok.extern.slf4j.Slf4j;

/** QueryTypeParser resolves query type as either AGGREGATE or DETAIL */
@Slf4j
public class QueryTypeParser implements SemanticParser {

    @Override
    public void parse(ChatQueryContext chatQueryContext) {
        chatQueryContext.getCandidateQueries().forEach(query -> {
            SemanticParseInfo parseInfo = query.getParseInfo();
            String s2SQL = parseInfo.getSqlInfo().getParsedS2SQL();
            QueryType queryType = QueryType.DETAIL;

            if (SqlSelectFunctionHelper.hasAggregateFunction(s2SQL)) {
                queryType = QueryType.AGGREGATE;
            }

            parseInfo.setQueryType(queryType);
        });
    }

}
