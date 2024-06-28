package com.tencent.supersonic.chat.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import com.tencent.supersonic.chat.api.pojo.request.ChatMemoryFilter;
import com.tencent.supersonic.chat.api.pojo.request.PageMemoryReq;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO;
import com.tencent.supersonic.chat.server.persistence.repository.ChatMemoryRepository;
import com.tencent.supersonic.chat.server.service.MemoryService;
import com.tencent.supersonic.common.pojo.SqlExemplar;
import com.tencent.supersonic.common.service.ExemplarService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class MemoryServiceImpl implements MemoryService {

    @Autowired
    private ChatMemoryRepository chatMemoryRepository;

    @Autowired
    private ExemplarService exemplarService;

    @Override
    public void createMemory(ChatMemoryDO memory) {
        chatMemoryRepository.createMemory(memory);
    }

    @Override
    public void updateMemory(ChatMemoryDO memory) {
        if (MemoryStatus.ENABLED.equals(memory.getStatus())) {
            enableMemory(memory);
        } else if (MemoryStatus.DISABLED.equals(memory.getStatus())) {
            disableMemory(memory);
        }
        chatMemoryRepository.updateMemory(memory);
    }

    @Override
    public PageInfo<ChatMemoryDO> pageMemories(PageMemoryReq pageMemoryReq) {
        return PageHelper.startPage(pageMemoryReq.getCurrent(),
                        pageMemoryReq.getPageSize())
                .doSelectPageInfo(() -> getMemories(pageMemoryReq.getChatMemoryFilter()));
    }

    @Override
    public List<ChatMemoryDO> getMemories(ChatMemoryFilter chatMemoryFilter) {
        QueryWrapper<ChatMemoryDO> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(chatMemoryFilter.getQuestion())) {
            queryWrapper.lambda().like(ChatMemoryDO::getQuestion, chatMemoryFilter.getQuestion());
        }
        if (!CollectionUtils.isEmpty(chatMemoryFilter.getQuestions())) {
            queryWrapper.lambda().in(ChatMemoryDO::getQuestion, chatMemoryFilter.getQuestions());
        }
        if (chatMemoryFilter.getStatus() != null) {
            queryWrapper.lambda().eq(ChatMemoryDO::getStatus, chatMemoryFilter.getStatus());
        }
        if (chatMemoryFilter.getHumanReviewRet() != null) {
            queryWrapper.lambda().eq(ChatMemoryDO::getHumanReviewRet, chatMemoryFilter.getHumanReviewRet());
        }
        if (chatMemoryFilter.getLlmReviewRet() != null) {
            queryWrapper.lambda().eq(ChatMemoryDO::getLlmReviewRet, chatMemoryFilter.getLlmReviewRet());
        }
        return chatMemoryRepository.getMemories(queryWrapper);
    }

    @Override
    public List<ChatMemoryDO> getMemoriesForLlmReview() {
        QueryWrapper<ChatMemoryDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ChatMemoryDO::getStatus, MemoryStatus.PENDING)
                .isNull(ChatMemoryDO::getLlmReviewRet);
        return chatMemoryRepository.getMemories(queryWrapper);
    }

    private void enableMemory(ChatMemoryDO memory) {
        exemplarService.storeExemplar(memory.getAgentId().toString(),
                SqlExemplar.builder()
                        .question(memory.getQuestion())
                        .dbSchema(memory.getDbSchema())
                        .sql(memory.getS2sql())
                        .build());
    }

    private void disableMemory(ChatMemoryDO memory) {
        exemplarService.removeExemplar(memory.getAgentId().toString(),
                SqlExemplar.builder()
                        .question(memory.getQuestion())
                        .dbSchema(memory.getDbSchema())
                        .sql(memory.getS2sql())
                        .build());
    }

}
