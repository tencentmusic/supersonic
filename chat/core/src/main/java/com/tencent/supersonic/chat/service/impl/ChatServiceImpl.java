package com.tencent.supersonic.chat.service.impl;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.ShowCaseResp;
import com.tencent.supersonic.chat.api.pojo.response.SolvedQueryRecallResp;
import com.tencent.supersonic.chat.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.persistence.dataobject.QueryDO;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.persistence.repository.ChatContextRepository;
import com.tencent.supersonic.chat.persistence.repository.ChatQueryRepository;
import com.tencent.supersonic.chat.persistence.repository.ChatRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.tencent.supersonic.chat.service.ChatService;
import com.tencent.supersonic.chat.utils.SolvedQueryManager;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service("ChatService")
@Primary
@Slf4j
public class ChatServiceImpl implements ChatService {

    private ChatContextRepository chatContextRepository;
    private ChatRepository chatRepository;
    private ChatQueryRepository chatQueryRepository;
    private SolvedQueryManager solvedQueryManager;

    public ChatServiceImpl(ChatContextRepository chatContextRepository, ChatRepository chatRepository,
            ChatQueryRepository chatQueryRepository, SolvedQueryManager solvedQueryManager) {
        this.chatContextRepository = chatContextRepository;
        this.chatRepository = chatRepository;
        this.chatQueryRepository = chatQueryRepository;
        this.solvedQueryManager = solvedQueryManager;
    }

    @Override
    public Long getContextModel(Integer chatId) {
        if (Objects.isNull(chatId)) {
            return null;
        }
        ChatContext chatContext = getOrCreateContext(chatId);
        if (Objects.isNull(chatContext)) {
            return null;
        }
        SemanticParseInfo originalSemanticParse = chatContext.getParseInfo();
        if (Objects.nonNull(originalSemanticParse) && Objects.nonNull(originalSemanticParse.getModelId())) {
            return originalSemanticParse.getModelId();
        }
        return null;
    }


    @Override
    public ChatContext getOrCreateContext(int chatId) {
        return chatContextRepository.getOrCreateContext(chatId);
    }

    @Override
    public void updateContext(ChatContext chatCtx) {
        log.debug("save ChatContext {}", chatCtx);
        chatContextRepository.updateContext(chatCtx);
    }

    @Override
    public void switchContext(ChatContext chatCtx) {
        log.debug("switchContext ChatContext {}", chatCtx);
        chatCtx.setParseInfo(new SemanticParseInfo());
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
    public void addQuery(QueryResult queryResult, ChatContext chatCtx) {
        chatQueryRepository.createChatQuery(queryResult, chatCtx);
        chatRepository.updateLastQuestion(chatCtx.getChatId().longValue(),
                chatCtx.getQueryText(), getCurrentTime());
    }

    @Override
    public Boolean updateQuery(Long questionId, QueryResult queryResult, ChatContext chatCtx) {
        ChatQueryDO chatQueryDO = new ChatQueryDO();
        chatQueryDO.setQuestionId(questionId);
        chatQueryDO.setQueryResult(JsonUtil.toString(queryResult));
        chatQueryDO.setQueryState(1);
        updateQuery(chatQueryDO);
        chatRepository.updateLastQuestion(chatCtx.getChatId().longValue(),
                chatCtx.getQueryText(), getCurrentTime());
        return true;
    }

    @Override
    public int updateQuery(ChatQueryDO chatQueryDO) {
        return chatQueryRepository.updateChatQuery(chatQueryDO);
    }

    @Override
    public List<ChatParseDO> batchAddParse(ChatContext chatCtx, QueryReq queryReq,
            ParseResp parseResult,
            List<SemanticParseInfo> candidateParses,
            List<SemanticParseInfo> selectedParses) {
        return chatQueryRepository.batchSaveParseInfo(chatCtx, queryReq, parseResult, candidateParses, selectedParses);
    }

    @Override
    public void updateChatParse(List<ChatParseDO> chatParseDOS) {
        chatQueryRepository.updateChatParseInfo(chatParseDOS);
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

    @Override
    public List<SolvedQueryRecallResp> getSolvedQuery(String queryText, Integer agentId) {
        //1. recall solved query by queryText
        List<SolvedQueryRecallResp> solvedQueryRecallResps = solvedQueryManager.recallSolvedQuery(queryText, agentId);
        if (CollectionUtils.isEmpty(solvedQueryRecallResps)) {
            return Lists.newArrayList();
        }
        List<Long> queryIds = solvedQueryRecallResps.stream()
                .map(SolvedQueryRecallResp::getQueryId).collect(Collectors.toList());
        PageQueryInfoReq pageQueryInfoReq = new PageQueryInfoReq();
        pageQueryInfoReq.setIds(queryIds);
        pageQueryInfoReq.setPageSize(100);
        pageQueryInfoReq.setCurrent(1);
        //2. remove low score query
        int lowScoreThreshold = 3;
        PageInfo<QueryResp> queryRespPageInfo = chatQueryRepository.getChatQuery(pageQueryInfoReq, null);
        List<QueryResp> queryResps = queryRespPageInfo.getList();
        if (CollectionUtils.isEmpty(queryResps)) {
            return Lists.newArrayList();
        }
        Set<Long> lowScoreQueryIds = queryResps.stream().filter(queryResp ->
                        queryResp.getScore() != null && queryResp.getScore() <= lowScoreThreshold)
                .map(QueryResp::getQuestionId).collect(Collectors.toSet());
        return solvedQueryRecallResps.stream().filter(solvedQueryRecallResp ->
                        !lowScoreQueryIds.contains(solvedQueryRecallResp.getQueryId()))
                .collect(Collectors.toList());
    }

}
