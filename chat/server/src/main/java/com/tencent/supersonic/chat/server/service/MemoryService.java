package com.tencent.supersonic.chat.server.service;

import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO;

import java.util.List;

public interface MemoryService {
    void createMemory(ChatMemoryDO memory);

    void updateMemory(ChatMemoryDO memory);

    List<ChatMemoryDO> getMemories();
}
