package com.tencent.supersonic.chat.service.impl;


import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.RelateSchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.request.RecommendReq;
import com.tencent.supersonic.chat.api.pojo.response.RecommendQuestionResp;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigFilter;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.api.pojo.response.RecommendResp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.chat.service.RecommendService;
import com.tencent.supersonic.chat.service.SemanticService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
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
    public RecommendResp recommend(RecommendReq recommendReq, Long limit) {
        if (Objects.isNull(limit) || limit <= 0) {
            limit = Long.MAX_VALUE;
        }
        log.debug("limit:{}", limit);
        Long modelId = recommendReq.getModelId();
        if (Objects.isNull(modelId)) {
            return new RecommendResp();
        }
        ModelSchema modelSchema = semanticService.getModelSchema(modelId);
        if (Objects.isNull(modelSchema)) {
            return new RecommendResp();
        }
        List<Long> drillDownDimensions = Lists.newArrayList();
        Set<SchemaElement> metricElements = modelSchema.getMetrics();
        if (recommendReq.getMetricId() != null && !CollectionUtils.isEmpty(metricElements)) {
            Optional<SchemaElement> metric = metricElements.stream().filter(schemaElement ->
                            recommendReq.getMetricId().equals(schemaElement.getId())
                                    && !CollectionUtils.isEmpty(schemaElement.getRelateSchemaElements()))
                    .findFirst();
            if (metric.isPresent()) {
                drillDownDimensions = metric.get().getRelateSchemaElements().stream()
                        .map(RelateSchemaElement::getDimensionId).collect(Collectors.toList());
            }
        }
        final List<Long> drillDownDimensionsFinal = drillDownDimensions;
        List<SchemaElement> dimensions = modelSchema.getDimensions().stream()
                .filter(dim -> {
                    if (Objects.isNull(dim)) {
                        return false;
                    }
                    if (!CollectionUtils.isEmpty(drillDownDimensionsFinal)) {
                        return drillDownDimensionsFinal.contains(dim.getId());
                    } else {
                        return Objects.nonNull(dim.getUseCnt());
                    }
                })
                .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                .limit(limit)
                .map(dimSchemaDesc -> {
                    SchemaElement item = new SchemaElement();
                    item.setModel(modelId);
                    item.setName(dimSchemaDesc.getName());
                    item.setBizName(dimSchemaDesc.getBizName());
                    item.setId(dimSchemaDesc.getId());
                    item.setAlias(dimSchemaDesc.getAlias());
                    return item;
                }).collect(Collectors.toList());

        List<SchemaElement> metrics = modelSchema.getMetrics().stream()
                .filter(metric -> Objects.nonNull(metric) && Objects.nonNull(metric.getUseCnt()))
                .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                .limit(limit)
                .map(metricSchemaDesc -> {
                    SchemaElement item = new SchemaElement();
                    item.setModel(modelId);
                    item.setName(metricSchemaDesc.getName());
                    item.setBizName(metricSchemaDesc.getBizName());
                    item.setId(metricSchemaDesc.getId());
                    item.setAlias(metricSchemaDesc.getAlias());
                    return item;
                }).collect(Collectors.toList());

        RecommendResp response = new RecommendResp();
        response.setDimensions(dimensions);
        response.setMetrics(metrics);
        return response;
    }

    @Override
    public RecommendResp recommendMetricMode(RecommendReq recommendReq, Long limit) {
        RecommendResp recommendResponse = recommend(recommendReq, limit);
        // filter black Item
        if (Objects.isNull(recommendResponse)) {
            return recommendResponse;
        }

        ChatConfigRichResp chatConfigRich = configService.getConfigRichInfo(recommendReq.getModelId());
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
    public List<RecommendQuestionResp> recommendQuestion(Long modelId) {
        List<RecommendQuestionResp> recommendQuestions = new ArrayList<>();
        ChatConfigFilter chatConfigFilter = new ChatConfigFilter();
        chatConfigFilter.setModelId(modelId);
        List<ChatConfigResp> chatConfigRespList = configService.search(chatConfigFilter, null);
        if (!CollectionUtils.isEmpty(chatConfigRespList)) {
            chatConfigRespList.stream().forEach(chatConfigResp -> {
                if (Objects.nonNull(chatConfigResp)
                        && !CollectionUtils.isEmpty(chatConfigResp.getRecommendedQuestions())) {
                    recommendQuestions.add(
                            new RecommendQuestionResp(chatConfigResp.getModelId(),
                                    chatConfigResp.getRecommendedQuestions()));
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
