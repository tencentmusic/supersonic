package com.tencent.supersonic.chat.application;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.domain.pojo.config.*;
import com.tencent.supersonic.chat.domain.utils.ComponentFactory;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.DomainResp;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;
import com.tencent.supersonic.chat.domain.repository.ChatConfigRepository;
import com.tencent.supersonic.chat.domain.service.ConfigService;
import com.tencent.supersonic.chat.domain.utils.ChatConfigUtils;
import com.tencent.supersonic.common.util.json.JsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Slf4j
@Service
public class ConfigServiceImpl implements ConfigService {

    private final ChatConfigRepository chatConfigRepository;
    private final ChatConfigUtils chatConfigUtils;
    private SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();


    public ConfigServiceImpl(ChatConfigRepository chatConfigRepository,
                             ChatConfigUtils chatConfigUtils) {
        this.chatConfigRepository = chatConfigRepository;
        this.chatConfigUtils = chatConfigUtils;
    }

    @Override
    public Long addConfig(ChatConfigBaseReq configBaseCmd, User user) {
        log.info("[create domain extend] object:{}", JsonUtil.toString(configBaseCmd, true));
        duplicateCheck(configBaseCmd.getDomainId());
        permissionCheckLogic(configBaseCmd.getDomainId(), user.getName());
        ChatConfig chaConfig = chatConfigUtils.newChatConfig(configBaseCmd, user);
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
        ChatConfig chaConfig = chatConfigUtils.editChatConfig(configEditCmd, user);
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
                                                           DomainSchemaResp domainSchemaDesc) {
        ItemVisibilityInfo itemVisibilityDesc = new ItemVisibilityInfo();

        List<Long> dimIdAllList = chatConfigUtils.generateAllDimIdList(domainSchemaDesc);
        List<Long> metricIdAllList = chatConfigUtils.generateAllMetricIdList(domainSchemaDesc);

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
        List<Long> whiteMetricIdList = metricIdAllList.stream().filter(id -> !blackMetricIdList.contains(id))
                .collect(Collectors.toList());
        List<Long> whiteDimIdList = dimIdAllList.stream().filter(id -> !blackDimIdList.contains(id))
                .collect(Collectors.toList());

        itemVisibilityDesc.setBlackDimIdList(blackDimIdList);
        itemVisibilityDesc.setBlackMetricIdList(blackMetricIdList);
        itemVisibilityDesc.setWhiteDimIdList(whiteDimIdList);
        itemVisibilityDesc.setWhiteMetricIdList(whiteMetricIdList);

        return itemVisibilityDesc;
    }

    @Override
    public ChatConfigRichResp getConfigRichInfo(Long domainId) {
        ChatConfigRichResp chatConfigRichResp = new ChatConfigRichResp();
        ChatConfigResp chatConfigResp = chatConfigRepository.getConfigByDomainId(domainId);
        if (Objects.isNull(chatConfigResp)) {
            log.info("there is no chatConfigDesc for domainId:{}", domainId);
            return chatConfigRichResp;
        }
        BeanUtils.copyProperties(chatConfigResp, chatConfigRichResp);

        SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();
        DomainSchemaResp domainSchemaInfo = semanticLayer.getDomainSchemaInfo(domainId, false);
        chatConfigRichResp.setBizName(domainSchemaInfo.getBizName());
        chatConfigRichResp.setDomainName(domainSchemaInfo.getName());

        chatConfigRichResp.setChatAggRichConfig(fillChatAggRichConfig(domainSchemaInfo, chatConfigResp));
        chatConfigRichResp.setChatDetailRichConfig(fillChatDetailRichConfig(domainSchemaInfo, chatConfigRichResp, chatConfigResp));

        return chatConfigRichResp;
    }

    private ChatDetailRichConfig fillChatDetailRichConfig(DomainSchemaResp domainSchemaInfo, ChatConfigRichResp chatConfigRichResp, ChatConfigResp chatConfigResp) {
        if (Objects.isNull(chatConfigResp) || Objects.isNull(chatConfigResp.getChatDetailConfig())) {
            return null;
        }
        ChatDetailRichConfig detailRichConfig = new ChatDetailRichConfig();
        ChatDetailConfig chatDetailConfig = chatConfigResp.getChatDetailConfig();

        detailRichConfig.setVisibility(fetchVisibilityDescByConfig(chatDetailConfig.getVisibility(), domainSchemaInfo));
        detailRichConfig.setKnowledgeInfos(fillKnowledgeBizName(chatDetailConfig.getKnowledgeInfos(), domainSchemaInfo));
        detailRichConfig.setGlobalKnowledgeConfig(chatDetailConfig.getGlobalKnowledgeConfig());
        detailRichConfig.setChatDefaultConfig(fetchDefaultConfig(chatDetailConfig.getChatDefaultConfig(), domainSchemaInfo));

        detailRichConfig.setEntity(generateRichEntity(chatDetailConfig.getEntity(), domainSchemaInfo));
        return detailRichConfig;
    }

