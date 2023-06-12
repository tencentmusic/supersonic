package com.tencent.supersonic.chat.application;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.service.SemanticLayer;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfig;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigBase;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigEditReq;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigFilter;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigInfo;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichInfo;
import com.tencent.supersonic.chat.domain.pojo.config.DefaultMetric;
import com.tencent.supersonic.chat.domain.pojo.config.EntityRichInfo;
import com.tencent.supersonic.chat.domain.pojo.config.ItemVisibilityInfo;
import com.tencent.supersonic.chat.domain.repository.ChatConfigRepository;
import com.tencent.supersonic.chat.domain.service.ConfigService;
import com.tencent.supersonic.chat.domain.utils.ChatConfigUtils;
import com.tencent.supersonic.common.util.json.JsonUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Slf4j
@Service
public class ConfigServiceImpl implements ConfigService {

    private final ChatConfigRepository chaConfigRepository;
    private final SemanticLayer semanticLayer;
    private final ChatConfigUtils chatConfigUtils;

    public ConfigServiceImpl(ChatConfigRepository chaConfigRepository,
            @Lazy SemanticLayer semanticLayer,
            ChatConfigUtils chatConfigUtils) {
        this.chaConfigRepository = chaConfigRepository;
        this.semanticLayer = semanticLayer;
        this.chatConfigUtils = chatConfigUtils;
    }

    @Override
    public Long addConfig(ChatConfigBase configBaseCmd, User user) {
        log.info("[create domain extend] object:{}", JsonUtil.toString(configBaseCmd, true));
        duplicateCheck(configBaseCmd.getDomainId());
        permissionCheckLogic(configBaseCmd.getDomainId(), user.getName());
        ChatConfig chaConfig = chatConfigUtils.newChatConfig(configBaseCmd, user);
        chaConfigRepository.createConfig(chaConfig);
        return chaConfig.getDomainId();
    }

    private void duplicateCheck(Long domainId) {
        ChatConfigFilter filter = new ChatConfigFilter();
        filter.setDomainId(domainId);
        List<ChatConfigInfo> chaConfigDescList = chaConfigRepository.getChatConfig(filter);
        if (!CollectionUtils.isEmpty(chaConfigDescList)) {
            throw new RuntimeException("chat config existed, no need to add repeatedly");
        }
    }


    @Override
    public Long editConfig(ChatConfigEditReq configEditCmd, User user) {
        log.info("[edit domain extend] object:{}", JsonUtil.toString(configEditCmd, true));
        if (Objects.isNull(configEditCmd) || Objects.isNull(configEditCmd.getId()) && Objects.isNull(
                configEditCmd.getDomainId())) {
            throw new RuntimeException("editConfig, id and domainId are not allowed to be empty at the same time");
        }
        permissionCheckLogic(configEditCmd.getDomainId(), user.getName());
        ChatConfig chaConfig = chatConfigUtils.editChaConfig(configEditCmd, user);
        chaConfigRepository.updateConfig(chaConfig);
        return configEditCmd.getDomainId();
    }


    /**
     * domain administrators have the right to modify related configuration information.
     */
    private Boolean permissionCheckLogic(Long domainId, String staffName) {
        // todo
        return true;
    }

    @Override
    public List<ChatConfigInfo> search(ChatConfigFilter filter, User user) {
        log.info("[search domain extend] object:{}", JsonUtil.toString(filter, true));
        List<ChatConfigInfo> chaConfigDescList = chaConfigRepository.getChatConfig(filter);
        return chaConfigDescList;
    }


    public ChatConfigInfo fetchConfigByDomainId(Long domainId) {
        return chaConfigRepository.getConfigByDomainId(domainId);
    }

    public EntityRichInfo fetchEntityDescByDomainId(Long domainId) {

        ChatConfigInfo chaConfigDesc = chaConfigRepository.getConfigByDomainId(domainId);
        return fetchEntityDescByConfig(chaConfigDesc);
    }

    public EntityRichInfo fetchEntityDescByConfig(ChatConfigInfo chatConfigDesc) {
        Long domainId = chatConfigDesc.getDomainId();
        EntityRichInfo entityDesc = new EntityRichInfo();
        if (Objects.isNull(chatConfigDesc) || Objects.isNull(chatConfigDesc.getEntity())) {
            log.info("domainId:{}, entityDesc info is null", domainId);
            return entityDesc;
        }
        DomainSchemaResp domain = semanticLayer.getDomainSchemaInfo(domainId);

        entityDesc.setDomainId(domain.getId());
        entityDesc.setDomainBizName(domain.getBizName());
        entityDesc.setDomainName(domain.getName());
        entityDesc.setNames(chatConfigDesc.getEntity().getNames());

        entityDesc.setEntityIds(chatConfigUtils.generateDimDesc(chatConfigDesc.getEntity().getEntityIds(), domain));
        entityDesc.setEntityInternalDetailDesc(
                chatConfigUtils.generateEntityDetailData(chatConfigDesc.getEntity().getDetailData(), domain));
        return entityDesc;
    }


