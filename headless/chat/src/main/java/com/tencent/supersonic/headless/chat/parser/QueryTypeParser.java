package com.tencent.supersonic.headless.chat.parser;

import com.tencent.supersonic.common.jsqlparser.SqlSelectFunctionHelper;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/** QueryTypeParser resolves query type as either AGGREGATE or DETAIL */
@Slf4j
public class QueryTypeParser implements SemanticParser {

    @Override
    public void parse(ChatQueryContext chatQueryContext) {

        List<SemanticQuery> candidateQueries = chatQueryContext.getCandidateQueries();
        User user = chatQueryContext.getRequest().getUser();

        for (SemanticQuery semanticQuery : candidateQueries) {
            // 1.init S2SQL
            Long dataSetId = semanticQuery.getParseInfo().getDataSetId();
            DataSetSchema dataSetSchema =
                    chatQueryContext.getSemanticSchema().getDataSetSchemaMap().get(dataSetId);
            semanticQuery.initS2Sql(dataSetSchema, user);
            // 2.set queryType
            QueryType queryType = getQueryType(semanticQuery);
            semanticQuery.getParseInfo().setQueryType(queryType);
        }
    }

    private QueryType getQueryType(SemanticQuery semanticQuery) {
        SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
        SqlInfo sqlInfo = parseInfo.getSqlInfo();
        if (Objects.isNull(sqlInfo) || StringUtils.isBlank(sqlInfo.getParsedS2SQL())) {
            return QueryType.DETAIL;
        }

        if (SqlSelectFunctionHelper.hasAggregateFunction(sqlInfo.getParsedS2SQL())) {
            return QueryType.AGGREGATE;
        }

        return QueryType.DETAIL;
    }

}
