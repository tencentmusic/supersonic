package com.tencent.supersonic.chat.utils;

import static com.tencent.supersonic.common.pojo.Constants.DAY;
import static com.tencent.supersonic.common.pojo.Constants.UNDERLINE;

import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.request.KnowledgeAdvancedConfig;
import com.tencent.supersonic.chat.api.pojo.request.KnowledgeInfoReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatDefaultRichConfigResp;
import com.tencent.supersonic.chat.config.Dim4Dict;
import com.tencent.supersonic.chat.config.DefaultMetric;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.chat.persistence.dataobject.DimValueDO;
import com.tencent.supersonic.knowledge.dictionary.DictUpdateMode;
import com.tencent.supersonic.knowledge.dictionary.DimValue2DictCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class DictMetaHelper {

    @Autowired
    private ConfigService configService;
    @Value("${model.internal.metric.suffix:internal_cnt}")
    private String internalMetricNameSuffix;
    @Value("${model.internal.day.number:2}")
    private Integer internalMetricDays;
    private SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();

    public List<DimValueDO> generateDimValueInfo(DimValue2DictCommand dimValue2DictCommend) {
        List<DimValueDO> dimValueDOList = new ArrayList<>();
        DictUpdateMode updateMode = dimValue2DictCommend.getUpdateMode();
        Set<Long> modelIds = new HashSet<>();
        switch (updateMode) {
            case OFFLINE_MODEL:
                modelIds.addAll(dimValue2DictCommend.getModelIds());
                dimValueDOList = generateDimValueInfoByModel(modelIds);
                break;
            case OFFLINE_FULL:
                List<ModelSchema> modelSchemaDescList = semanticLayer.getModelSchema();
                if (CollectionUtils.isEmpty(modelSchemaDescList)) {
                    break;
                }

                Map<Long, ModelSchema> modelIdAndDescPair = modelSchemaDescList.stream()
                        .collect(Collectors.toMap(a -> a.getModel().getId(), schema -> schema, (k1, k2) -> k1));
                if (!CollectionUtils.isEmpty(modelIdAndDescPair)) {
                    modelIds.addAll(modelIdAndDescPair.keySet());
                    dimValueDOList = generateDimValueInfoByModel(modelIds);
                    break;
                }
                break;
            case REALTIME_ADD:
                dimValueDOList = generateDimValueInfoByModelAndDim(dimValue2DictCommend.getModelAndDimPair());
                break;
            case NOT_SUPPORT:
                throw new RuntimeException("illegal parameter for updateMode");
            default:
                break;
        }

        return dimValueDOList;
    }

    private List<DimValueDO> generateDimValueInfoByModelAndDim(Map<Long, List<Long>> modelAndDimMap) {
        List<DimValueDO> dimValueDOList = new ArrayList<>();
        if (CollectionUtils.isEmpty(modelAndDimMap)) {
            return dimValueDOList;
        }

        List<ModelSchema> modelSchemaDescList = semanticLayer.getModelSchema();
        if (CollectionUtils.isEmpty(modelSchemaDescList)) {
            return dimValueDOList;
        }
        Map<Long, ModelSchema> modelIdAndDescPair = modelSchemaDescList.stream()
                .collect(Collectors.toMap(a -> a.getModel().getId(), a -> a, (k1, k2) -> k1));

        for (Long modelId : modelAndDimMap.keySet()) {
            if (!modelIdAndDescPair.containsKey(modelId)) {
                continue;
            }
            Map<Long, SchemaElement> dimIdAndDescPairAll;
            dimIdAndDescPairAll = modelIdAndDescPair.get(modelId).getDimensions().stream()
                    .collect(Collectors.toMap(SchemaElement::getId, dimSchemaDesc -> dimSchemaDesc, (k1, k2) -> k1));
            List<Long> dimIdReq = modelAndDimMap.get(modelId);
            Map<Long, SchemaElement> dimIdAndDescPairReq = new HashMap<>();
            for (Long dimId : dimIdReq) {
                if (dimIdAndDescPairAll.containsKey(dimId)) {
                    dimIdAndDescPairReq.put(dimId, dimIdAndDescPairAll.get(dimId));
                }
            }
            fillDimValueDOList(dimValueDOList, modelId, dimIdAndDescPairReq);
        }

        return dimValueDOList;
    }

    private List<DimValueDO> generateDimValueInfoByModel(Set<Long> modelIds) {
        List<DimValueDO> dimValueDOList = new ArrayList<>();
        List<ModelSchema> modelSchemaDescList = semanticLayer.getModelSchema(new ArrayList<>(modelIds));
        if (CollectionUtils.isEmpty(modelSchemaDescList)) {
            return dimValueDOList;
        }

        modelSchemaDescList.forEach(modelSchemaDesc -> {
            Map<Long, SchemaElement> dimIdAndDescPair = modelSchemaDesc.getDimensions().stream()
                    .collect(Collectors.toMap(SchemaElement::getId, dimSchemaDesc -> dimSchemaDesc, (k1, k2) -> k1));
            fillDimValueDOList(dimValueDOList, modelSchemaDesc.getModel().getId(), dimIdAndDescPair);

        });

        return dimValueDOList;
    }

    private void fillDimValueDOList(List<DimValueDO> dimValueDOList, Long modelId,
                                    Map<Long, SchemaElement> dimIdAndDescPair) {
        ChatConfigRichResp chaConfigRichDesc = configService.getConfigRichInfo(modelId);
        if (Objects.nonNull(chaConfigRichDesc) && Objects.nonNull(chaConfigRichDesc.getChatAggRichConfig())) {

            ChatDefaultRichConfigResp chatDefaultConfig =
                    chaConfigRichDesc.getChatAggRichConfig().getChatDefaultConfig();

            KnowledgeAdvancedConfig globalKnowledgeConfigAgg = chaConfigRichDesc.getChatAggRichConfig()
                    .getGlobalKnowledgeConfig();
            List<KnowledgeInfoReq> knowledgeAggInfo =
                    chaConfigRichDesc.getChatAggRichConfig().getKnowledgeInfos();

            KnowledgeAdvancedConfig globalKnowledgeConfigDetail = chaConfigRichDesc.getChatDetailRichConfig()
                    .getGlobalKnowledgeConfig();
            List<KnowledgeInfoReq> knowledgeDetailInfo =
                    chaConfigRichDesc.getChatDetailRichConfig().getKnowledgeInfos();

            fillKnowledgeDimValue(knowledgeDetailInfo, chatDefaultConfig, dimValueDOList, dimIdAndDescPair,
                    modelId, globalKnowledgeConfigDetail);
            fillKnowledgeDimValue(knowledgeAggInfo, chatDefaultConfig, dimValueDOList, dimIdAndDescPair,
                    modelId, globalKnowledgeConfigAgg);


        }
    }

    private void fillKnowledgeDimValue(List<KnowledgeInfoReq> knowledgeInfos,
                                       ChatDefaultRichConfigResp chatDefaultConfig,
                                       List<DimValueDO> dimValueDOList,
                                       Map<Long, SchemaElement> dimIdAndDescPair, Long modelId,
                                       KnowledgeAdvancedConfig globalKnowledgeConfigDetail) {
        if (!CollectionUtils.isEmpty(knowledgeInfos)) {
            List<Dim4Dict> dimensions = new ArrayList<>();
            List<DefaultMetric> defaultMetricDescList = new ArrayList<>();
            knowledgeInfos.stream()
                    .filter(knowledgeInfo -> knowledgeInfo.getSearchEnable()
                            && !CollectionUtils.isEmpty(dimIdAndDescPair)
                            && dimIdAndDescPair.containsKey(knowledgeInfo.getItemId()))
                    .forEach(knowledgeInfo -> {
                        SchemaElement dimensionDesc = dimIdAndDescPair.get(knowledgeInfo.getItemId());

                        //default cnt
                        if (Objects.isNull(chatDefaultConfig)
                                || CollectionUtils.isEmpty(chatDefaultConfig.getMetrics())) {
                            String datasourceBizName = dimensionDesc.getBizName();
                            if (Strings.isNotEmpty(datasourceBizName)) {
                                String internalMetricName =
                                        datasourceBizName + UNDERLINE + internalMetricNameSuffix;
                                defaultMetricDescList.add(new DefaultMetric(internalMetricName,
                                        internalMetricDays, DAY));
                            }
                        } else {
                            SchemaElement schemaItem = chatDefaultConfig.getMetrics().get(0);
                            defaultMetricDescList.add(new DefaultMetric(schemaItem.getBizName(),
                                    chatDefaultConfig.getUnit(), chatDefaultConfig.getPeriod()));

                        }

                        String bizName = dimensionDesc.getBizName();
                        Dim4Dict dim4Dict = new Dim4Dict();
                        dim4Dict.setDimId(knowledgeInfo.getItemId());
                        dim4Dict.setBizName(bizName);
                        if (Objects.nonNull(knowledgeInfo.getKnowledgeAdvancedConfig())) {
                            KnowledgeAdvancedConfig knowledgeAdvancedConfig
                                    = knowledgeInfo.getKnowledgeAdvancedConfig();
                            BeanUtils.copyProperties(knowledgeAdvancedConfig, dim4Dict);

                            if (Objects.nonNull(globalKnowledgeConfigDetail)
                                    && !CollectionUtils.isEmpty(globalKnowledgeConfigDetail.getRuleList())) {
                                dim4Dict.getRuleList().addAll(globalKnowledgeConfigDetail.getRuleList());
                            }
                        }
                        dimensions.add(dim4Dict);

                    });

            if (!CollectionUtils.isEmpty(dimensions)) {
                DimValueDO dimValueDO = new DimValueDO()
                        .setModelId(modelId)
                        .setDefaultMetricIds(defaultMetricDescList)
                        .setDimensions(dimensions);
                dimValueDOList.add(dimValueDO);
            }
        }
    }
}