    public List<DefaultMetric> fetchDefaultMetricDescByDomainId(Long domainId) {
        ChatConfigInfo chatConfigDesc = chaConfigRepository.getConfigByDomainId(domainId);
        return fetchDefaultMetricDescByConfig(chatConfigDesc);
    }

    public List<DefaultMetric> fetchDefaultMetricDescByConfig(ChatConfigInfo chatConfigDesc) {
        Long domainId = chatConfigDesc.getDomainId();
        DomainSchemaResp domain = semanticLayer.getDomainSchemaInfo(domainId);
        List<DefaultMetric> defaultMetricDescList = new ArrayList<>();
        if (Objects.isNull(chatConfigDesc) || CollectionUtils.isEmpty(chatConfigDesc.getDefaultMetrics())) {
            log.info("domainId:{}, defaultMetricDescList info is null", domainId);
            return defaultMetricDescList;
        }
        List<Long> metricIds = chatConfigDesc.getDefaultMetrics().stream()
                .map(defaultMetricInfo -> defaultMetricInfo.getMetricId()).collect(Collectors.toList());
        Map<Long, MetricSchemaResp> metricIdAndDescPair = chatConfigUtils.generateMetricIdAndDescPair(metricIds,
                domain);
        chatConfigDesc.getDefaultMetrics().stream().forEach(defaultMetricInfo -> {
            DefaultMetric defaultMetricDesc = new DefaultMetric();
            BeanUtils.copyProperties(defaultMetricInfo, defaultMetricDesc);
            if (metricIdAndDescPair.containsKey(defaultMetricInfo.getMetricId())) {
                MetricSchemaResp metricDesc = metricIdAndDescPair.get(defaultMetricInfo.getMetricId());
                defaultMetricDesc.setBizName(metricDesc.getBizName());
                defaultMetricDesc.setName(metricDesc.getName());
            }
            defaultMetricDescList.add(defaultMetricDesc);
        });
        return defaultMetricDescList;
    }

    public ItemVisibilityInfo fetchVisibilityDescByDomainId(Long domainId) {
        ChatConfigInfo chatConfigDesc = chaConfigRepository.getConfigByDomainId(domainId);
        return fetchVisibilityDescByConfig(chatConfigDesc);
    }

    private ItemVisibilityInfo fetchVisibilityDescByConfig(ChatConfigInfo chatConfigDesc) {
        ItemVisibilityInfo itemVisibilityDesc = new ItemVisibilityInfo();
        Long domainId = chatConfigDesc.getDomainId();
        DomainSchemaResp domainSchemaDesc = semanticLayer.getDomainSchemaInfo(domainId);
        List<Long> dimIdAllList = chatConfigUtils.generateAllDimIdList(domainSchemaDesc);
        List<Long> metricIdAllList = chatConfigUtils.generateAllMetricIdList(domainSchemaDesc);

        List<Long> blackDimIdList = new ArrayList<>();
        List<Long> blackMetricIdList = new ArrayList<>();
        if (Objects.nonNull(chatConfigDesc.getVisibility())) {
            if (!CollectionUtils.isEmpty(chatConfigDesc.getVisibility().getBlackDimIdList())) {
                blackDimIdList.addAll(chatConfigDesc.getVisibility().getBlackDimIdList());
            }
            if (!CollectionUtils.isEmpty(chatConfigDesc.getVisibility().getBlackMetricIdList())) {
                blackMetricIdList.addAll(chatConfigDesc.getVisibility().getBlackMetricIdList());
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
    public ChatConfigRichInfo getConfigRichInfo(Long domainId) {
        ChatConfigRichInfo chaConfigRichDesc = new ChatConfigRichInfo();
        ChatConfigInfo chatConfigDesc = chaConfigRepository.getConfigByDomainId(domainId);
        BeanUtils.copyProperties(chatConfigDesc, chaConfigRichDesc);

        DomainSchemaResp domainSchemaDesc = semanticLayer.getDomainSchemaInfo(domainId);
        chaConfigRichDesc.setBizName(domainSchemaDesc.getBizName());
        chaConfigRichDesc.setName(domainSchemaDesc.getName());

        chaConfigRichDesc.setDefaultMetrics(fetchDefaultMetricDescByConfig(chatConfigDesc));
        chaConfigRichDesc.setVisibility(fetchVisibilityDescByConfig(chatConfigDesc));
        chaConfigRichDesc.setEntity(fetchEntityDescByConfig(chatConfigDesc));

        return chaConfigRichDesc;
    }
}