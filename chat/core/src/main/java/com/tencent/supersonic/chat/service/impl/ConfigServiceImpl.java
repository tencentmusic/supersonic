package com.tencent.supersonic.chat.service.impl;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.pojo.DomainSchema;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.config.*;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.semantic.api.model.response.DomainResp;
import com.tencent.supersonic.chat.persistence.repository.ChatConfigRepository;
import com.tencent.supersonic.chat.utils.ChatConfigHelper;
import com.tencent.supersonic.common.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Slf4j
@Service
public class ConfigServiceImpl implements ConfigService {

    private final ChatConfigRepository chatConfigRepository;
    private final ChatConfigHelper chatConfigHelper;
    @Autowired
    private SemanticService semanticService;

    private SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();


    public ConfigServiceImpl(ChatConfigRepository chatConfigRepository,
                             ChatConfigHelper chatConfigHelper) {
        this.chatConfigRepository = chatConfigRepository;
        this.chatConfigHelper = chatConfigHelper;
    }

    @Override
    public Long addConfig(ChatConfigBaseReq configBaseCmd, User user) {
        log.info("[create domain extend] object:{}", JsonUtil.toString(configBaseCmd, true));
        duplicateCheck(configBaseCmd.getDomainId());
        permissionCheckLogic(configBaseCmd.getDomainId(), user.getName());
        ChatConfig chaConfig = chatConfigHelper.newChatConfig(configBaseCmd, user);
        Long id = chatConfigRepository.createConfig(chaConfig);
        return id;
    }

    private void duplicateCheck(Long domainId) {
        ChatConfigFilter filter = new ChatConfigFilter();
        filter.setDomainId(domainId);
        List<ChatConfigResp> chaConfigDescList = chatConfigRepository.getChatConfig(filter);
        if (!CollectionUtils.isEmpty(chaConfigDescList)) {
            throw new RuntimeException("chat config existed, no need to add repeatedly");
        }
    }


    @Override
    public Long editConfig(ChatConfigEditReqReq configEditCmd, User user) {
        log.info("[edit domain extend] object:{}", JsonUtil.toString(configEditCmd, true));
        if (Objects.isNull(configEditCmd) || Objects.isNull(configEditCmd.getId()) && Objects.isNull(
                configEditCmd.getDomainId())) {
            throw new RuntimeException("editConfig, id and domainId are not allowed to be empty at the same time");
        }
        permissionCheckLogic(configEditCmd.getDomainId(), user.getName());
        ChatConfig chaConfig = chatConfigHelper.editChatConfig(configEditCmd, user);
        chatConfigRepository.updateConfig(chaConfig);
        return configEditCmd.getId();
    }


    /**
     * domain administrators have the right to modify related configuration information.
     */
    private Boolean permissionCheckLogic(Long domainId, String staffName) {
        // todo
        return true;
    }

    @Override
    public List<ChatConfigResp> search(ChatConfigFilter filter, User user) {
        log.info("[search domain extend] object:{}", JsonUtil.toString(filter, true));
        List<ChatConfigResp> chaConfigDescList = chatConfigRepository.getChatConfig(filter);
        return chaConfigDescList;
    }


    @Override
    public ChatConfigResp fetchConfigByDomainId(Long domainId) {
        return chatConfigRepository.getConfigByDomainId(domainId);
    }


    private ItemVisibilityInfo fetchVisibilityDescByConfig(ItemVisibility visibility,
                                                           DomainSchema domainSchema) {
        ItemVisibilityInfo itemVisibilityDesc = new ItemVisibilityInfo();

        List<Long> dimIdAllList = chatConfigHelper.generateAllDimIdList(domainSchema);
        List<Long> metricIdAllList = chatConfigHelper.generateAllMetricIdList(domainSchema);

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
        itemVisibilityDesc.setWhiteMetricIdList(Objects.isNull(whiteMetricIdList) ? new ArrayList<>() : whiteMetricIdList);

        return itemVisibilityDesc;
    }

    @Override
    public ChatConfigRich getConfigRichInfo(Long domainId) {
        ChatConfigRich chatConfigRich = new ChatConfigRich();
        ChatConfigResp chatConfigResp = chatConfigRepository.getConfigByDomainId(domainId);
        if (Objects.isNull(chatConfigResp)) {
            log.info("there is no chatConfigDesc for domainId:{}", domainId);
            return chatConfigRich;
        }
        BeanUtils.copyProperties(chatConfigResp, chatConfigRich);

        DomainSchema domainSchema = semanticService.getDomainSchema(domainId);
        chatConfigRich.setBizName(domainSchema.getDomain().getBizName());
        chatConfigRich.setDomainName(domainSchema.getDomain().getName());

        chatConfigRich.setChatAggRichConfig(fillChatAggRichConfig(domainSchema, chatConfigResp));
        chatConfigRich.setChatDetailRichConfig(fillChatDetailRichConfig(domainSchema, chatConfigRich, chatConfigResp));

        return chatConfigRich;
    }

