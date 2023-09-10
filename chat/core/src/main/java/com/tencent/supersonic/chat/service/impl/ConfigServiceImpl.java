package com.tencent.supersonic.chat.service.impl;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.request.ItemVisibility;
import com.tencent.supersonic.chat.api.pojo.request.ItemNameVisibilityInfo;
import com.tencent.supersonic.chat.api.pojo.request.ChatAggConfigReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatDetailConfigReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigBaseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigFilter;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigEditReqReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatDefaultConfigReq;
import com.tencent.supersonic.chat.api.pojo.request.KnowledgeInfoReq;
import com.tencent.supersonic.chat.api.pojo.request.Entity;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatDefaultRichConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatAggRichConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatDetailRichConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.EntityRichInfoResp;
import com.tencent.supersonic.chat.api.pojo.response.ItemVisibilityInfo;
import com.tencent.supersonic.chat.config.ChatConfig;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.chat.persistence.repository.ChatConfigRepository;
import com.tencent.supersonic.chat.utils.ChatConfigHelper;
import com.tencent.supersonic.chat.utils.VisibilityEvent;
import com.tencent.supersonic.common.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Slf4j
@Service
public class ConfigServiceImpl implements ConfigService {

    private final ChatConfigRepository chatConfigRepository;
    private final ChatConfigHelper chatConfigHelper;
    private final DimensionService dimensionService;
    private final MetricService metricService;
    @Autowired
    private SemanticService semanticService;
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();


    public ConfigServiceImpl(ChatConfigRepository chatConfigRepository,
                             ChatConfigHelper chatConfigHelper,
                             DimensionService dimensionService,
                             MetricService metricService) {
        this.chatConfigRepository = chatConfigRepository;
        this.chatConfigHelper = chatConfigHelper;
        this.dimensionService = dimensionService;
        this.metricService = metricService;
    }

    @Override
    public Long addConfig(ChatConfigBaseReq configBaseCmd, User user) {
        log.info("[create model extend] object:{}", JsonUtil.toString(configBaseCmd, true));
        duplicateCheck(configBaseCmd.getModelId());
        permissionCheckLogic(configBaseCmd.getModelId(), user.getName());
        ChatConfig chaConfig = chatConfigHelper.newChatConfig(configBaseCmd, user);
        Long id = chatConfigRepository.createConfig(chaConfig);
        applicationEventPublisher.publishEvent(new VisibilityEvent(this, chaConfig));
        return id;
    }

    private void duplicateCheck(Long modelId) {
        ChatConfigFilter filter = new ChatConfigFilter();
        filter.setModelId(modelId);
        List<ChatConfigResp> chaConfigDescList = chatConfigRepository.getChatConfig(filter);
        if (!CollectionUtils.isEmpty(chaConfigDescList)) {
            throw new RuntimeException("chat config existed, no need to add repeatedly");
        }
    }


    @Override
    public Long editConfig(ChatConfigEditReqReq configEditCmd, User user) {
        log.info("[edit model extend] object:{}", JsonUtil.toString(configEditCmd, true));
        if (Objects.isNull(configEditCmd) || Objects.isNull(configEditCmd.getId()) && Objects.isNull(
                configEditCmd.getModelId())) {
            throw new RuntimeException("editConfig, id and modelId are not allowed to be empty at the same time");
        }
        permissionCheckLogic(configEditCmd.getModelId(), user.getName());
        ChatConfig chaConfig = chatConfigHelper.editChatConfig(configEditCmd, user);
        chatConfigRepository.updateConfig(chaConfig);
        applicationEventPublisher.publishEvent(new VisibilityEvent(this, chaConfig));
        return configEditCmd.getId();
    }

    public ItemNameVisibilityInfo getVisibilityByModelId(Long modelId) {
        ChatConfigResp chatConfigResp = fetchConfigByModelId(modelId);
        ChatConfig chatConfig = new ChatConfig();
        chatConfig.setModelId(modelId);
        chatConfig.setChatAggConfig(chatConfigResp.getChatAggConfig());
        chatConfig.setChatDetailConfig(chatConfigResp.getChatDetailConfig());
        ItemNameVisibilityInfo itemNameVisibility = getItemNameVisibility(chatConfig);
        return itemNameVisibility;
    }

