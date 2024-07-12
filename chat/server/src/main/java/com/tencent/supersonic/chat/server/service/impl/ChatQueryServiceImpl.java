package com.tencent.supersonic.chat.server.service.impl;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatQueryDataReq;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.executor.ChatQueryExecutor;
import com.tencent.supersonic.chat.server.parser.ChatQueryParser;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.chat.server.processor.execute.ExecuteResultProcessor;
import com.tencent.supersonic.chat.server.processor.parse.ParseResultProcessor;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatContextService;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.chat.server.service.ChatQueryService;
import com.tencent.supersonic.chat.server.util.ComponentFactory;
import com.tencent.supersonic.chat.server.util.QueryReqConverter;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryDataReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import com.tencent.supersonic.headless.api.pojo.response.MapResp;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;
import com.tencent.supersonic.headless.server.facade.service.ChatLayerService;
import com.tencent.supersonic.headless.server.facade.service.RetrieveService;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;


@Slf4j
@Service
public class ChatQueryServiceImpl implements ChatQueryService {

    @Autowired
    private ChatManageService chatManageService;
    @Autowired
    private ChatLayerService chatLayerService;
    @Autowired
    private RetrieveService retrieveService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private SemanticLayerService semanticLayerService;
    @Autowired
    private ChatContextService chatContextService;

    private List<ChatQueryParser> chatQueryParsers = ComponentFactory.getChatParsers();
    private List<ChatQueryExecutor> chatQueryExecutors = ComponentFactory.getChatExecutors();
    private List<ParseResultProcessor> parseResultProcessors = ComponentFactory.getParseProcessors();
    private List<ExecuteResultProcessor> executeResultProcessors = ComponentFactory.getExecuteProcessors();

    @Override
    public List<SearchResult> search(ChatParseReq chatParseReq) {
        ParseContext parseContext = buildParseContext(chatParseReq);
        Agent agent = parseContext.getAgent();
        if (!agent.enableSearch()) {
            return Lists.newArrayList();
        }
        QueryNLReq queryNLReq = QueryReqConverter.buildText2SqlQueryReq(parseContext);
        return retrieveService.retrieve(queryNLReq);
    }

    @Override
    public ParseResp performParsing(ChatParseReq chatParseReq) {
        ParseResp parseResp = new ParseResp(chatParseReq.getQueryText());
        chatManageService.createChatQuery(chatParseReq, parseResp);
        ParseContext parseContext = buildParseContext(chatParseReq);
        supplyMapInfo(parseContext);
        for (ChatQueryParser chatQueryParser : chatQueryParsers) {
            chatQueryParser.parse(parseContext, parseResp);
        }
        for (ParseResultProcessor processor : parseResultProcessors) {
            processor.process(parseContext, parseResp);
        }
        chatParseReq.setQueryText(parseContext.getQueryText());
        parseResp.setQueryText(parseContext.getQueryText());
        chatManageService.batchAddParse(chatParseReq, parseResp);
        chatManageService.updateParseCostTime(parseResp);
        return parseResp;
    }

    @Override
    public QueryResult performExecution(ChatExecuteReq chatExecuteReq) {
        QueryResult queryResult = new QueryResult();
        ExecuteContext executeContext = buildExecuteContext(chatExecuteReq);
        for (ChatQueryExecutor chatQueryExecutor : chatQueryExecutors) {
            queryResult = chatQueryExecutor.execute(executeContext);
            if (queryResult != null) {
                break;
            }
        }

        if (queryResult != null) {
            for (ExecuteResultProcessor processor : executeResultProcessors) {
                processor.process(executeContext, queryResult);
            }
            saveQueryResult(chatExecuteReq, queryResult);
        }

        return queryResult;
    }

    @Override
    public QueryResult parseAndExecute(int chatId, int agentId, String queryText) {
        ChatParseReq chatParseReq = new ChatParseReq();
        chatParseReq.setQueryText(queryText);
        chatParseReq.setChatId(chatId);
        chatParseReq.setAgentId(agentId);
        chatParseReq.setUser(User.getFakeUser());
        ParseResp parseResp = performParsing(chatParseReq);
        if (CollectionUtils.isEmpty(parseResp.getSelectedParses())) {
            log.debug("chatId:{}, agentId:{}, queryText:{}, parseResp.getSelectedParses() is empty",
                    chatId, agentId, queryText);
            return null;
        }
        ChatExecuteReq executeReq = new ChatExecuteReq();
        executeReq.setQueryId(parseResp.getQueryId());
        executeReq.setParseId(parseResp.getSelectedParses().get(0).getId());
        executeReq.setQueryText(queryText);
        executeReq.setChatId(chatId);
        executeReq.setUser(User.getFakeUser());
        executeReq.setAgentId(agentId);
        executeReq.setSaveAnswer(true);
        return performExecution(executeReq);
    }

    private ParseContext buildParseContext(ChatParseReq chatParseReq) {
        ParseContext parseContext = new ParseContext();
        BeanMapper.mapper(chatParseReq, parseContext);
        Agent agent = agentService.getAgent(chatParseReq.getAgentId());
        parseContext.setAgent(agent);
        return parseContext;
    }

    private void supplyMapInfo(ParseContext parseContext) {
        QueryNLReq queryNLReq = QueryReqConverter.buildText2SqlQueryReq(parseContext);
        MapResp mapResp = chatLayerService.performMapping(queryNLReq);
        parseContext.setMapInfo(mapResp.getMapInfo());
    }

    private ExecuteContext buildExecuteContext(ChatExecuteReq chatExecuteReq) {
        ExecuteContext executeContext = new ExecuteContext();
        BeanMapper.mapper(chatExecuteReq, executeContext);
        SemanticParseInfo parseInfo = chatManageService.getParseInfo(
                chatExecuteReq.getQueryId(), chatExecuteReq.getParseId());
        Agent agent = agentService.getAgent(chatExecuteReq.getAgentId());
        executeContext.setAgent(agent);
        executeContext.setParseInfo(parseInfo);
        return executeContext;
    }

    @Override
    public Object queryData(ChatQueryDataReq chatQueryDataReq, User user) throws Exception {
        Integer parseId = chatQueryDataReq.getParseId();
        SemanticParseInfo parseInfo = chatManageService.getParseInfo(
                chatQueryDataReq.getQueryId(), parseId);
        QueryDataReq queryData = new QueryDataReq();
        BeanMapper.mapper(chatQueryDataReq, queryData);
        queryData.setParseInfo(parseInfo);
        return chatLayerService.executeDirectQuery(queryData, user);
    }

    @Override
    public Object queryDimensionValue(DimensionValueReq dimensionValueReq, User user) throws Exception {
        Integer agentId = dimensionValueReq.getAgentId();
        Agent agent = agentService.getAgent(agentId);
        dimensionValueReq.setDataSetIds(agent.getDataSetIds());
        return chatLayerService.queryDimensionValue(dimensionValueReq, user);
    }

    public void saveQueryResult(ChatExecuteReq chatExecuteReq, QueryResult queryResult) {
        //The history record only retains the query result of the first parse
        if (chatExecuteReq.getParseId() > 1) {
            return;
        }
        chatManageService.saveQueryResult(chatExecuteReq, queryResult);
    }

}
