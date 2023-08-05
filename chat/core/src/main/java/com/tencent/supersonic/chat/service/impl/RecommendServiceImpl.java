package com.tencent.supersonic.chat.service.impl;


import com.tencent.supersonic.chat.api.pojo.DomainSchema;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.RecommendQuestionResp;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigFilter;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.api.pojo.response.RecommendResp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.chat.service.RecommendService;
import com.tencent.supersonic.chat.service.SemanticService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/***
 * Recommend Service impl
 */
@Service
@Slf4j
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private ConfigService configService;
    @Autowired
    private SemanticService semanticService;

    @Override
    public RecommendResp recommend(QueryReq queryCtx, Long limit) {
        if (Objects.isNull(limit) || limit <= 0) {
            limit = Long.MAX_VALUE;
        }
        log.debug("limit:{}", limit);
        Long domainId = queryCtx.getDomainId();
        if (Objects.isNull(domainId)) {
            return new RecommendResp();
        }

        DomainSchema domainSchema = semanticService.getDomainSchema(domainId);

        List<SchemaElement> dimensions = domainSchema.getDimensions().stream()
                .filter(dim -> Objects.nonNull(dim) && Objects.nonNull(dim.getUseCnt()))
                .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                .limit(limit)
                .map(dimSchemaDesc -> {
                    SchemaElement item = new SchemaElement();
                    item.setDomain(domainId);
                    item.setName(dimSchemaDesc.getName());
                    item.setBizName(dimSchemaDesc.getBizName());
                    item.setId(dimSchemaDesc.getId());
                    return item;
                }).collect(Collectors.toList());

        List<SchemaElement> metrics = domainSchema.getMetrics().stream()
                .filter(metric -> Objects.nonNull(metric) && Objects.nonNull(metric.getUseCnt()))
                .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                .limit(limit)
                .map(metricSchemaDesc -> {
                    SchemaElement item = new SchemaElement();
                    item.setDomain(domainId);
                    item.setName(metricSchemaDesc.getName());
                    item.setBizName(metricSchemaDesc.getBizName());
                    item.setId(metricSchemaDesc.getId());
                    return item;
                }).collect(Collectors.toList());

        RecommendResp response = new RecommendResp();
        response.setDimensions(dimensions);
        response.setMetrics(metrics);
        return response;
    }

    @Override
    public RecommendResp recommendMetricMode(QueryReq queryCtx, Long limit) {
        RecommendResp recommendResponse = recommend(queryCtx, limit);
        // filter black Item
        if (Objects.isNull(recommendResponse)) {
            return recommendResponse;
        }

        ChatConfigRichResp chatConfigRich = configService.getConfigRichInfo(Long.valueOf(queryCtx.getDomainId()));
        if (Objects.nonNull(chatConfigRich) && Objects.nonNull(chatConfigRich.getChatAggRichConfig())
                && Objects.nonNull(chatConfigRich.getChatAggRichConfig().getVisibility())) {
            List<Long> blackMetricIdList = chatConfigRich.getChatAggRichConfig().getVisibility().getBlackMetricIdList();
            List<SchemaElement> metrics = filterBlackItem(recommendResponse.getMetrics(), blackMetricIdList);
            recommendResponse.setMetrics(metrics);

            List<Long> blackDimIdList = chatConfigRich.getChatAggRichConfig().getVisibility().getBlackDimIdList();
            List<SchemaElement> dimensions = filterBlackItem(recommendResponse.getDimensions(), blackDimIdList);
            recommendResponse.setDimensions(dimensions);
        }

        return recommendResponse;
    }

    @Override
    public List<RecommendQuestionResp> recommendQuestion(Long domainId) {
        List<RecommendQuestionResp> recommendQuestions = new ArrayList<>();
        ChatConfigFilter chatConfigFilter = new ChatConfigFilter();
        chatConfigFilter.setDomainId(domainId);
        List<ChatConfigResp> chatConfigRespList = configService.search(chatConfigFilter, null);
        if (!CollectionUtils.isEmpty(chatConfigRespList)) {
            chatConfigRespList.stream().forEach(chatConfigResp -> {
                if (Objects.nonNull(chatConfigResp) && !CollectionUtils.isEmpty(chatConfigResp.getRecommendedQuestions())) {
                    recommendQuestions.add(new RecommendQuestionResp(chatConfigResp.getDomainId(), chatConfigResp.getRecommendedQuestions()));
                }
            });
            return recommendQuestions;
        }
        return new ArrayList<>();
    }

    private List<SchemaElement> filterBlackItem(List<SchemaElement> itemList, List<Long> blackDimIdList) {
        if (CollectionUtils.isEmpty(blackDimIdList) || CollectionUtils.isEmpty(itemList)) {
            return itemList;
        }

        return itemList.stream().filter(dim -> !blackDimIdList.contains(dim.getId())).collect(Collectors.toList());
    }
}