    public ItemNameVisibilityInfo getItemNameVisibility(ChatConfig chatConfig) {
        Long modelId = chatConfig.getModelId();

        List<Long> blackDimIdList = new ArrayList<>();
        if (Objects.nonNull(chatConfig.getChatAggConfig()) && Objects.nonNull(chatConfig.getChatAggConfig())) {
            blackDimIdList.addAll(chatConfig.getChatAggConfig().getVisibility().getBlackDimIdList());
        }
        if (Objects.nonNull(chatConfig.getChatDetailConfig()) && Objects.nonNull(chatConfig.getChatDetailConfig())) {
            blackDimIdList.addAll(chatConfig.getChatDetailConfig().getVisibility().getBlackDimIdList());
        }
        List<Long> filterDimIdList = blackDimIdList.stream().distinct().collect(Collectors.toList());

        List<Long> blackMetricIdList = new ArrayList<>();
        if (Objects.nonNull(chatConfig.getChatAggConfig()) && Objects.nonNull(chatConfig.getChatAggConfig())) {
            blackMetricIdList.addAll(chatConfig.getChatAggConfig().getVisibility().getBlackMetricIdList());
        }
        if (Objects.nonNull(chatConfig.getChatDetailConfig()) && Objects.nonNull(chatConfig.getChatDetailConfig())) {
            blackMetricIdList.addAll(chatConfig.getChatDetailConfig().getVisibility().getBlackMetricIdList());
        }
        List<Long> filterMetricIdList = blackMetricIdList.stream().distinct().collect(Collectors.toList());

        ItemNameVisibilityInfo itemNameVisibility = new ItemNameVisibilityInfo();
        if (!CollectionUtils.isEmpty(blackDimIdList)) {
            List<DimensionResp> dimensionRespList = dimensionService.getDimensions(modelId);
            List<String> blackDimNameList = dimensionRespList.stream().filter(o -> filterDimIdList.contains(o.getId()))
                    .map(o -> o.getName()).collect(Collectors.toList());
            itemNameVisibility.setBlackDimNameList(blackDimNameList);
        }
        if (!CollectionUtils.isEmpty(blackMetricIdList)) {
            List<MetricResp> metricRespList = metricService.getMetrics(modelId);
            List<String> blackMetricList = metricRespList.stream().filter(o -> filterMetricIdList.contains(o.getId()))
                    .map(o -> o.getName()).collect(Collectors.toList());
            itemNameVisibility.setBlackMetricNameList(blackMetricList);
        }
        return itemNameVisibility;
    }

    /**
     * model administrators have the right to modify related configuration information.
     */
    private Boolean permissionCheckLogic(Long modelId, String staffName) {
        // todo
        return true;
    }

    @Override
    public List<ChatConfigResp> search(ChatConfigFilter filter, User user) {
        log.info("[search model extend] object:{}", JsonUtil.toString(filter, true));
        List<ChatConfigResp> chaConfigDescList = chatConfigRepository.getChatConfig(filter);
        return chaConfigDescList;
    }


    @Override
    public ChatConfigResp fetchConfigByModelId(Long modelId) {
        return chatConfigRepository.getConfigByModelId(modelId);
    }


    private ItemVisibilityInfo fetchVisibilityDescByConfig(ItemVisibility visibility,
                                                           ModelSchema modelSchema) {
        ItemVisibilityInfo itemVisibilityDesc = new ItemVisibilityInfo();

        List<Long> dimIdAllList = chatConfigHelper.generateAllDimIdList(modelSchema);
        List<Long> metricIdAllList = chatConfigHelper.generateAllMetricIdList(modelSchema);

        List<Long> blackDimIdList = new ArrayList<>();
        List<Long> blackMetricIdList = new ArrayList<>();
        if (Objects.nonNull(visibility)) {
            if (!CollectionUtils.isEmpty(visibility.getBlackDimIdList())) {
                blackDimIdList.addAll(visibility.getBlackDimIdList());
            }
            if (!CollectionUtils.isEmpty(visibility.getBlackMetricIdList())) {
                blackMetricIdList.addAll(visibility.getBlackMetricIdList());
            }
        }
        List<Long> whiteMetricIdList = metricIdAllList.stream()
                .filter(id -> !blackMetricIdList.contains(id) && metricIdAllList.contains(id))
                .collect(Collectors.toList());
        List<Long> whiteDimIdList = dimIdAllList.stream()
                .filter(id -> !blackDimIdList.contains(id) && dimIdAllList.contains(id))
                .collect(Collectors.toList());

        itemVisibilityDesc.setBlackDimIdList(blackDimIdList);
        itemVisibilityDesc.setBlackMetricIdList(blackMetricIdList);
        itemVisibilityDesc.setWhiteDimIdList(Objects.isNull(whiteDimIdList) ? new ArrayList<>() : whiteDimIdList);
        itemVisibilityDesc.setWhiteMetricIdList(
                Objects.isNull(whiteMetricIdList) ? new ArrayList<>() : whiteMetricIdList);

        return itemVisibilityDesc;
    }

