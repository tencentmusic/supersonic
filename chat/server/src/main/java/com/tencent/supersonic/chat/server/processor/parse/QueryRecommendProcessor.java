package com.tencent.supersonic.chat.server.processor.parse;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.tencent.supersonic.chat.api.pojo.response.SimilarQueryRecallResp;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.server.persistence.repository.ChatQueryRepository;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.service.ExemplarService;
import com.tencent.supersonic.common.util.ContextUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * MetricRecommendProcessor fills recommended query based on embedding similarity.
 **/
@Slf4j
public class QueryRecommendProcessor implements ParseResultProcessor {

    @Override
    public boolean accept(ParseContext parseContext) {
        return true;
    }

    @Override
    public void process(ParseContext parseContext) {
        CompletableFuture.runAsync(() -> doProcess(parseContext));
    }

    @SneakyThrows
    private void doProcess(ParseContext parseContext) {
        Long queryId = parseContext.getResponse().getQueryId();
        List<SimilarQueryRecallResp> solvedQueries = getSimilarQueries(
                parseContext.getRequest().getQueryText(), parseContext.getAgent().getId());
        ChatQueryDO chatQueryDO = getChatQuery(queryId);
        chatQueryDO.setSimilarQueries(JSONObject.toJSONString(solvedQueries));
        updateChatQuery(chatQueryDO);
    }

    public List<SimilarQueryRecallResp> getSimilarQueries(String queryText, Integer agentId) {
        ExemplarService exemplarService = ContextUtils.getBean(ExemplarService.class);
        EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
        String memoryCollectionName = embeddingConfig.getMemoryCollectionName(agentId);
        List<Text2SQLExemplar> exemplars =
                exemplarService.recallExemplars(memoryCollectionName, queryText, 5);
        return exemplars.stream().map(sqlExemplar -> SimilarQueryRecallResp.builder()
                .queryText(sqlExemplar.getQuestion()).build()).collect(Collectors.toList());
    }

    private ChatQueryDO getChatQuery(Long queryId) {
        ChatQueryRepository chatQueryRepository = ContextUtils.getBean(ChatQueryRepository.class);
        return chatQueryRepository.getChatQueryDO(queryId);
    }

    private void updateChatQuery(ChatQueryDO chatQueryDO) {
        ChatQueryRepository chatQueryRepository = ContextUtils.getBean(ChatQueryRepository.class);
        UpdateWrapper<ChatQueryDO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().eq(ChatQueryDO::getQuestionId, chatQueryDO.getQuestionId());
        chatQueryRepository.updateChatQuery(chatQueryDO, updateWrapper);
    }
}
