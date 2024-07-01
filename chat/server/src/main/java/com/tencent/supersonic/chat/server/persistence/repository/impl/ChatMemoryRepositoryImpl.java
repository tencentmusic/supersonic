package com.tencent.supersonic.chat.server.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO;
import com.tencent.supersonic.chat.server.persistence.mapper.ChatMemoryMapper;
import com.tencent.supersonic.chat.server.persistence.repository.ChatMemoryRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
public class ChatMemoryRepositoryImpl implements ChatMemoryRepository {

    private final ChatMemoryMapper chatMemoryMapper;

    public ChatMemoryRepositoryImpl(ChatMemoryMapper chatMemoryMapper) {
        this.chatMemoryMapper = chatMemoryMapper;
    }

    @Override
    public void createMemory(ChatMemoryDO chatMemoryDO) {
        chatMemoryMapper.insert(chatMemoryDO);
    }

    @Override
    public void updateMemory(ChatMemoryDO chatMemoryDO) {
        chatMemoryMapper.updateById(chatMemoryDO);
    }

    @Override
    public ChatMemoryDO getMemory(Long id) {
        return chatMemoryMapper.selectById(id);
    }

    @Override
    public List<ChatMemoryDO> getMemories(QueryWrapper<ChatMemoryDO> queryWrapper) {
        return chatMemoryMapper.selectList(queryWrapper);
    }

}
