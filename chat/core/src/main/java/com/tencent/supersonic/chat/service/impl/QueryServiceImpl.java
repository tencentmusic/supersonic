package com.tencent.supersonic.chat.service.impl;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.request.QueryDataReq;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.QuerySelector;
import com.tencent.supersonic.chat.service.ChatService;
import com.tencent.supersonic.chat.service.QueryService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.common.util.JsonUtil;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
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
    private QuerySelector querySelector = ComponentFactory.getQuerySelector();

    @Override
    public ParseResp performParsing(QueryReq queryReq) {
        QueryContext queryCtx = new QueryContext(queryReq);
        // in order to support multi-turn conversation, chat context is needed
        ChatContext chatCtx = chatService.getOrCreateContext(queryReq.getChatId());

        schemaMappers.stream().forEach(mapper -> {
            mapper.map(queryCtx);
            log.info("{} result:{}", mapper.getClass().getSimpleName(), JsonUtil.toString(queryCtx));
        });

        semanticParsers.stream().forEach(parser -> {
            parser.parse(queryCtx, chatCtx);
            log.info("{} result:{}", parser.getClass().getSimpleName(), JsonUtil.toString(queryCtx));
        });

        ParseResp parseResult;
        if (queryCtx.getCandidateQueries().size() > 0) {
            log.debug("pick before [{}]", queryCtx.getCandidateQueries().stream().collect(
                    Collectors.toList()));
            List<SemanticQuery> selectedQueries = querySelector.select(queryCtx.getCandidateQueries());
            log.debug("pick after [{}]", selectedQueries.stream().collect(
                    Collectors.toList()));

            List<SemanticParseInfo> selectedParses = selectedQueries.stream()
                    .map(SemanticQuery::getParseInfo).collect(Collectors.toList());
            List<SemanticParseInfo> candidateParses = queryCtx.getCandidateQueries().stream()
                    .map(SemanticQuery::getParseInfo).collect(Collectors.toList());

            parseResult = ParseResp.builder()
                    .chatId(queryReq.getChatId())
                    .queryText(queryReq.getQueryText())
                    .state(selectedParses.size() > 1 ? ParseResp.ParseState.PENDING : ParseResp.ParseState.COMPLETED)
                    .selectedParses(selectedParses)
                    .candidateParses(candidateParses)
                    .build();
        } else {
            parseResult = ParseResp.builder()
                    .chatId(queryReq.getChatId())
                    .queryText(queryReq.getQueryText())
                    .state(ParseResp.ParseState.FAILED)
                    .build();
        }

        return parseResult;
    }

    @Override
    public QueryResult performExecution(ExecuteQueryReq queryReq) throws Exception {
        SemanticParseInfo parseInfo = queryReq.getParseInfo();
        SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());
        if (semanticQuery == null) {
            return null;
        }
        semanticQuery.setParseInfo(parseInfo);

        // in order to support multi-turn conversation, chat context is needed
        ChatContext chatCtx = chatService.getOrCreateContext(queryReq.getChatId());

        QueryResult queryResult = semanticQuery.execute(queryReq.getUser());
        if (queryResult != null) {
            queryResult.setChatContext(parseInfo);
            // update chat context after a successful semantic query
            if (queryReq.isSaveAnswer() && QueryState.SUCCESS.equals(queryResult.getQueryState())) {
                chatCtx.setParseInfo(parseInfo);
                chatService.updateContext(chatCtx);
            }
            chatCtx.setQueryText(queryReq.getQueryText());
            chatCtx.setUser(queryReq.getUser().getName());
            chatService.addQuery(queryResult, chatCtx);
        }

        return queryResult;
    }

    @Override
    public QueryResult executeQuery(QueryReq queryReq) throws Exception {
        QueryContext queryCtx = new QueryContext(queryReq);
        // in order to support multi-turn conversation, chat context is needed
        ChatContext chatCtx = chatService.getOrCreateContext(queryReq.getChatId());

        schemaMappers.stream().forEach(mapper -> {
            mapper.map(queryCtx);
            log.info("{} result:{}", mapper.getClass().getSimpleName(), JsonUtil.toString(queryCtx));
        });

        semanticParsers.stream().forEach(parser -> {
            parser.parse(queryCtx, chatCtx);
            log.info("{} result:{}", parser.getClass().getSimpleName(), JsonUtil.toString(queryCtx));
        });

        QueryResult queryResult = null;
        if (queryCtx.getCandidateQueries().size() > 0) {
            log.info("pick before [{}]", queryCtx.getCandidateQueries().stream().collect(
                    Collectors.toList()));
            List<SemanticQuery> selectedQueries = querySelector.select(queryCtx.getCandidateQueries());
            log.info("pick after [{}]", selectedQueries.stream().collect(
                    Collectors.toList()));

            SemanticQuery semanticQuery = selectedQueries.get(0);
            queryResult = semanticQuery.execute(queryReq.getUser());
            if (queryResult != null) {
                chatCtx.setQueryText(queryReq.getQueryText());
                // update chat context after a successful semantic query
                if (queryReq.isSaveAnswer() && QueryState.SUCCESS.equals(queryResult.getQueryState())) {
                    chatCtx.setParseInfo(semanticQuery.getParseInfo());
                    chatService.updateContext(chatCtx);
                }
                queryResult.setChatContext(chatCtx.getParseInfo());
                chatService.addQuery(queryResult, chatCtx);
            }
        }

        return queryResult;
    }

    @Override
    public SemanticParseInfo queryContext(QueryReq queryCtx) {
        ChatContext context = chatService.getOrCreateContext(queryCtx.getChatId());
        return context.getParseInfo();
    }

    @Override
    public QueryResult executeDirectQuery(QueryDataReq queryData, User user) throws SqlParseException {
        SemanticQuery semanticQuery = QueryManager.createRuleQuery(queryData.getQueryMode());
        BeanUtils.copyProperties(queryData, semanticQuery.getParseInfo());
        return semanticQuery.execute(user);
    }

}

