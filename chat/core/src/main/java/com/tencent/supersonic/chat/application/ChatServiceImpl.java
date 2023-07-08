package com.tencent.supersonic.chat.application;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.response.QueryResultResp;
import com.tencent.supersonic.chat.domain.dataobject.ChatDO;
import com.tencent.supersonic.chat.domain.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.domain.dataobject.QueryDO;
import com.tencent.supersonic.chat.domain.pojo.chat.ChatQueryVO;
import com.tencent.supersonic.chat.domain.pojo.chat.PageQueryInfoReq;
import com.tencent.supersonic.chat.domain.repository.ChatContextRepository;
import com.tencent.supersonic.chat.domain.repository.ChatQueryRepository;
import com.tencent.supersonic.chat.domain.repository.ChatRepository;
import com.tencent.supersonic.chat.domain.service.ChatService;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;

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
    public Long getContextDomain(Integer chatId) {
        if (Objects.isNull(chatId)) {
            return null;
        }
        ChatContext chatContext = getOrCreateContext(chatId);
        if (Objects.isNull(chatContext)) {
            return null;
        }
        SemanticParseInfo originalSemanticParse = chatContext.getParseInfo();
        if (Objects.nonNull(originalSemanticParse) && Objects.nonNull(originalSemanticParse.getDomainId())) {
            return originalSemanticParse.getDomainId();
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
    public void updateContext(ChatContext chatCtx, QueryContextReq queryCtx, SemanticParseInfo semanticParseInfo) {
        chatCtx.setParseInfo(semanticParseInfo);
        chatCtx.setQueryText(queryCtx.getQueryText());
        updateContext(chatCtx);
    }

    @Override
    public void switchContext(ChatContext chatCtx) {
        log.debug("switchContext ChatContext {}", chatCtx);
        chatCtx.setParseInfo(new SemanticParseInfo());
    }


    @Override
    public Boolean addChat(User user, String chatName) {
        ChatDO intelligentConversionDO = new ChatDO();
        intelligentConversionDO.setChatName(chatName);
        intelligentConversionDO.setCreator(user.getName());
        intelligentConversionDO.setCreateTime(getCurrentTime());
        intelligentConversionDO.setIsDelete(0);
        intelligentConversionDO.setLastTime(getCurrentTime());
        intelligentConversionDO.setLastQuestion("Hello, welcome to using supersonic");
        intelligentConversionDO.setIsTop(0);
        return chatRepository.createChat(intelligentConversionDO);
    }

    @Override
    public List<ChatDO> getAll(String userName) {
        return chatRepository.getAll(userName);
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
    public PageInfo<ChatQueryVO> queryInfo(PageQueryInfoReq pageQueryInfoCommend, long chatId) {
        return chatQueryRepository.getChatQuery(pageQueryInfoCommend, chatId);
    }

    @Override
    public void addQuery(QueryResultResp queryResponse, QueryContextReq queryContext, ChatContext chatCtx) {
        chatQueryRepository.createChatQuery(queryResponse, queryContext, chatCtx);
        chatRepository.updateLastQuestion(chatCtx.getChatId().longValue(), queryContext.getQueryText(),
                getCurrentTime());
    }

    @Override
    public ChatQueryDO getLastQuery(long chatId) {
        return chatQueryRepository.getLastChatQuery(chatId);
    }

    @Override
    public int updateQuery(ChatQueryDO chatQueryDO) {
        return chatQueryRepository.updateChatQuery(chatQueryDO);
    }

    private String getCurrentTime() {
        SimpleDateFormat tempDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return tempDate.format(new java.util.Date());
    }

}
