package com.tencent.supersonic.chat.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryReviewResult;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import com.tencent.supersonic.chat.api.pojo.request.ChatHistoryFilter;
import com.tencent.supersonic.chat.api.pojo.request.ChatHistoryUpdateReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatMemoryUpdateReq;
import com.tencent.supersonic.chat.api.pojo.request.PageHistoryReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatHistoryDO;
import com.tencent.supersonic.chat.server.persistence.mapper.ChatHistoryMapper;
import com.tencent.supersonic.chat.server.persistence.repository.ChatHistoryRepository;
import com.tencent.supersonic.chat.server.pojo.ChatHistory;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.chat.server.service.HistoryService;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.service.ExemplarService;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HistoryServiceImpl implements HistoryService {

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private ChatHistoryMapper chatHistoryMapper;

    @Override
    public void saveHistoryInfo(ParseContext parseContext) {
        Text2SQLExemplar exemplar = getExemplar(parseContext);
        if (parseContext.getResponse().getState() == ParseResp.ParseState.COMPLETED && exemplar != null) {
            createHistory(ChatHistory.builder().queryId(parseContext.getRequest().getQueryId())
                    .agentId(parseContext.getAgent().getId()).status(MemoryStatus.PENDING)
                    .question(exemplar.getQuestion()).sideInfo(exemplar.getSideInfo())
                    .dbSchema(exemplar.getDbSchema()).s2sql(exemplar.getSql())
                    .createdBy(parseContext.getRequest().getUser().getName())
                    .updatedBy(parseContext.getRequest().getUser().getName()).createdAt(new Date())
                    .build());
        } else {
            createHistory(ChatHistory.builder().queryId(parseContext.getRequest().getQueryId())
                    .agentId(parseContext.getAgent().getId()).status(MemoryStatus.PENDING)
                    .question(parseContext.getRequest().getQueryText())
                    .createdBy(parseContext.getRequest().getUser().getName())
                    .updatedBy(parseContext.getRequest().getUser().getName()).createdAt(new Date())
                    .build());
        }
        log.info("saveHistoryInfo");
    }

    public Text2SQLExemplar getExemplar(ParseContext parseContext) {
        Text2SQLExemplar exemplar = JsonUtil.toObject(
                JsonUtil.toString(parseContext.getResponse().getSelectedParses().get(0)
                        .getProperties().get(Text2SQLExemplar.PROPERTY_KEY)),
                Text2SQLExemplar.class);
        return exemplar;
    }

    @Override
    public void createHistory(ChatHistory history) {
        // if an existing enabled history has the same question, just skip
        List<ChatHistory> memories =
                getMemories(ChatHistoryFilter.builder().agentId(history.getAgentId())
                        .question(history.getQuestion()).status(MemoryStatus.ENABLED).build());
        if (memories.isEmpty()) {
            ChatHistoryDO historyDO = getHistoryDO(history);
            chatHistoryRepository.createHistory(historyDO);
        }
    }

    @Override
    public void updateHistory(ChatHistoryUpdateReq chatHistoryUpdateReq, User user) {
        ChatHistoryDO chatHistoryDO =
                chatHistoryRepository.getHistory(chatHistoryUpdateReq.getId());

        LambdaUpdateWrapper<ChatHistoryDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatHistoryDO::getId, chatHistoryDO.getId());
        if (Objects.nonNull(chatHistoryUpdateReq.getStatus())) {
            updateWrapper.set(ChatHistoryDO::getStatus, chatHistoryUpdateReq.getStatus());
        }
        if (Objects.nonNull(chatHistoryUpdateReq.getLlmReviewRet())) {
            updateWrapper.set(ChatHistoryDO::getLlmReviewRet,
                    chatHistoryUpdateReq.getLlmReviewRet().toString());
        }
        if (Objects.nonNull(chatHistoryUpdateReq.getLlmReviewCmt())) {
            updateWrapper.set(ChatHistoryDO::getLlmReviewCmt,
                    chatHistoryUpdateReq.getLlmReviewCmt());
        }
        if (Objects.nonNull(chatHistoryUpdateReq.getHumanReviewRet())) {
            updateWrapper.set(ChatHistoryDO::getHumanReviewRet,
                    chatHistoryUpdateReq.getHumanReviewRet().toString());
        }
        if (Objects.nonNull(chatHistoryUpdateReq.getHumanReviewCmt())) {
            updateWrapper.set(ChatHistoryDO::getHumanReviewCmt,
                    chatHistoryUpdateReq.getHumanReviewCmt());
        }
        updateWrapper.set(ChatHistoryDO::getUpdatedAt, new Date());
        updateWrapper.set(ChatHistoryDO::getUpdatedBy, user.getName());

        chatHistoryMapper.update(updateWrapper);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        chatHistoryRepository.batchDelete(ids);
    }

    @Override
    public PageInfo<ChatHistory> pageHistories(PageHistoryReq pageHistoryReq) {
        ChatHistoryFilter chatHistoryFilter = pageHistoryReq.getChatHistoryFilter();
        chatHistoryFilter.setSort(pageHistoryReq.getSort());
        chatHistoryFilter.setOrderCondition(pageHistoryReq.getOrderCondition());
        return PageHelper.startPage(pageHistoryReq.getCurrent(), pageHistoryReq.getPageSize())
                .doSelectPageInfo(() -> getMemories(pageHistoryReq.getChatHistoryFilter()));
    }

    @Override
    public List<ChatHistory> getMemories(ChatHistoryFilter chatHistoryFilter) {
        QueryWrapper<ChatHistoryDO> queryWrapper = new QueryWrapper<>();
        if (chatHistoryFilter.getAgentId() != null) {
            queryWrapper.lambda().eq(ChatHistoryDO::getAgentId, chatHistoryFilter.getAgentId());
        }
        if (chatHistoryFilter.getQueryId() != null) {
            queryWrapper.lambda().eq(ChatHistoryDO::getQueryId, chatHistoryFilter.getQueryId());
        }
        if (StringUtils.isNotBlank(chatHistoryFilter.getQuestion())) {
            queryWrapper.lambda().like(ChatHistoryDO::getQuestion, chatHistoryFilter.getQuestion());
        }
        if (!CollectionUtils.isEmpty(chatHistoryFilter.getQuestions())) {
            queryWrapper.lambda().in(ChatHistoryDO::getQuestion, chatHistoryFilter.getQuestions());
        }
        if (chatHistoryFilter.getStatus() != null) {
            queryWrapper.lambda().eq(ChatHistoryDO::getStatus, chatHistoryFilter.getStatus());
        }
        if (chatHistoryFilter.getHumanReviewRet() != null) {
            queryWrapper.lambda().eq(ChatHistoryDO::getHumanReviewRet,
                    chatHistoryFilter.getHumanReviewRet());
        }
        if (chatHistoryFilter.getLlmReviewRet() != null) {
            queryWrapper.lambda().eq(ChatHistoryDO::getLlmReviewRet,
                    chatHistoryFilter.getLlmReviewRet());
        }
        if (StringUtils.isBlank(chatHistoryFilter.getOrderCondition())) {
            queryWrapper.orderByDesc("id");
        } else {
            queryWrapper.orderBy(true, chatHistoryFilter.isAsc(),
                    chatHistoryFilter.getOrderCondition());
        }
        List<ChatHistoryDO> chatHistoryDOS = chatHistoryRepository.getHistories(queryWrapper);
        return chatHistoryDOS.stream().map(this::getHistory).collect(Collectors.toList());
    }

    @Override
    public void updateHistoryByQueryId(ChatMemoryUpdateReq chatMemoryUpdateReq, User user) {
        ChatHistoryFilter historyFilter =
                ChatHistoryFilter.builder().queryId(chatMemoryUpdateReq.getQueryId()).build();
        List<ChatHistory> histories = getMemories(historyFilter);
        ChatHistoryUpdateReq chatHistoryUpdateReq = new ChatHistoryUpdateReq();
        histories.forEach(h -> {
            BeanUtils.copyProperties(chatMemoryUpdateReq, chatHistoryUpdateReq);
            chatHistoryUpdateReq.setId(h.getId());
            updateHistory(chatHistoryUpdateReq, user);
        });
    }

    private ChatHistoryDO getHistoryDO(ChatHistory history) {
        ChatHistoryDO historyDO = new ChatHistoryDO();
        BeanUtils.copyProperties(history, historyDO);
        historyDO.setStatus(history.getStatus().toString().trim());
        if (Objects.nonNull(history.getHumanReviewRet())) {
            historyDO.setHumanReviewRet(history.getHumanReviewRet().toString().trim());
        }
        if (Objects.nonNull(history.getLlmReviewRet())) {
            historyDO.setLlmReviewRet(history.getLlmReviewRet().toString().trim());
        }

        return historyDO;
    }

    private ChatHistory getHistory(ChatHistoryDO historyDO) {
        ChatHistory history = new ChatHistory();
        BeanUtils.copyProperties(historyDO, history);
        history.setStatus(MemoryStatus.valueOf(historyDO.getStatus().trim()));
        if (Objects.nonNull(historyDO.getHumanReviewRet())) {
            history.setHumanReviewRet(
                    MemoryReviewResult.valueOf(historyDO.getHumanReviewRet().trim()));
        }
        if (Objects.nonNull(historyDO.getLlmReviewRet())) {
            history.setLlmReviewRet(MemoryReviewResult.valueOf(historyDO.getLlmReviewRet().trim()));
        }
        return history;
    }

}