    @Override
    public ChatConfigRichResp getConfigRichInfo(Long modelId) {
        ChatConfigRichResp chatConfigRich = new ChatConfigRichResp();
        ChatConfigResp chatConfigResp = chatConfigRepository.getConfigByModelId(modelId);
        if (Objects.isNull(chatConfigResp)) {
            log.info("there is no chatConfigDesc for modelId:{}", modelId);
            return chatConfigRich;
        }
        BeanUtils.copyProperties(chatConfigResp, chatConfigRich);

        ModelSchema modelSchema = semanticService.getModelSchema(modelId);
        chatConfigRich.setBizName(modelSchema.getModel().getBizName());
        chatConfigRich.setModelName(modelSchema.getModel().getName());

        chatConfigRich.setChatAggRichConfig(fillChatAggRichConfig(modelSchema, chatConfigResp));
        chatConfigRich.setChatDetailRichConfig(fillChatDetailRichConfig(modelSchema, chatConfigRich, chatConfigResp));

        return chatConfigRich;
    }

    private ChatDetailRichConfigResp fillChatDetailRichConfig(ModelSchema modelSchema,
                                                              ChatConfigRichResp chatConfigRich,
                                                              ChatConfigResp chatConfigResp) {
        if (Objects.isNull(chatConfigResp) || Objects.isNull(chatConfigResp.getChatDetailConfig())) {
            return null;
        }
        ChatDetailRichConfigResp detailRichConfig = new ChatDetailRichConfigResp();
        ChatDetailConfigReq chatDetailConfig = chatConfigResp.getChatDetailConfig();
        ItemVisibilityInfo itemVisibilityInfo = fetchVisibilityDescByConfig(
                chatDetailConfig.getVisibility(), modelSchema);
        detailRichConfig.setVisibility(itemVisibilityInfo);
        detailRichConfig.setKnowledgeInfos(fillKnowledgeBizName(chatDetailConfig.getKnowledgeInfos(), modelSchema));
        detailRichConfig.setGlobalKnowledgeConfig(chatDetailConfig.getGlobalKnowledgeConfig());
        detailRichConfig.setChatDefaultConfig(fetchDefaultConfig(chatDetailConfig.getChatDefaultConfig(),
                modelSchema, itemVisibilityInfo));

        return detailRichConfig;
    }

    private EntityRichInfoResp generateRichEntity(Entity entity, ModelSchema modelSchema) {
        EntityRichInfoResp entityRichInfo = new EntityRichInfoResp();
        if (Objects.isNull(entity) || Objects.isNull(entity.getEntityId())) {
            return entityRichInfo;
        }
        BeanUtils.copyProperties(entity, entityRichInfo);
        Map<Long, SchemaElement> dimIdAndRespPair = modelSchema.getDimensions().stream()
                .collect(Collectors.toMap(SchemaElement::getId, Function.identity(), (k1, k2) -> k1));

        entityRichInfo.setDimItem(dimIdAndRespPair.get(entity.getEntityId()));
        return entityRichInfo;
    }

    private ChatAggRichConfigResp fillChatAggRichConfig(ModelSchema modelSchema, ChatConfigResp chatConfigResp) {
        if (Objects.isNull(chatConfigResp) || Objects.isNull(chatConfigResp.getChatAggConfig())) {
            return null;
        }
        ChatAggConfigReq chatAggConfig = chatConfigResp.getChatAggConfig();
        ChatAggRichConfigResp chatAggRichConfig = new ChatAggRichConfigResp();
        ItemVisibilityInfo itemVisibilityInfo = fetchVisibilityDescByConfig(chatAggConfig.getVisibility(), modelSchema);
        chatAggRichConfig.setVisibility(itemVisibilityInfo);
        chatAggRichConfig.setKnowledgeInfos(fillKnowledgeBizName(chatAggConfig.getKnowledgeInfos(), modelSchema));
        chatAggRichConfig.setGlobalKnowledgeConfig(chatAggConfig.getGlobalKnowledgeConfig());
        chatAggRichConfig.setChatDefaultConfig(fetchDefaultConfig(chatAggConfig.getChatDefaultConfig(),
                modelSchema, itemVisibilityInfo));

        return chatAggRichConfig;
    }

