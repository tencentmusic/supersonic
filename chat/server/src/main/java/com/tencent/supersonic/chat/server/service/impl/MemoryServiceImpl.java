package com.tencent.supersonic.chat.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import com.tencent.supersonic.chat.api.pojo.request.ChatMemoryFilter;
import com.tencent.supersonic.chat.api.pojo.request.ChatMemoryUpdateReq;
import com.tencent.supersonic.chat.api.pojo.request.PageMemoryReq;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO;
import com.tencent.supersonic.chat.server.persistence.repository.ChatMemoryRepository;
import com.tencent.supersonic.chat.server.service.MemoryService;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.SqlExemplar;
import com.tencent.supersonic.common.service.ExemplarService;
import com.tencent.supersonic.common.util.BeanMapper;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class MemoryServiceImpl implements MemoryService {

    @Autowired
    private ChatMemoryRepository chatMemoryRepository;

    @Autowired
    private ExemplarService exemplarService;

    @Autowired
    private EmbeddingConfig embeddingConfig;

    @Override
    public void createMemory(ChatMemoryDO memory) {
        chatMemoryRepository.createMemory(memory);
    }

    @Override
    public void updateMemory(ChatMemoryUpdateReq chatMemoryUpdateReq, User user) {
        chatMemoryUpdateReq.updatedBy(user.getName());
        ChatMemoryDO chatMemoryDO = chatMemoryRepository.getMemory(chatMemoryUpdateReq.getId());
        boolean hadEnabled = MemoryStatus.ENABLED.equals(chatMemoryDO.getStatus());
        BeanMapper.mapper(chatMemoryUpdateReq, chatMemoryDO);
        if (MemoryStatus.ENABLED.equals(chatMemoryUpdateReq.getStatus()) && !hadEnabled) {
            enableMemory(chatMemoryDO);
        } else if (MemoryStatus.DISABLED.equals(chatMemoryUpdateReq.getStatus()) && hadEnabled) {
            disableMemory(chatMemoryDO);
        }
        updateMemory(chatMemoryDO);
    }

    @Override
    public void updateMemory(ChatMemoryDO memory) {
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
        if (chatMemoryFilter.getAgentId() != null) {
            queryWrapper.lambda().eq(ChatMemoryDO::getAgentId, chatMemoryFilter.getAgentId());
        }
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
        exemplarService.storeExemplar(embeddingConfig.getMemoryCollectionName(memory.getAgentId()),
                SqlExemplar.builder()
                        .question(memory.getQuestion())
                        .dbSchema(memory.getDbSchema())
                        .sql(memory.getS2sql())
                        .build());
    }

    private void disableMemory(ChatMemoryDO memory) {
        exemplarService.removeExemplar(embeddingConfig.getMemoryCollectionName(memory.getAgentId()),
                SqlExemplar.builder()
                        .question(memory.getQuestion())
                        .dbSchema(memory.getDbSchema())
                        .sql(memory.getS2sql())
                        .build());
    }

}
