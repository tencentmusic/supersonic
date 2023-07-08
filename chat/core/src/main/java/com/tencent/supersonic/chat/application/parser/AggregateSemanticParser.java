package com.tencent.supersonic.chat.application.parser;

import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.application.query.EntityListFilter;
import com.tencent.supersonic.chat.application.query.MetricGroupBy;
import com.tencent.supersonic.chat.application.query.MetricOrderBy;
import com.tencent.supersonic.common.enums.AggregateTypeEnum;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AggregateSemanticParser implements SemanticParser {

    public static final Integer TOPN_LIMIT = 1000;

    private static Map<AggregateTypeEnum, Pattern> aggregateRegexMap = new HashMap<>();

    static {
        aggregateRegexMap.put(AggregateTypeEnum.MAX, Pattern.compile("(?i)(最大值|最大|max|峰值|最高|最多)"));
        aggregateRegexMap.put(AggregateTypeEnum.MIN, Pattern.compile("(?i)(最小值|最小|min|最低|最少)"));
        aggregateRegexMap.put(AggregateTypeEnum.SUM, Pattern.compile("(?i)(汇总|总和|sum)"));
        aggregateRegexMap.put(AggregateTypeEnum.AVG, Pattern.compile("(?i)(平均值|日均|平均|avg)"));
        aggregateRegexMap.put(AggregateTypeEnum.TOPN, Pattern.compile("(?i)(top)"));
        aggregateRegexMap.put(AggregateTypeEnum.DISTINCT, Pattern.compile("(?i)(uv)"));
        aggregateRegexMap.put(AggregateTypeEnum.COUNT, Pattern.compile("(?i)(总数|pv)"));
        aggregateRegexMap.put(AggregateTypeEnum.NONE, Pattern.compile("(?i)(明细)"));
    }

    public static AggregateTypeEnum resolveAggregateType(String queryText) {

        Map<AggregateTypeEnum, Integer> aggregateCount = new HashMap<>(aggregateRegexMap.size());
        for (Map.Entry<AggregateTypeEnum, Pattern> entry : aggregateRegexMap.entrySet()) {
            Matcher matcher = entry.getValue().matcher(queryText);
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            if (count > 0) {
                aggregateCount.put(entry.getKey(), count);
            }
        }

        return aggregateCount.entrySet().stream().max(Map.Entry.comparingByValue()).map(entry -> entry.getKey())
                .orElse(null);
    }

    @Override
    public void parse(QueryContextReq queryContext, ChatContext chatContext) {
        AggregateTypeEnum aggregateType = resolveAggregateType(queryContext.getQueryText());

        for (SemanticQuery semanticQuery : queryContext.getCandidateQueries()) {
            SemanticParseInfo semanticParse = semanticQuery.getParseInfo();

            semanticParse.setNativeQuery(getNativeQuery(aggregateType, semanticParse));
            semanticParse.setAggType(aggregateType);
            if (Objects.isNull(semanticParse.getLimit()) || semanticParse.getLimit() <= 0) {
                semanticParse.setLimit(Long.valueOf(TOPN_LIMIT));
            }
            resetQueryModeByAggregateType(semanticParse, aggregateType);
        }
    }

    /**
     * query mode reset by the AggregateType
     *
     * @param parseInfo
     * @param aggregateType
     */
    private void resetQueryModeByAggregateType(SemanticParseInfo parseInfo,
            AggregateTypeEnum aggregateType) {

        String queryMode = parseInfo.getQueryMode();
        if (MetricGroupBy.QUERY_MODE.equals(queryMode) || MetricGroupBy.QUERY_MODE.equals(queryMode)) {
            if (AggregateTypeEnum.MAX.equals(aggregateType) || AggregateTypeEnum.MIN.equals(aggregateType)
                    || AggregateTypeEnum.TOPN.equals(aggregateType)) {
                parseInfo.setQueryMode(MetricOrderBy.QUERY_MODE);
            } else {
                parseInfo.setQueryMode(MetricGroupBy.QUERY_MODE);
            }
            log.info("queryMode mode  [{}]->[{}]", queryMode, parseInfo.getQueryMode());
        }
    }

    private boolean getNativeQuery(AggregateTypeEnum aggregateType, SemanticParseInfo semanticParse) {
        if (AggregateTypeEnum.TOPN.equals(aggregateType)) {
            return true;
        }
        if (EntityListFilter.QUERY_MODE.equals(semanticParse.getQueryMode()) && (semanticParse.getMetrics() == null
                || semanticParse.getMetrics().isEmpty())) {
            return true;
        }
        return semanticParse.getNativeQuery();
    }
}