    private ChatDetailRichConfig fillChatDetailRichConfig(DomainSchema domainSchema, ChatConfigRich chatConfigRich, ChatConfigResp chatConfigResp) {
        if (Objects.isNull(chatConfigResp) || Objects.isNull(chatConfigResp.getChatDetailConfig())) {
            return null;
        }
        ChatDetailRichConfig detailRichConfig = new ChatDetailRichConfig();
        ChatDetailConfig chatDetailConfig = chatConfigResp.getChatDetailConfig();
        ItemVisibilityInfo itemVisibilityInfo = fetchVisibilityDescByConfig(chatDetailConfig.getVisibility(), domainSchema);
        detailRichConfig.setVisibility(itemVisibilityInfo);
        detailRichConfig.setKnowledgeInfos(fillKnowledgeBizName(chatDetailConfig.getKnowledgeInfos(), domainSchema));
        detailRichConfig.setGlobalKnowledgeConfig(chatDetailConfig.getGlobalKnowledgeConfig());
        detailRichConfig.setChatDefaultConfig(fetchDefaultConfig(chatDetailConfig.getChatDefaultConfig(), domainSchema, itemVisibilityInfo));

        detailRichConfig.setEntity(generateRichEntity(chatDetailConfig.getEntity(), domainSchema));
        return detailRichConfig;
    }

    private EntityRichInfo generateRichEntity(Entity entity, DomainSchema domainSchema) {
        EntityRichInfo entityRichInfo = new EntityRichInfo();
        if (Objects.isNull(entity) || Objects.isNull(entity.getEntityId())) {
            return entityRichInfo;
        }
        BeanUtils.copyProperties(entity, entityRichInfo);
        Map<Long, SchemaElement> dimIdAndRespPair = domainSchema.getDimensions().stream()
                .collect(Collectors.toMap(SchemaElement::getId, Function.identity(), (k1, k2) -> k1));

        entityRichInfo.setDimItem(dimIdAndRespPair.get(entity.getEntityId()));
        return entityRichInfo;
    }

    private ChatAggRichConfig fillChatAggRichConfig(DomainSchema domainSchema, ChatConfigResp chatConfigResp) {
        if (Objects.isNull(chatConfigResp) || Objects.isNull(chatConfigResp.getChatAggConfig())) {
            return null;
        }
        ChatAggConfig chatAggConfig = chatConfigResp.getChatAggConfig();
        ChatAggRichConfig chatAggRichConfig = new ChatAggRichConfig();
        ItemVisibilityInfo itemVisibilityInfo = fetchVisibilityDescByConfig(chatAggConfig.getVisibility(), domainSchema);
        chatAggRichConfig.setVisibility(itemVisibilityInfo);
        chatAggRichConfig.setKnowledgeInfos(fillKnowledgeBizName(chatAggConfig.getKnowledgeInfos(), domainSchema));
        chatAggRichConfig.setGlobalKnowledgeConfig(chatAggConfig.getGlobalKnowledgeConfig());
        chatAggRichConfig.setChatDefaultConfig(fetchDefaultConfig(chatAggConfig.getChatDefaultConfig(), domainSchema, itemVisibilityInfo));

        return chatAggRichConfig;
    }

    private ChatDefaultRichConfig fetchDefaultConfig(ChatDefaultConfig chatDefaultConfig, DomainSchema domainSchema, ItemVisibilityInfo itemVisibilityInfo) {
        ChatDefaultRichConfig defaultRichConfig = new ChatDefaultRichConfig();
        if (Objects.isNull(chatDefaultConfig)) {
            return defaultRichConfig;
        }
        BeanUtils.copyProperties(chatDefaultConfig, defaultRichConfig);
        Map<Long, SchemaElement> dimIdAndRespPair = domainSchema.getDimensions().stream()
                .collect(Collectors.toMap(SchemaElement::getId, Function.identity(), (k1, k2) -> k1));

        Map<Long, SchemaElement> metricIdAndRespPair = domainSchema.getMetrics().stream()
                .collect(Collectors.toMap(SchemaElement::getId, Function.identity(), (k1, k2) -> k1));

        List<SchemaElement> dimensions = new ArrayList<>();
        List<SchemaElement> metrics = new ArrayList<>();
        if (!CollectionUtils.isEmpty(chatDefaultConfig.getDimensionIds())) {
            chatDefaultConfig.getDimensionIds().stream()
                    .filter(dimId -> dimIdAndRespPair.containsKey(dimId) && itemVisibilityInfo.getWhiteDimIdList().contains(dimId))
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
                    .filter(metricId -> metricIdAndRespPair.containsKey(metricId) && itemVisibilityInfo.getWhiteMetricIdList().contains(metricId))
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


    private List<KnowledgeInfo> fillKnowledgeBizName(List<KnowledgeInfo> knowledgeInfos,
                                                     DomainSchema domainSchema) {
        if (CollectionUtils.isEmpty(knowledgeInfos)) {
            return new ArrayList<>();
        }
        Map<Long, SchemaElement> dimIdAndRespPair = domainSchema.getDimensions().stream()
                .collect(Collectors.toMap(SchemaElement::getId, Function.identity(),(k1, k2) -> k1));
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
    public List<ChatConfigRich> getAllChatRichConfig() {
        List<ChatConfigRich> chatConfigRichInfoList = new ArrayList<>();
        List<DomainResp> domainRespList = semanticLayer.getDomainListForAdmin();
        domainRespList.stream().forEach(domainResp -> {
            ChatConfigRich chatConfigRichInfo = getConfigRichInfo(domainResp.getId());
            if (Objects.nonNull(chatConfigRichInfo)) {
                chatConfigRichInfoList.add(chatConfigRichInfo);
            }
        });
        return chatConfigRichInfoList;
    }
}
