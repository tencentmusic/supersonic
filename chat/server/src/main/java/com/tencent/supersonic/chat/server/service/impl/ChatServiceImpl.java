package com.tencent.supersonic.chat.server.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.api.pojo.response.ShowCaseResp;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.QueryDO;
import com.tencent.supersonic.chat.server.persistence.repository.ChatQueryRepository;
import com.tencent.supersonic.chat.server.persistence.repository.ChatRepository;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatService;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryDataReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;
import com.tencent.supersonic.headless.server.service.ChatQueryService;
import com.tencent.supersonic.headless.server.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private ChatQueryRepository chatQueryRepository;
    @Autowired
    private ChatQueryService chatQueryService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private SearchService searchService;

    @Override
    public List<SearchResult> search(ChatParseReq chatParseReq) {
        QueryReq queryReq = buildSqlQueryReq(chatParseReq);
        return searchService.search(queryReq);
    }

    @Override
    public ParseResp performParsing(ChatParseReq chatParseReq) {
        QueryReq queryReq = buildSqlQueryReq(chatParseReq);
        ParseResp parseResp = chatQueryService.performParsing(queryReq);
        batchAddParse(chatParseReq, parseResp);
        return parseResp;
    }

    @Override
    public QueryResult performExecution(ChatExecuteReq chatExecuteReq) throws Exception {
        ExecuteQueryReq executeQueryReq = buildExecuteReq(chatExecuteReq);
        QueryResult queryResult = chatQueryService.performExecution(executeQueryReq);
        saveQueryResult(chatExecuteReq, queryResult);
        return queryResult;
    }

    @Override
    public Object queryData(QueryDataReq queryData, User user) throws Exception {
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

    private QueryReq buildSqlQueryReq(ChatParseReq chatParseReq) {
        QueryReq queryReq = new QueryReq();
        BeanMapper.mapper(chatParseReq, queryReq);
        if (chatParseReq.getAgentId() == null) {
            return queryReq;
        }
        Agent agent = agentService.getAgent(chatParseReq.getAgentId());
        if (agent == null) {
            return queryReq;
        }
        if (agent.containsLLMParserTool()) {
            queryReq.setEnableLLM(true);
        }
        queryReq.setDataSetIds(agent.getDataSetIds());
        return queryReq;
    }

    private ExecuteQueryReq buildExecuteReq(ChatExecuteReq chatExecuteReq) {
        ChatParseDO chatParseDO = getParseInfo(chatExecuteReq.getQueryId(), chatExecuteReq.getParseId());
        SemanticParseInfo parseInfo = JSONObject.parseObject(chatParseDO.getParseInfo(), SemanticParseInfo.class);
        return ExecuteQueryReq.builder()
                .queryId(chatExecuteReq.getQueryId())
                .chatId(chatExecuteReq.getChatId())
                .queryText(chatExecuteReq.getQueryText())
                .parseInfo(parseInfo)
                .saveAnswer(chatExecuteReq.isSaveAnswer())
                .user(chatExecuteReq.getUser())
                .build();
    }

    @Override
    public Boolean addChat(User user, String chatName, Integer agentId) {
        ChatDO chatDO = new ChatDO();
        chatDO.setChatName(chatName);
        chatDO.setCreator(user.getName());
        chatDO.setCreateTime(getCurrentTime());
        chatDO.setIsDelete(0);
        chatDO.setLastTime(getCurrentTime());
        chatDO.setLastQuestion("Hello, welcome to using supersonic");
        chatDO.setIsTop(0);
        chatDO.setAgentId(agentId);
        return chatRepository.createChat(chatDO);
    }

    @Override
    public List<ChatDO> getAll(String userName, Integer agentId) {
        return chatRepository.getAll(userName, agentId);
    }

    @Override
    public boolean updateChatName(Long chatId, String chatName, String userName) {
        return chatRepository.updateChatName(chatId, chatName, getCurrentTime(), userName);
    }

    @Override
    public boolean updateFeedback(Integer id, Integer score, String feedback) {
        QueryDO intelligentQueryDO = new QueryDO();
        intelligentQueryDO.setId(id);
        intelligentQueryDO.setScore(score);
        intelligentQueryDO.setFeedback(feedback);
        return chatRepository.updateFeedback(intelligentQueryDO);
    }

    @Override
    public boolean updateChatIsTop(Long chatId, int isTop) {
        return chatRepository.updateConversionIsTop(chatId, isTop);
    }

    @Override
    public Boolean deleteChat(Long chatId, String userName) {
        return chatRepository.deleteChat(chatId, userName);
    }

    @Override
    public PageInfo<QueryResp> queryInfo(PageQueryInfoReq pageQueryInfoReq, long chatId) {
        PageInfo<QueryResp> queryRespPageInfo = chatQueryRepository.getChatQuery(pageQueryInfoReq, chatId);
        if (CollectionUtils.isEmpty(queryRespPageInfo.getList())) {
            return queryRespPageInfo;
        }
        fillParseInfo(queryRespPageInfo.getList());
        return queryRespPageInfo;
    }

    @Override
    public QueryResp getChatQuery(Long queryId) {
        return chatQueryRepository.getChatQuery(queryId);
    }

    @Override
    public ShowCaseResp queryShowCase(PageQueryInfoReq pageQueryInfoReq, int agentId) {
        ShowCaseResp showCaseResp = new ShowCaseResp();
        showCaseResp.setCurrent(pageQueryInfoReq.getCurrent());
        showCaseResp.setPageSize(pageQueryInfoReq.getPageSize());
        List<QueryResp> queryResps = chatQueryRepository.queryShowCase(pageQueryInfoReq, agentId);
        if (CollectionUtils.isEmpty(queryResps)) {
            return showCaseResp;
        }
        queryResps.removeIf(queryResp -> {
            if (queryResp.getQueryResult() == null) {
                return true;
            }
            if (queryResp.getQueryResult().getResponse() != null) {
                return false;
            }
            if (CollectionUtils.isEmpty(queryResp.getQueryResult().getQueryResults())) {
                return true;
            }
            Map<String, Object> data = queryResp.getQueryResult().getQueryResults().get(0);
            return CollectionUtils.isEmpty(data);
        });
        queryResps = new ArrayList<>(queryResps.stream()
                .collect(Collectors.toMap(QueryResp::getQueryText, Function.identity(),
                        (existing, replacement) -> existing, LinkedHashMap::new)).values());
        fillParseInfo(queryResps);
        Map<Long, List<QueryResp>> showCaseMap = queryResps.stream()
                .collect(Collectors.groupingBy(QueryResp::getChatId));
        showCaseResp.setShowCaseMap(showCaseMap);
        return showCaseResp;
    }

    private void fillParseInfo(List<QueryResp> queryResps) {
        List<Long> queryIds = queryResps.stream()
                .map(QueryResp::getQuestionId).collect(Collectors.toList());
        List<ChatParseDO> chatParseDOs = chatQueryRepository.getParseInfoList(queryIds);
        if (CollectionUtils.isEmpty(chatParseDOs)) {
            return;
        }
        Map<Long, List<ChatParseDO>> chatParseMap = chatParseDOs.stream()
                .collect(Collectors.groupingBy(ChatParseDO::getQuestionId));
        for (QueryResp queryResp : queryResps) {
            List<ChatParseDO> chatParseDOList = chatParseMap.get(queryResp.getQuestionId());
            if (CollectionUtils.isEmpty(chatParseDOList)) {
                continue;
            }
            List<SemanticParseInfo> parseInfos = chatParseDOList.stream().map(chatParseDO ->
                            JsonUtil.toObject(chatParseDO.getParseInfo(), SemanticParseInfo.class))
                    .sorted(Comparator.comparingDouble(SemanticParseInfo::getScore).reversed())
                    .collect(Collectors.toList());
            queryResp.setParseInfos(parseInfos);
        }
    }

    @Override
    public void saveQueryResult(ChatExecuteReq chatExecuteReq, QueryResult queryResult) {
        //The history record only retains the query result of the first parse
        if (chatExecuteReq.getParseId() > 1) {
            return;
        }
        ChatQueryDO chatQueryDO = new ChatQueryDO();
        chatQueryDO.setQuestionId(chatExecuteReq.getQueryId());
        chatQueryDO.setQueryResult(JsonUtil.toString(queryResult));
        chatQueryDO.setQueryState(1);
        updateQuery(chatQueryDO);
        chatRepository.updateLastQuestion(chatExecuteReq.getChatId().longValue(),
                chatExecuteReq.getQueryText(), getCurrentTime());
    }

    @Override
    public int updateQuery(ChatQueryDO chatQueryDO) {
        return chatQueryRepository.updateChatQuery(chatQueryDO);
    }

    @Override
    public List<ChatParseDO> batchAddParse(ChatParseReq chatParseReq, ParseResp parseResult) {
        List<SemanticParseInfo> candidateParses = parseResult.getSelectedParses();
        return chatQueryRepository.batchSaveParseInfo(chatParseReq, parseResult, candidateParses);
    }

    @Override
    public ChatQueryDO getLastQuery(long chatId) {
        return chatQueryRepository.getLastChatQuery(chatId);
    }

    private String getCurrentTime() {
        SimpleDateFormat tempDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return tempDate.format(new java.util.Date());
    }

    public ChatParseDO getParseInfo(Long questionId, int parseId) {
        return chatQueryRepository.getParseInfo(questionId, parseId);
    }

    public Boolean deleteChatQuery(Long questionId) {
        return chatQueryRepository.deleteChatQuery(questionId);
    }

}
