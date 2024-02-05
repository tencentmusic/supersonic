package com.tencent.supersonic.chat.core.parser;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.response.SqlInfo;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.core.query.SemanticQuery;
import com.tencent.supersonic.chat.core.query.llm.s2sql.LLMSqlQuery;
import com.tencent.supersonic.chat.core.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.jsqlparser.SqlSelectHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
            QueryType queryType = getQueryType(queryContext, semanticQuery);
            semanticQuery.getParseInfo().setQueryType(queryType);
        }
    }

    private QueryType getQueryType(QueryContext queryContext, SemanticQuery semanticQuery) {
        SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
        SqlInfo sqlInfo = parseInfo.getSqlInfo();
        if (Objects.isNull(sqlInfo) || StringUtils.isBlank(sqlInfo.getS2SQL())) {
            return QueryType.ID;
        }
        //1. entity queryType
        Long viewId = parseInfo.getViewId();
        SemanticSchema semanticSchema = queryContext.getSemanticSchema();
        if (semanticQuery instanceof RuleSemanticQuery || semanticQuery instanceof LLMSqlQuery) {
            //If all the fields in the SELECT statement are of tag type.
            List<String> whereFields = SqlSelectHelper.getWhereFields(sqlInfo.getS2SQL())
                    .stream().filter(field -> !TimeDimensionEnum.containsTimeDimension(field))
                    .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(whereFields)) {
                Set<String> ids = semanticSchema.getEntities(viewId).stream().map(SchemaElement::getName)
                        .collect(Collectors.toSet());
                if (CollectionUtils.isNotEmpty(ids) && ids.stream().anyMatch(whereFields::contains)) {
                    return QueryType.ID;
                }
                Set<String> tags = semanticSchema.getTags(viewId).stream().map(SchemaElement::getName)
                        .collect(Collectors.toSet());
                if (CollectionUtils.isNotEmpty(tags) && tags.containsAll(whereFields)) {
                    return QueryType.TAG;
                }
            }
        }
        //2. metric queryType
        List<String> selectFields = SqlSelectHelper.getSelectFields(sqlInfo.getS2SQL());
        List<SchemaElement> metrics = semanticSchema.getMetrics(viewId);
        if (CollectionUtils.isNotEmpty(metrics)) {
            Set<String> metricNameSet = metrics.stream().map(SchemaElement::getName).collect(Collectors.toSet());
            boolean containMetric = selectFields.stream().anyMatch(metricNameSet::contains);
            if (containMetric) {
                return QueryType.METRIC;
            }
        }
        return QueryType.ID;
    }

}
