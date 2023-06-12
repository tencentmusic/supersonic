package com.tencent.supersonic.chat.application.parser;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.service.SemanticParser;
import com.tencent.supersonic.chat.application.parser.resolver.AggregateTypeResolver;
import com.tencent.supersonic.chat.application.query.MetricCompare;
import com.tencent.supersonic.chat.application.query.MetricFilter;
import com.tencent.supersonic.chat.application.query.MetricGroupBy;
import com.tencent.supersonic.chat.application.query.MetricOrderBy;
import com.tencent.supersonic.common.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.common.util.context.ContextUtils;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AggregateSemanticParser implements SemanticParser {

    private final Logger logger = LoggerFactory.getLogger(AggregateSemanticParser.class);
    public static final Integer TOPN_LIMIT = 10;

    private AggregateTypeResolver aggregateTypeResolver;

    @Override
    public boolean parse(QueryContextReq queryContext, ChatContext chatCtx) {
        aggregateTypeResolver = ContextUtils.getBean(AggregateTypeResolver.class);

        AggregateTypeEnum aggregateType = aggregateTypeResolver.resolve(queryContext.getQueryText());

        SemanticParseInfo semanticParse = queryContext.getParseInfo();

        List<SchemaItem> metrics = semanticParse.getMetrics();

        semanticParse.setNativeQuery(getNativeQuery(aggregateType, queryContext));

        semanticParse.setAggType(aggregateType);
        //semanticParse.setOrders(getOrder(aggregateType, metrics));
        semanticParse.setLimit(Long.valueOf(TOPN_LIMIT));
        resetQueryModeByAggregateType(queryContext, aggregateType);
        return false;
    }

    /**
     * query mode reset by the AggregateType
     *
     * @param searchCtx
     * @param aggregateType
     */
    private void resetQueryModeByAggregateType(QueryContextReq searchCtx, AggregateTypeEnum aggregateType) {

        SemanticParseInfo parseInfo = searchCtx.getParseInfo();
        String queryMode = parseInfo.getQueryMode();
        if (MetricGroupBy.QUERY_MODE.equals(queryMode) || MetricGroupBy.QUERY_MODE.equals(queryMode)) {
            if (AggregateTypeEnum.MAX.equals(aggregateType) || AggregateTypeEnum.MIN.equals(aggregateType)
                    || AggregateTypeEnum.TOPN.equals(aggregateType)) {
                parseInfo.setQueryMode(MetricOrderBy.QUERY_MODE);
            } else {
                parseInfo.setQueryMode(MetricGroupBy.QUERY_MODE);
            }
        }
        if (MetricFilter.QUERY_MODE.equals(queryMode) || MetricCompare.QUERY_MODE.equals(queryMode)) {
            if (aggregateTypeResolver.hasCompareIntentionalWords(searchCtx.getQueryText())) {
                parseInfo.setQueryMode(MetricCompare.QUERY_MODE);
            } else {
                parseInfo.setQueryMode(MetricFilter.QUERY_MODE);
            }
        }
        logger.info("queryMode mode  [{}]->[{}]", queryMode, parseInfo.getQueryMode());
    }

    private boolean getNativeQuery(AggregateTypeEnum aggregateType, QueryContextReq searchCtx) {
        if (AggregateTypeEnum.TOPN.equals(aggregateType)) {
            return true;
        }
        return searchCtx.getParseInfo().getNativeQuery();
    }


}
