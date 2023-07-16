package com.tencent.supersonic.chat.application;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.*;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.response.QueryResultResp;
import com.tencent.supersonic.chat.application.query.QuerySelector;
import com.tencent.supersonic.chat.domain.pojo.chat.QueryData;
import com.tencent.supersonic.chat.domain.pojo.search.QueryState;
import com.tencent.supersonic.chat.domain.service.QueryService;
import com.tencent.supersonic.chat.domain.service.ChatService;
import com.tencent.supersonic.chat.domain.utils.ComponentFactory;
import com.tencent.supersonic.chat.domain.utils.SchemaInfoConverter;
import com.tencent.supersonic.common.util.json.JsonUtil;
import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Component("chatQueryService")
@Primary
@Slf4j
public class QueryServiceImpl implements QueryService {

    @Autowired
    private ChatService chatService;

    private List<SchemaMapper> schemaMappers = ComponentFactory.getSchemaMappers();
    private List<SemanticParser> semanticParsers = ComponentFactory.getSemanticParsers();
    private SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();
    private QuerySelector querySelector = ComponentFactory.getQuerySelector();

    @Override
    public QueryResultResp executeQuery(QueryContextReq queryCtx) throws Exception {
        schemaMappers.stream().forEach(s -> s.map(queryCtx));

        // in order to support multi-turn conversation, we need to consider chat context
        ChatContext chatCtx = chatService.getOrCreateContext(queryCtx.getChatId());

        for (SemanticParser semanticParser : semanticParsers) {
            log.info("semanticParser processing:[{}]", semanticParser.getClass().getName());
            semanticParser.parse(queryCtx, chatCtx);
        }
        if (queryCtx.getCandidateQueries().size() > 0) {
            log.info("pick before [{}]", queryCtx.getCandidateQueries().stream().collect(
                    Collectors.toList()));
            SemanticQuery semanticQuery = querySelector.select(queryCtx.getCandidateQueries());
            log.info("pick after [{}]", semanticQuery);

            QueryResultResp queryResponse = semanticQuery.execute(queryCtx.getUser());
            if (queryResponse != null) {
                // update chat context after a successful semantic query
                if (queryCtx.isSaveAnswer() && queryResponse.getQueryState() == QueryState.NORMAL.getState()) {
                    chatService.updateContext(chatCtx, queryCtx, semanticQuery.getParseInfo());
                }
                queryResponse.setChatContext(chatCtx.getParseInfo());
                chatService.addQuery(queryResponse, queryCtx, chatCtx);
                return queryResponse;
            }
        }

        return null;
    }

    @Override
    public SemanticParseInfo queryContext(QueryContextReq queryCtx) {
        ChatContext context = chatService.getOrCreateContext(queryCtx.getChatId());
        return context.getParseInfo();
    }

    @Override
    public QueryResultResp executeDirectQuery(QueryData queryData, User user) throws Exception {
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        QueryResultResp queryResponse = new QueryResultResp();
        BeanUtils.copyProperties(queryData, semanticParseInfo);
        QueryResultWithSchemaResp resultWithColumns = semanticLayer.queryByStruct(
                SchemaInfoConverter.convertTo(semanticParseInfo), user);
        queryResponse.setQueryColumns(resultWithColumns.getColumns());
        queryResponse.setQueryResults(resultWithColumns.getResultList());
        return queryResponse;
    }
}

