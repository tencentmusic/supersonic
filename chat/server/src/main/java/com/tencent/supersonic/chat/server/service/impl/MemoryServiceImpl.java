package com.tencent.supersonic.chat.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryReviewResult;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import com.tencent.supersonic.chat.api.pojo.request.ChatMemoryFilter;
import com.tencent.supersonic.chat.api.pojo.request.ChatMemoryUpdateReq;
import com.tencent.supersonic.chat.api.pojo.request.PageMemoryReq;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO;
import com.tencent.supersonic.chat.server.persistence.mapper.ChatMemoryMapper;
import com.tencent.supersonic.chat.server.persistence.repository.ChatMemoryRepository;
import com.tencent.supersonic.chat.server.pojo.ChatMemory;
import com.tencent.supersonic.chat.server.service.MemoryService;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.service.ExemplarService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MemoryServiceImpl implements MemoryService {

    @Autowired
    private ChatMemoryRepository chatMemoryRepository;

    @Autowired
    private ChatMemoryMapper chatMemoryMapper;

    @Autowired
    private ExemplarService exemplarService;

    @Autowired
    private EmbeddingConfig embeddingConfig;

    @Override
    public void createMemory(ChatMemory memory) {
        // if an existing enabled memory has the same question, just skip
        List<ChatMemory> memories =
                getMemories(ChatMemoryFilter.builder().agentId(memory.getAgentId())
                        .question(memory.getQuestion()).status(MemoryStatus.ENABLED).build());
        if (memories.isEmpty()) {
            ChatMemoryDO memoryDO = getMemoryDO(memory);
            chatMemoryRepository.createMemory(memoryDO);
        }
    }

    @Override
    public void updateMemory(ChatMemoryUpdateReq chatMemoryUpdateReq, User user) {
        ChatMemoryDO chatMemoryDO = chatMemoryRepository.getMemory(chatMemoryUpdateReq.getId());
        boolean hadEnabled =
                MemoryStatus.ENABLED.toString().equals(chatMemoryDO.getStatus().trim());
        if (MemoryStatus.ENABLED.equals(chatMemoryUpdateReq.getStatus()) && !hadEnabled) {
            enableMemory(chatMemoryDO);
        } else if (MemoryStatus.DISABLED.equals(chatMemoryUpdateReq.getStatus()) && hadEnabled) {
            disableMemory(chatMemoryDO);
        }

        LambdaUpdateWrapper<ChatMemoryDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatMemoryDO::getId, chatMemoryDO.getId());
        if (Objects.nonNull(chatMemoryUpdateReq.getStatus())) {
            updateWrapper.set(ChatMemoryDO::getStatus, chatMemoryUpdateReq.getStatus());
        }
        if (Objects.nonNull(chatMemoryUpdateReq.getLlmReviewRet())) {
            updateWrapper.set(ChatMemoryDO::getLlmReviewRet,
                    chatMemoryUpdateReq.getLlmReviewRet().toString());
        }
        if (Objects.nonNull(chatMemoryUpdateReq.getLlmReviewCmt())) {
            updateWrapper.set(ChatMemoryDO::getLlmReviewCmt, chatMemoryUpdateReq.getLlmReviewCmt());
        }
        if (Objects.nonNull(chatMemoryUpdateReq.getHumanReviewRet())) {
            updateWrapper.set(ChatMemoryDO::getHumanReviewRet,
                    chatMemoryUpdateReq.getHumanReviewRet().toString());
        }
        if (Objects.nonNull(chatMemoryUpdateReq.getHumanReviewCmt())) {
            updateWrapper.set(ChatMemoryDO::getHumanReviewCmt,
                    chatMemoryUpdateReq.getHumanReviewCmt());
        }
        updateWrapper.set(ChatMemoryDO::getUpdatedAt, new Date());
        updateWrapper.set(ChatMemoryDO::getUpdatedBy, user.getName());

        chatMemoryMapper.update(updateWrapper);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        chatMemoryRepository.batchDelete(ids);
    }

    @Override
    public PageInfo<ChatMemory> pageMemories(PageMemoryReq pageMemoryReq) {
        ChatMemoryFilter chatMemoryFilter = pageMemoryReq.getChatMemoryFilter();
        chatMemoryFilter.setSort(pageMemoryReq.getSort());
        chatMemoryFilter.setOrderCondition(pageMemoryReq.getOrderCondition());
        return PageHelper.startPage(pageMemoryReq.getCurrent(), pageMemoryReq.getPageSize())
                .doSelectPageInfo(() -> getMemories(pageMemoryReq.getChatMemoryFilter()));
    }

    @Override
    public List<ChatMemory> getMemories(ChatMemoryFilter chatMemoryFilter) {
        QueryWrapper<ChatMemoryDO> queryWrapper = new QueryWrapper<>();
        if (chatMemoryFilter.getAgentId() != null) {
            queryWrapper.lambda().eq(ChatMemoryDO::getAgentId, chatMemoryFilter.getAgentId());
        }
        if (chatMemoryFilter.getQueryId() != null) {
            queryWrapper.lambda().eq(ChatMemoryDO::getQueryId, chatMemoryFilter.getQueryId());
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
            queryWrapper.lambda().eq(ChatMemoryDO::getHumanReviewRet,
                    chatMemoryFilter.getHumanReviewRet());
        }
        if (chatMemoryFilter.getLlmReviewRet() != null) {
            queryWrapper.lambda().eq(ChatMemoryDO::getLlmReviewRet,
                    chatMemoryFilter.getLlmReviewRet());
        }
        if (StringUtils.isBlank(chatMemoryFilter.getOrderCondition())) {
            queryWrapper.orderByDesc("id");
        } else {
            queryWrapper.orderBy(true, chatMemoryFilter.isAsc(),
                    chatMemoryFilter.getOrderCondition());
        }
        List<ChatMemoryDO> chatMemoryDOS = chatMemoryRepository.getMemories(queryWrapper);
        return chatMemoryDOS.stream().map(this::getMemory).collect(Collectors.toList());
    }

    public void enableMemory(ChatMemoryDO memory) {
        memory.setStatus(MemoryStatus.ENABLED.toString());
        exemplarService.storeExemplar(embeddingConfig.getMemoryCollectionName(memory.getAgentId()),
                Text2SQLExemplar.builder().question(memory.getQuestion())
                        .sideInfo(memory.getSideInfo()).dbSchema(memory.getDbSchema())
                        .sql(memory.getS2sql()).build());
    }

    public void disableMemory(ChatMemoryDO memory) {
        memory.setStatus(MemoryStatus.DISABLED.toString());
        exemplarService.removeExemplar(embeddingConfig.getMemoryCollectionName(memory.getAgentId()),
                Text2SQLExemplar.builder().question(memory.getQuestion())
                        .sideInfo(memory.getSideInfo()).dbSchema(memory.getDbSchema())
                        .sql(memory.getS2sql()).build());
    }

    private ChatMemoryDO getMemoryDO(ChatMemory memory) {
        ChatMemoryDO memoryDO = new ChatMemoryDO();
        BeanUtils.copyProperties(memory, memoryDO);
        memoryDO.setStatus(memory.getStatus().toString().trim());
        if (Objects.nonNull(memory.getHumanReviewRet())) {
            memoryDO.setHumanReviewRet(memory.getHumanReviewRet().toString().trim());
        }
        if (Objects.nonNull(memory.getLlmReviewRet())) {
            memoryDO.setLlmReviewRet(memory.getLlmReviewRet().toString().trim());
        }

        return memoryDO;
    }

    private ChatMemory getMemory(ChatMemoryDO memoryDO) {
        ChatMemory memory = new ChatMemory();
        BeanUtils.copyProperties(memoryDO, memory);
        memory.setStatus(MemoryStatus.valueOf(memoryDO.getStatus().trim()));
        if (Objects.nonNull(memoryDO.getHumanReviewRet())) {
            memory.setHumanReviewRet(
                    MemoryReviewResult.valueOf(memoryDO.getHumanReviewRet().trim()));
        }
        if (Objects.nonNull(memoryDO.getLlmReviewRet())) {
            memory.setLlmReviewRet(MemoryReviewResult.valueOf(memoryDO.getLlmReviewRet().trim()));
        }
        return memory;
    }

}
