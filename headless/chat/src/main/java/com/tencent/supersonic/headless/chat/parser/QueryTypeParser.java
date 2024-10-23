package com.tencent.supersonic.headless.chat.parser;

import com.tencent.supersonic.common.jsqlparser.SqlSelectFunctionHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMSqlQuery;
import com.tencent.supersonic.headless.chat.query.rule.RuleSemanticQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** QueryTypeParser resolves query type as either METRIC or TAG, or ID. */
@Slf4j
public class QueryTypeParser implements SemanticParser {

    @Override
    public void parse(ChatQueryContext chatQueryContext) {

        List<SemanticQuery> candidateQueries = chatQueryContext.getCandidateQueries();
        User user = chatQueryContext.getUser();

        for (SemanticQuery semanticQuery : candidateQueries) {
            // 1.init S2SQL
            Long dataSetId = semanticQuery.getParseInfo().getDataSetId();
            DataSetSchema dataSetSchema =
                    chatQueryContext.getSemanticSchema().getDataSetSchemaMap().get(dataSetId);
            semanticQuery.initS2Sql(dataSetSchema, user);
            // 2.set queryType
            QueryType queryType = getQueryType(chatQueryContext, semanticQuery);
            semanticQuery.getParseInfo().setQueryType(queryType);
        }
    }

    private QueryType getQueryType(ChatQueryContext chatQueryContext, SemanticQuery semanticQuery) {
        SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
        SqlInfo sqlInfo = parseInfo.getSqlInfo();
        if (Objects.isNull(sqlInfo) || StringUtils.isBlank(sqlInfo.getParsedS2SQL())) {
            return QueryType.DETAIL;
        }

        // 1. entity queryType
        Long dataSetId = parseInfo.getDataSetId();
        SemanticSchema semanticSchema = chatQueryContext.getSemanticSchema();
        if (semanticQuery instanceof RuleSemanticQuery || semanticQuery instanceof LLMSqlQuery) {
            List<String> whereFields = SqlSelectHelper.getWhereFields(sqlInfo.getParsedS2SQL());
            List<String> whereFilterByTimeFields = filterByTimeFields(whereFields);
            if (CollectionUtils.isNotEmpty(whereFilterByTimeFields)) {
                Set<String> ids = semanticSchema.getEntities(dataSetId).stream()
                        .map(SchemaElement::getName).collect(Collectors.toSet());
                if (CollectionUtils.isNotEmpty(ids)
                        && ids.stream().anyMatch(whereFilterByTimeFields::contains)) {
                    return QueryType.ID;
                }
            }
        }

        // 2. metric queryType
        if (selectContainsMetric(sqlInfo, dataSetId, semanticSchema)
                || SqlSelectFunctionHelper.hasAggregateFunction(sqlInfo.getParsedS2SQL())) {
            return QueryType.AGGREGATE;
        }

        return QueryType.DETAIL;
    }

    private static List<String> filterByTimeFields(List<String> whereFields) {
        List<String> selectAndWhereFilterByTimeFields = whereFields.stream()
                .filter(field -> !TimeDimensionEnum.containsTimeDimension(field))
                .collect(Collectors.toList());
        return selectAndWhereFilterByTimeFields;
    }

    private static boolean selectContainsMetric(SqlInfo sqlInfo, Long dataSetId,
            SemanticSchema semanticSchema) {
        List<String> selectFields = SqlSelectHelper.getSelectFields(sqlInfo.getParsedS2SQL());
        List<SchemaElement> metrics = semanticSchema.getMetrics(dataSetId);
        if (CollectionUtils.isNotEmpty(metrics)) {
            Set<String> metricNameSet =
                    metrics.stream().map(SchemaElement::getName).collect(Collectors.toSet());
            return selectFields.stream().anyMatch(metricNameSet::contains);
        }
        return false;
    }
}
