package com.tencent.supersonic.chat.application.query;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import org.springframework.stereotype.Service;

@Service
public class MetricCompare extends BaseSemanticQuery {

    public static String QUERY_MODE = "METRIC_COMPARE";


    public MetricCompare() {
        queryModeOption.setAggregation(QueryModeElementOption.optional());
        queryModeOption.setDate(QueryModeElementOption.optional());
        queryModeOption.setDimension(QueryModeElementOption.unused());
        queryModeOption.setFilter(SchemaElementOption.REQUIRED, QueryModeElementOption.RequireNumberType.AT_LEAST, 1);
        queryModeOption.setMetric(SchemaElementOption.REQUIRED, QueryModeElementOption.RequireNumberType.AT_LEAST, 1);
        queryModeOption.setEntity(QueryModeElementOption.unused());
        queryModeOption.setDomain(QueryModeElementOption.optional());
        queryModeOption.setSupportCompare(true);
        queryModeOption.setSupportOrderBy(true);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public SemanticParseInfo getParseInfo(QueryContextReq queryCtx, ChatContext chatCt) {
        SemanticParseInfo semanticParseInfo = chatCt.getParseInfo();
        ContextHelper.updateTime(queryCtx.getParseInfo(), semanticParseInfo);
        ContextHelper.updateDomain(queryCtx.getParseInfo(), semanticParseInfo);
        ContextHelper.updateSemanticQuery(queryCtx.getParseInfo(), semanticParseInfo);
        ContextHelper.addIfEmpty(queryCtx.getParseInfo().getDimensionFilters(),
                semanticParseInfo.getDimensionFilters());
        ContextHelper.updateList(queryCtx.getParseInfo().getMetrics(), semanticParseInfo.getMetrics());
        ContextHelper.updateEntity(queryCtx.getParseInfo(), semanticParseInfo);
        return semanticParseInfo;
    }

    @Override
    public SemanticParseInfo getContext(ChatContext chatCtx, QueryContextReq queryCtx) {
        SemanticParseInfo semanticParseInfo = queryCtx.getParseInfo();
        ContextHelper.updateTimeIfEmpty(chatCtx.getParseInfo(), semanticParseInfo);
        ContextHelper.addIfEmpty(chatCtx.getParseInfo().getMetrics(), semanticParseInfo.getMetrics());
        ContextHelper.appendList(chatCtx.getParseInfo().getDimensionFilters(), semanticParseInfo.getDimensionFilters());
        return semanticParseInfo;
    }
}
