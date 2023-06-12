package com.tencent.supersonic.chat.application.query;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.SchemaElementCount;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.response.QueryResultResp;
import com.tencent.supersonic.chat.api.service.SemanticLayer;
import com.tencent.supersonic.chat.api.service.SemanticQuery;
import com.tencent.supersonic.semantic.api.core.pojo.QueryColumn;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.chat.application.DomainEntityService;
import com.tencent.supersonic.chat.application.parser.resolver.DomainResolver;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichInfo;
import com.tencent.supersonic.chat.domain.pojo.search.QueryState;
import com.tencent.supersonic.chat.domain.service.ChatService;
import com.tencent.supersonic.chat.domain.utils.DefaultSemanticInternalUtils;
import com.tencent.supersonic.chat.domain.utils.SchemaInfoConverter;
import com.tencent.supersonic.common.util.context.ContextUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public abstract class BaseSemanticQuery implements SemanticQuery {

    protected QueryModeOption queryModeOption = QueryModeOption.build();
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSemanticQuery.class);

    @Override
    public QueryResultResp execute(QueryContextReq queryCtx, ChatContext chatCtx) {

        DomainResolver domainResolver = ContextUtils.getBean(DomainResolver.class);
        ChatService chatService = ContextUtils.getBean(ChatService.class);
        SemanticLayer semanticLayer = ContextUtils.getBean(SemanticLayer.class);

        SemanticParseInfo semanticParse = queryCtx.getParseInfo();

        String queryMode = semanticParse.getQueryMode();

        if (semanticParse.getDomainId() < 0 || StringUtils.isEmpty(queryMode)) {
            // reach here some error may happen
            LOGGER.error("not find QueryMode");
            throw new RuntimeException("not find QueryMode");
        }

        supplyMetadata(semanticLayer, queryCtx);

        // is domain switch
        if (domainResolver.isDomainSwitch(chatCtx, queryCtx)) {
            chatService.switchContext(chatCtx);
        }
        // submit semantic query based on the result of semantic parsing
        SemanticParseInfo semanticParseInfo = getContext(chatCtx, queryCtx);
        QueryResultResp queryResponse = new QueryResultResp();
        QueryResultWithSchemaResp queryResult = semanticLayer.queryByStruct(
                SchemaInfoConverter.convertTo(semanticParseInfo), queryCtx.getUser());
        if (queryResult != null) {
            queryResponse.setQueryAuthorization(queryResult.getQueryAuthorization());
        }
        String sql = queryResult == null ? null : queryResult.getSql();
        List<Map<String, Object>> resultList = queryResult == null ? new ArrayList<>()
                : queryResult.getResultList();
        List<QueryColumn> columns = queryResult == null ? new ArrayList<>() : queryResult.getColumns();
        queryResponse.setQuerySql(sql);
        queryResponse.setQueryResults(resultList);
        queryResponse.setQueryColumns(columns);
        queryResponse.setQueryMode(queryMode);

        // add domain info
        EntityInfo entityInfo = ContextUtils.getBean(DomainEntityService.class)
                .getEntityInfo(queryCtx, chatCtx, queryCtx.getUser());
        queryResponse.setEntityInfo(entityInfo);
        return queryResponse;

    }

    private void supplyMetadata(SemanticLayer semanticLayer, QueryContextReq queryCtx) {
        DefaultSemanticInternalUtils defaultSemanticUtils = ContextUtils.getBean(DefaultSemanticInternalUtils.class);

        SchemaMapInfo mapInfo = queryCtx.getMapInfo();
        SemanticParseInfo semanticParse = queryCtx.getParseInfo();
        Long domain = semanticParse.getDomainId();
        List<SchemaElementMatch> schemaElementMatches = mapInfo.getMatchedElements(domain.intValue());
        DomainSchemaResp domainSchemaDesc = semanticLayer.getDomainSchemaInfo(domain);
        ChatConfigRichInfo chaConfigRichDesc = defaultSemanticUtils.getChatConfigRichInfo(domain);

        // supply metadata
        if (!CollectionUtils.isEmpty(schemaElementMatches)) {
            this.queryModeOption.addQuerySemanticParseInfo(schemaElementMatches, domainSchemaDesc,
                    chaConfigRichDesc, semanticParse);
        }
    }

    @Override
    public void updateContext(QueryResultResp queryResponse, ChatContext chatCtx, QueryContextReq queryCtx) {
        if (queryCtx.isSaveAnswer() && queryResponse != null
                && queryResponse.getQueryState() == QueryState.NORMAL.getState()) {
            chatCtx.setParseInfo(getParseInfo(queryCtx, chatCtx));
            chatCtx.setQueryText(queryCtx.getQueryText());
            ContextUtils.getBean(ChatService.class).updateContext(chatCtx);
        }
        queryResponse.setChatContext(queryCtx.getParseInfo());
    }

    public abstract SemanticParseInfo getParseInfo(QueryContextReq queryCtx, ChatContext chatCtx);


    @Override
    public SchemaElementCount match(List<SchemaElementMatch> elementMatches, QueryContextReq queryCtx) {
        return queryModeOption.match(elementMatches, queryCtx);
    }

}
