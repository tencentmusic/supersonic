package com.tencent.supersonic.chat.server.service.impl;

import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO.ReviewResult;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO.Status;
import com.tencent.supersonic.chat.server.persistence.repository.ChatMemoryRepository;
import com.tencent.supersonic.chat.server.service.MemoryService;
import com.tencent.supersonic.common.pojo.SqlExemplar;
import com.tencent.supersonic.common.service.ExemplarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemoryServiceImpl implements MemoryService {

    @Autowired
    private ChatMemoryRepository chatMemoryRepository;

    @Autowired
    private ExemplarService exemplarService;

    @Override
    public void createMemory(ChatMemoryDO memory) {
        if (ReviewResult.POSITIVE.equals(memory.getHumanReviewRet())) {
            enableMemory(memory);
        }
        chatMemoryRepository.createMemory(memory);
    }

    @Override
    public void updateMemory(ChatMemoryDO memory) {
        if (!ChatMemoryDO.Status.ENABLED.equals(memory.getStatus())
                && ReviewResult.POSITIVE.equals(memory.getHumanReviewRet())) {
            enableMemory(memory);
        }
        chatMemoryRepository.updateMemory(memory);
    }

    @Override
    public List<ChatMemoryDO> getMemories() {
        return chatMemoryRepository.getMemories();
    }

    private void enableMemory(ChatMemoryDO memory) {
        exemplarService.storeExemplar(memory.getAgentId().toString(),
                SqlExemplar.builder()
                        .question(memory.getQuestion())
                        .dbSchema(memory.getDbSchema())
                        .sql(memory.getS2sql())
                        .build());
        memory.setStatus(Status.ENABLED);
    }
}