    private ChatDefaultRichConfigResp fetchDefaultConfig(ChatDefaultConfigReq chatDefaultConfig,
                                                         ModelSchema modelSchema,
                                                         ItemVisibilityInfo itemVisibilityInfo) {
        ChatDefaultRichConfigResp defaultRichConfig = new ChatDefaultRichConfigResp();
        if (Objects.isNull(chatDefaultConfig)) {
            return defaultRichConfig;
        }
        BeanUtils.copyProperties(chatDefaultConfig, defaultRichConfig);
        Map<Long, SchemaElement> dimIdAndRespPair = modelSchema.getDimensions().stream()
                .collect(Collectors.toMap(SchemaElement::getId, Function.identity(), (k1, k2) -> k1));

        Map<Long, SchemaElement> metricIdAndRespPair = modelSchema.getMetrics().stream()
                .collect(Collectors.toMap(SchemaElement::getId, Function.identity(), (k1, k2) -> k1));

        List<SchemaElement> dimensions = new ArrayList<>();
        List<SchemaElement> metrics = new ArrayList<>();
        if (!CollectionUtils.isEmpty(chatDefaultConfig.getDimensionIds())) {
            chatDefaultConfig.getDimensionIds().stream()
                    .filter(dimId -> dimIdAndRespPair.containsKey(dimId)
                            && itemVisibilityInfo.getWhiteDimIdList().contains(dimId))
                    .forEach(dimId -> {
                        SchemaElement dimSchemaResp = dimIdAndRespPair.get(dimId);
                        if (Objects.nonNull(dimSchemaResp)) {
                            SchemaElement dimSchema = new SchemaElement();
                            BeanUtils.copyProperties(dimSchemaResp, dimSchema);
                            dimensions.add(dimSchema);
                        }

                    });
        }

        if (!CollectionUtils.isEmpty(chatDefaultConfig.getMetricIds())) {
            chatDefaultConfig.getMetricIds().stream()
                    .filter(metricId -> metricIdAndRespPair.containsKey(metricId)
                            && itemVisibilityInfo.getWhiteMetricIdList().contains(metricId))
                    .forEach(metricId -> {
                        SchemaElement metricSchemaResp = metricIdAndRespPair.get(metricId);
                        if (Objects.nonNull(metricSchemaResp)) {
                            SchemaElement metricSchema = new SchemaElement();
                            BeanUtils.copyProperties(metricSchemaResp, metricSchema);
                            metrics.add(metricSchema);
                        }
                    });
        }

        defaultRichConfig.setDimensions(dimensions);
        defaultRichConfig.setMetrics(metrics);
        return defaultRichConfig;
    }


    private List<KnowledgeInfoReq> fillKnowledgeBizName(List<KnowledgeInfoReq> knowledgeInfos,
                                                        ModelSchema modelSchema) {
        if (CollectionUtils.isEmpty(knowledgeInfos)) {
            return new ArrayList<>();
        }
        Map<Long, SchemaElement> dimIdAndRespPair = modelSchema.getDimensions().stream()
                .collect(Collectors.toMap(SchemaElement::getId, Function.identity(), (k1, k2) -> k1));
        knowledgeInfos.stream().forEach(knowledgeInfo -> {
            if (Objects.nonNull(knowledgeInfo)) {
                SchemaElement dimSchemaResp = dimIdAndRespPair.get(knowledgeInfo.getItemId());
                if (Objects.nonNull(dimSchemaResp)) {
                    knowledgeInfo.setBizName(dimSchemaResp.getBizName());
                }
            }
        });
        return knowledgeInfos;
    }

    @Override
    public List<ChatConfigRichResp> getAllChatRichConfig() {
        List<ChatConfigRichResp> chatConfigRichInfoList = new ArrayList<>();
        List<ModelSchema> modelSchemas = semanticLayer.getModelSchema();
        modelSchemas.stream().forEach(modelSchema -> {
            ChatConfigRichResp chatConfigRichInfo = getConfigRichInfo(modelSchema.getModel().getId());
            if (Objects.nonNull(chatConfigRichInfo)) {
                chatConfigRichInfoList.add(chatConfigRichInfo);
            }
        });
        return chatConfigRichInfoList;
    }
}
