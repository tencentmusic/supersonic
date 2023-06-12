package com.tencent.supersonic.chat.infrastructure.repository;

import com.tencent.supersonic.chat.domain.dataobject.ChatDO;
import com.tencent.supersonic.chat.domain.dataobject.QueryDO;
import com.tencent.supersonic.chat.domain.repository.ChatRepository;
import com.tencent.supersonic.chat.infrastructure.mapper.ChatMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
@Slf4j
public class ChatRepositoryImpl implements ChatRepository {

    private final ChatMapper chatMapper;

    public ChatRepositoryImpl(ChatMapper chatMapper) {
        this.chatMapper = chatMapper;
    }

    @Override
    public boolean createChat(ChatDO chatDO) {
        return chatMapper.createChat(chatDO);
    }

    @Override
    public List<ChatDO> getAll(String creator) {
        return chatMapper.getAll(creator);
    }


    @Override
    public Boolean updateChatName(Long chatId, String chatName, String lastTime, String creator) {
        return chatMapper.updateChatName(chatId, chatName, lastTime, creator);
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
    public Boolean deleteChat(Long chatId, String userName) {
        return chatMapper.deleteChat(chatId, userName);
    }

}
