package com.tencent.supersonic.chat.server.persistence.repository.impl;

import com.tencent.supersonic.chat.server.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.QueryDO;
import com.tencent.supersonic.chat.server.persistence.mapper.ChatMapper;
import com.tencent.supersonic.chat.server.persistence.repository.ChatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
@Slf4j
public class ChatRepositoryImpl implements ChatRepository {

    private final ChatMapper chatMapper;

    public ChatRepositoryImpl(ChatMapper chatMapper) {
        this.chatMapper = chatMapper;
    }

    @Override
    public Long createChat(ChatDO chatDO) {
        chatMapper.createChat(chatDO);
        return chatDO.getChatId();
    }

    @Override
    public List<ChatDO> getAll(String creator, Integer agentId, Long tenantId, String chatName) {
        return chatMapper.getAll(creator, agentId, tenantId, chatName);
    }

    @Override
    public Boolean updateChatName(Long chatId, String chatName, String lastTime, String creator,
            Long tenantId) {
        return chatMapper.updateChatName(chatId, chatName, lastTime, creator, tenantId);
    }

    @Override
    public Boolean updateLastQuestion(Long chatId, String lastQuestion, String lastTime) {
        return chatMapper.updateLastQuestion(chatId, lastQuestion, lastTime);
    }

    @Override
    public Boolean updateConversionIsTop(Long chatId, int isTop) {
        return chatMapper.updateConversionIsTop(chatId, isTop);
    }

    @Override
    public boolean updateFeedback(QueryDO queryDO) {
        return chatMapper.updateFeedback(queryDO);
    }

    @Override
    public Boolean deleteChat(Long chatId, String userName, Long tenantId) {
        return chatMapper.deleteChat(chatId, userName, tenantId);
    }
}
