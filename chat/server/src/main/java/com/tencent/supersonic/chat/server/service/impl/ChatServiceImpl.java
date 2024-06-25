package com.tencent.supersonic.chat.server.service.impl;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatQueryDataReq;
import com.tencent.supersonic.chat.api.pojo.request.SimilarQueryReq;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.executor.ChatExecutor;
import com.tencent.supersonic.chat.server.parser.ChatParser;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.server.pojo.ChatExecuteContext;
import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.chat.server.processor.execute.ExecuteResultProcessor;
import com.tencent.supersonic.chat.server.processor.parse.ParseResultProcessor;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.chat.server.service.ChatService;
import com.tencent.supersonic.chat.server.util.ComponentFactory;
import com.tencent.supersonic.chat.server.util.QueryReqConverter;
import com.tencent.supersonic.chat.server.util.SimilarQueryManager;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryDataReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.response.MapResp;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;
import com.tencent.supersonic.headless.server.service.ChatQueryService;
import com.tencent.supersonic.headless.server.service.RetrieveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatManageService chatManageService;
    @Autowired
    private ChatQueryService chatQueryService;
    @Autowired
    private RetrieveService searchService;
    @Autowired
    private SimilarQueryManager similarQueryManager;
    private List<ChatParser> chatParsers = ComponentFactory.getChatParsers();
    private List<ChatExecutor> chatExecutors = ComponentFactory.getChatExecutors();
    private List<ParseResultProcessor> parseResultProcessors = ComponentFactory.getParseProcessors();
    private List<ExecuteResultProcessor> executeResultProcessors = ComponentFactory.getExecuteProcessors();

    @Override
    public List<SearchResult> search(ChatParseReq chatParseReq) {
        ChatParseContext chatParseContext = buildParseContext(chatParseReq);
        QueryReq queryReq = QueryReqConverter.buildText2SqlQueryReq(chatParseContext);
        return searchService.search(queryReq);
    }

    @Override
    public ParseResp performParsing(ChatParseReq chatParseReq) {
        ParseResp parseResp = new ParseResp(chatParseReq.getChatId(), chatParseReq.getQueryText());
        chatManageService.createChatQuery(chatParseReq, parseResp);
        ChatParseContext chatParseContext = buildParseContext(chatParseReq);
        for (ChatParser chatParser : chatParsers) {
            chatParser.parse(chatParseContext, parseResp);
        }
        for (ParseResultProcessor processor : parseResultProcessors) {
            processor.process(chatParseContext, parseResp);
        }
        chatParseReq.setQueryText(chatParseContext.getQueryText());
        parseResp.setQueryText(chatParseContext.getQueryText());
        chatManageService.batchAddParse(chatParseReq, parseResp);
        chatManageService.updateParseCostTime(parseResp);
        return parseResp;
    }

    @Override
    public QueryResult performExecution(ChatExecuteReq chatExecuteReq) {
        QueryResult queryResult = new QueryResult();
        ChatExecuteContext chatExecuteContext = buildExecuteContext(chatExecuteReq);
        for (ChatExecutor chatExecutor : chatExecutors) {
            queryResult = chatExecutor.execute(chatExecuteContext);
            if (queryResult != null) {
                break;
            }
        }
        for (ExecuteResultProcessor processor : executeResultProcessors) {
            processor.process(chatExecuteContext, queryResult);
        }
        saveQueryResult(chatExecuteReq, queryResult);
        return queryResult;
    }

    private ChatParseContext buildParseContext(ChatParseReq chatParseReq) {
        ChatParseContext chatParseContext = new ChatParseContext();
        BeanMapper.mapper(chatParseReq, chatParseContext);
        AgentService agentService = ContextUtils.getBean(AgentService.class);
        Agent agent = agentService.getAgent(chatParseReq.getAgentId());
        chatParseContext.setAgent(agent);
        QueryReq queryReq = QueryReqConverter.buildText2SqlQueryReq(chatParseContext);
        MapResp mapResp = chatQueryService.performMapping(queryReq);
        chatParseContext.setMapInfo(mapResp.getMapInfo());
        return chatParseContext;
    }

    private ChatExecuteContext buildExecuteContext(ChatExecuteReq chatExecuteReq) {
        ChatExecuteContext chatExecuteContext = new ChatExecuteContext();
        BeanMapper.mapper(chatExecuteReq, chatExecuteContext);
        SemanticParseInfo parseInfo = chatManageService.getParseInfo(
                chatExecuteReq.getQueryId(), chatExecuteReq.getParseId());
        chatExecuteContext.setParseInfo(parseInfo);
        return chatExecuteContext;
    }

    @Override
    public Object queryData(ChatQueryDataReq chatQueryDataReq, User user) throws Exception {
        Integer parseId = chatQueryDataReq.getParseId();
        SemanticParseInfo parseInfo = chatManageService.getParseInfo(
                chatQueryDataReq.getQueryId(), parseId);
        QueryDataReq queryData = new QueryDataReq();
        BeanMapper.mapper(chatQueryDataReq, queryData);
        queryData.setParseInfo(parseInfo);
        return chatQueryService.executeDirectQuery(queryData, user);
    }

    @Override
    public SemanticParseInfo queryContext(Integer chatId) {
        return chatQueryService.queryContext(chatId);
    }

    @Override
    public Object queryDimensionValue(DimensionValueReq dimensionValueReq, User user) throws Exception {
        return chatQueryService.queryDimensionValue(dimensionValueReq, user);
    }

    public void saveQueryResult(ChatExecuteReq chatExecuteReq, QueryResult queryResult) {
        //The history record only retains the query result of the first parse
        if (chatExecuteReq.getParseId() > 1) {
            return;
        }
        ChatQueryDO chatQueryDO = chatManageService.saveQueryResult(chatExecuteReq, queryResult);
        SimilarQueryReq similarQueryReq = SimilarQueryReq.builder().queryId(chatExecuteReq.getQueryId())
                .queryText(chatQueryDO.getQueryText()).agentId(chatQueryDO.getAgentId()).build();
        similarQueryManager.saveSimilarQuery(similarQueryReq);
    }

}
