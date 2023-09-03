package com.tencent.supersonic.chat.service.impl;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.ShowCaseResp;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.tencent.supersonic.chat.service.ChatService;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service("ChatService")
@Primary
@Slf4j
public class ChatServiceImpl implements ChatService {

    private ChatContextRepository chatContextRepository;
    private ChatRepository chatRepository;
    private ChatQueryRepository chatQueryRepository;

    public ChatServiceImpl(ChatContextRepository chatContextRepository, ChatRepository chatRepository,
                           ChatQueryRepository chatQueryRepository) {
        this.chatContextRepository = chatContextRepository;
        this.chatRepository = chatRepository;
        this.chatQueryRepository = chatQueryRepository;
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
    public PageInfo<QueryResp> queryInfo(PageQueryInfoReq pageQueryInfoCommend, long chatId) {
        return chatQueryRepository.getChatQuery(pageQueryInfoCommend, chatId);
    }

    @Override
    public ShowCaseResp queryShowCase(PageQueryInfoReq pageQueryInfoCommend, int agentId) {
        ShowCaseResp showCaseResp = new ShowCaseResp();
        List<QueryResp> queryResps = chatQueryRepository.queryShowCase(pageQueryInfoCommend, agentId);
        Map<Long, List<QueryResp>> showCaseMap = queryResps.stream()
                .collect(Collectors.groupingBy(QueryResp::getChatId));
        showCaseResp.setShowCaseMap(showCaseMap);
        showCaseResp.setCurrent(pageQueryInfoCommend.getCurrent());
        showCaseResp.setPageSize(pageQueryInfoCommend.getPageSize());
        return showCaseResp;
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
    public void batchAddParse(ChatContext chatCtx, QueryReq queryReq,
                              ParseResp parseResult,
                              List<SemanticParseInfo> candidateParses,
                              List<SemanticParseInfo> selectedParses) {
        chatQueryRepository.batchSaveParseInfo(chatCtx, queryReq, parseResult, candidateParses, selectedParses);

    }

    @Override
    public ChatQueryDO getLastQuery(long chatId) {
        return chatQueryRepository.getLastChatQuery(chatId);
    }

    private String getCurrentTime() {
        SimpleDateFormat tempDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return tempDate.format(new java.util.Date());
    }

    public ChatParseDO getParseInfo(Long questionId, String userName, int parseId) {
        return chatQueryRepository.getParseInfo(questionId, userName, parseId);
    }

    public Boolean deleteChatQuery(Long questionId) {
        return chatQueryRepository.deleteChatQuery(questionId);
    }

}