    private EntityRichInfo generateRichEntity(Entity entity, DomainSchemaResp domainSchemaInfo) {
        EntityRichInfo entityRichInfo = new EntityRichInfo();
        if (Objects.isNull(entity) || Objects.isNull(entity.getEntityId())) {
            return entityRichInfo;
        }
        BeanUtils.copyProperties(entity, entityRichInfo);
        Map<Long, DimSchemaResp> dimIdAndRespPair = domainSchemaInfo.getDimensions().stream()
                .collect(Collectors.toMap(DimSchemaResp::getId, Function.identity()));

        entityRichInfo.setDimItem(dimIdAndRespPair.get(entity.getEntityId()));
        return entityRichInfo;
    }

    private ChatAggRichConfig fillChatAggRichConfig(DomainSchemaResp domainSchemaInfo, ChatConfigResp chatConfigResp) {
        if (Objects.isNull(chatConfigResp) || Objects.isNull(chatConfigResp.getChatAggConfig())) {
            return null;
        }
        ChatAggConfig chatAggConfig = chatConfigResp.getChatAggConfig();
        ChatAggRichConfig chatAggRichConfig = new ChatAggRichConfig();

        chatAggRichConfig.setVisibility(fetchVisibilityDescByConfig(chatAggConfig.getVisibility(), domainSchemaInfo));
        chatAggRichConfig.setKnowledgeInfos(fillKnowledgeBizName(chatAggConfig.getKnowledgeInfos(), domainSchemaInfo));
        chatAggRichConfig.setGlobalKnowledgeConfig(chatAggConfig.getGlobalKnowledgeConfig());
        chatAggRichConfig.setChatDefaultConfig(fetchDefaultConfig(chatAggConfig.getChatDefaultConfig(), domainSchemaInfo));

        return chatAggRichConfig;
    }

    private ChatDefaultRichConfig fetchDefaultConfig(ChatDefaultConfig chatDefaultConfig, DomainSchemaResp domainSchemaInfo) {
        ChatDefaultRichConfig defaultRichConfig = new ChatDefaultRichConfig();
        if (Objects.isNull(chatDefaultConfig)) {
            return defaultRichConfig;
        }
        BeanUtils.copyProperties(chatDefaultConfig, defaultRichConfig);
        Map<Long, DimSchemaResp> dimIdAndRespPair = domainSchemaInfo.getDimensions().stream()
                .collect(Collectors.toMap(DimSchemaResp::getId, Function.identity()));

        Map<Long, MetricSchemaResp> metricIdAndRespPair = domainSchemaInfo.getMetrics().stream()
                .collect(Collectors.toMap(MetricSchemaResp::getId, Function.identity()));

        List<SchemaItem> dimensions = new ArrayList<>();
        List<SchemaItem> metrics = new ArrayList<>();
        if (!CollectionUtils.isEmpty(chatDefaultConfig.getDimensionIds())) {
            chatDefaultConfig.getDimensionIds().stream().forEach(dimId -> {
                DimSchemaResp dimSchemaResp = dimIdAndRespPair.get(dimId);
                SchemaItem dimSchema = new SchemaItem();
                BeanUtils.copyProperties(dimSchemaResp, dimSchema);
                dimensions.add(dimSchema);
            });
        }

        if (!CollectionUtils.isEmpty(chatDefaultConfig.getMetricIds())) {
            chatDefaultConfig.getMetricIds().stream().forEach(metricId -> {
                MetricSchemaResp metricSchemaResp = metricIdAndRespPair.get(metricId);
                SchemaItem metricSchema = new SchemaItem();
                BeanUtils.copyProperties(metricSchemaResp, metricSchema);
                metrics.add(metricSchema);
            });
        }

        defaultRichConfig.setDimensions(dimensions);
        defaultRichConfig.setMetrics(metrics);
        return defaultRichConfig;
    }


    private List<KnowledgeInfo> fillKnowledgeBizName(List<KnowledgeInfo> knowledgeInfos,
                                                     DomainSchemaResp domainSchemaInfo) {
        if (CollectionUtils.isEmpty(knowledgeInfos)) {
            return new ArrayList<>();
        }
        Map<Long, DimSchemaResp> dimIdAndRespPair = domainSchemaInfo.getDimensions().stream()
                .collect(Collectors.toMap(DimSchemaResp::getId, Function.identity()));
        knowledgeInfos.stream().forEach(knowledgeInfo -> {
            if (Objects.nonNull(knowledgeInfo)) {
                DimSchemaResp dimSchemaResp = dimIdAndRespPair.get(knowledgeInfo.getItemId());
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
        List<DomainResp> domainRespList = semanticLayer.getDomainListForAdmin();
        domainRespList.stream().forEach(domainResp -> {
            ChatConfigRichResp chatConfigRichInfo = getConfigRichInfo(domainResp.getId());
            if (Objects.nonNull(chatConfigRichInfo)) {
                chatConfigRichInfoList.add(chatConfigRichInfo);
            }
        });
        return chatConfigRichInfoList;
    }
}
