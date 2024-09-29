package com.tencent.supersonic.chat.server.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO;

import java.util.List;

public interface ChatMemoryRepository {
    void createMemory(ChatMemoryDO chatMemoryDO);

    void updateMemory(ChatMemoryDO chatMemoryDO);

    void batchDelete(List<Long> ids);

    ChatMemoryDO getMemory(Long id);

    List<ChatMemoryDO> getMemories(QueryWrapper<ChatMemoryDO> queryWrapper);
}
