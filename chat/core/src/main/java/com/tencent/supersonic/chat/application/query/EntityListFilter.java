package com.tencent.supersonic.chat.application.query;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import org.springframework.stereotype.Service;

@Service
public class EntityListFilter extends BaseSemanticQuery {

    public static String QUERY_MODE = "ENTITY_LIST_FILTER";

    public EntityListFilter() {
        queryModeOption.setAggregation(QueryModeElementOption.unused());
        queryModeOption.setDate(QueryModeElementOption.unused());
        queryModeOption.setDimension(QueryModeElementOption.unused());
        queryModeOption.setFilter(SchemaElementOption.REQUIRED, QueryModeElementOption.RequireNumberType.AT_LEAST, 1);
        queryModeOption.setMetric(QueryModeElementOption.unused());
        queryModeOption.setEntity(SchemaElementOption.REQUIRED, QueryModeElementOption.RequireNumberType.AT_LEAST, 1);
        queryModeOption.setDomain(QueryModeElementOption.optional());
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public SemanticParseInfo getParseInfo(QueryContextReq queryCtx, ChatContext chatCtx) {
        SemanticParseInfo semanticParseInfo = chatCtx.getParseInfo();
        ContextHelper.updateDomain(queryCtx.getParseInfo(), semanticParseInfo);
        ContextHelper.updateSemanticQuery(queryCtx.getParseInfo(), semanticParseInfo);
        ContextHelper.updateList(queryCtx.getParseInfo().getDimensionFilters(),
                semanticParseInfo.getDimensionFilters());
        ContextHelper.updateEntity(queryCtx.getParseInfo(), semanticParseInfo);
        return semanticParseInfo;
    }

    @Override
    public SemanticParseInfo getContext(ChatContext chatCtx, QueryContextReq queryCtx) {
        SemanticParseInfo semanticParseInfo = queryCtx.getParseInfo();
        ContextHelper.addIfEmpty(chatCtx.getParseInfo().getDimensionFilters(), semanticParseInfo.getDimensionFilters());
        return semanticParseInfo;
    }

}
