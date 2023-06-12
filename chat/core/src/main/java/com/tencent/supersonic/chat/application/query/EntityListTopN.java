package com.tencent.supersonic.chat.application.query;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import org.springframework.stereotype.Service;

@Service
public class EntityListTopN extends BaseSemanticQuery {

    public static String QUERY_MODE = "ENTITY_LIST_TOPN";


    public EntityListTopN() {
        queryModeOption.setAggregation(QueryModeElementOption.optional());
        queryModeOption.setDate(QueryModeElementOption.optional());
        queryModeOption.setDimension(QueryModeElementOption.unused());
        queryModeOption.setFilter(QueryModeElementOption.unused());
        queryModeOption.setMetric(SchemaElementOption.REQUIRED, QueryModeElementOption.RequireNumberType.AT_LEAST, 1);
        queryModeOption.setEntity(SchemaElementOption.REQUIRED, QueryModeElementOption.RequireNumberType.AT_LEAST, 1);
        queryModeOption.setDomain(QueryModeElementOption.optional());
        queryModeOption.setSupportOrderBy(true);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public SemanticParseInfo getParseInfo(QueryContextReq queryCtx, ChatContext chatCtx) {
        SemanticParseInfo semanticParseInfo = chatCtx.getParseInfo();
        ContextHelper.updateTime(queryCtx.getParseInfo(), semanticParseInfo);
        ContextHelper.updateDomain(queryCtx.getParseInfo(), semanticParseInfo);
        ContextHelper.updateSemanticQuery(queryCtx.getParseInfo(), semanticParseInfo);
        ContextHelper.updateList(queryCtx.getParseInfo().getMetrics(), semanticParseInfo.getMetrics());
        ContextHelper.updateEntity(queryCtx.getParseInfo(), semanticParseInfo);
        return semanticParseInfo;
    }

    @Override
    public SemanticParseInfo getContext(ChatContext chatCtx, QueryContextReq queryCtx) {
        SemanticParseInfo semanticParseInfo = queryCtx.getParseInfo();
        ContextHelper.updateTimeIfEmpty(chatCtx.getParseInfo(), semanticParseInfo);
        ContextHelper.addIfEmpty(chatCtx.getParseInfo().getMetrics(), semanticParseInfo.getMetrics());
        return semanticParseInfo;
    }

}
