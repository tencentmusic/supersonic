package com.tencent.supersonic.chat.domain.utils;

import static com.tencent.supersonic.common.constant.Constants.ADMIN_LOWER;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;
import com.tencent.supersonic.chat.domain.dataobject.ChatConfigDO;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfig;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigBase;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigEditReq;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigInfo;
import com.tencent.supersonic.chat.domain.pojo.config.DefaultMetricInfo;
import com.tencent.supersonic.chat.domain.pojo.config.Entity;
import com.tencent.supersonic.chat.domain.pojo.config.EntityDetailData;
import com.tencent.supersonic.chat.domain.pojo.config.EntityInternalDetail;
import com.tencent.supersonic.chat.domain.pojo.config.ItemVisibility;
import com.tencent.supersonic.chat.domain.pojo.config.KnowledgeInfo;
import com.tencent.supersonic.common.enums.StatusEnum;
import com.tencent.supersonic.common.util.RecordInfo;
import com.tencent.supersonic.common.util.json.JsonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


@Component
@Slf4j
public class ChatConfigUtils {

    public ChatConfig newChatConfig(ChatConfigBase extendBaseCmd, User user) {
        ChatConfig chaConfig = new ChatConfig();

        BeanUtils.copyProperties(extendBaseCmd, chaConfig);

        RecordInfo recordInfo = new RecordInfo();
        String creator = (Objects.isNull(user) || Strings.isEmpty(user.getName())) ? ADMIN_LOWER : user.getName();
        recordInfo.createdBy(creator);
        chaConfig.setRecordInfo(recordInfo);
        chaConfig.setStatus(StatusEnum.ONLINE);
        return chaConfig;
    }


    public ChatConfig editChaConfig(ChatConfigEditReq extendEditCmd, User facadeUser) {
        ChatConfig chaConfig = new ChatConfig();

        BeanUtils.copyProperties(extendEditCmd, chaConfig);

        RecordInfo recordInfo = new RecordInfo();
        String user = (Objects.isNull(facadeUser) || Strings.isEmpty(facadeUser.getName()))
                ? ADMIN_LOWER : facadeUser.getName();
        recordInfo.updatedBy(user);
        chaConfig.setRecordInfo(recordInfo);
        return chaConfig;
    }


    public List<DimSchemaResp> generateDimDesc(List<Long> dimIds, DomainSchemaResp domainSchemaDesc) {
        List<DimSchemaResp> dimSchemaDescList = new ArrayList<>();
        if (Objects.nonNull(domainSchemaDesc) && !CollectionUtils.isEmpty(domainSchemaDesc.getDimensions())) {
            dimSchemaDescList = domainSchemaDesc.getDimensions().stream()
                    .filter(dimSchemaDesc -> dimIds.contains(dimSchemaDesc.getId()))
                    .collect(Collectors.toList());
        }
        return dimSchemaDescList;
    }

    public List<MetricSchemaResp> generateMetricDesc(List<Long> metricIds, DomainSchemaResp domainSchemaDesc) {
        List<MetricSchemaResp> metricSchemaDescList = new ArrayList<>();
        if (Objects.nonNull(domainSchemaDesc) && !CollectionUtils.isEmpty(domainSchemaDesc.getMetrics())) {
            metricSchemaDescList = domainSchemaDesc.getMetrics().stream()
                    .filter(metricSchemaDesc -> metricIds.contains(metricSchemaDesc.getId()))
                    .collect(Collectors.toList());
        }
        return metricSchemaDescList;
    }

    public EntityInternalDetail generateEntityDetailData(EntityDetailData detailData,
                                                         DomainSchemaResp domainSchemaDesc) {
        EntityInternalDetail entityInternalDetailDesc = new EntityInternalDetail();
        if (Objects.isNull(detailData)) {
            return entityInternalDetailDesc;
        }
        entityInternalDetailDesc.setDimensionList(generateDimDesc(detailData.getDimensionIds(), domainSchemaDesc));
        entityInternalDetailDesc.setMetricList(generateMetricDesc(detailData.getMetricIds(), domainSchemaDesc));

        return entityInternalDetailDesc;
    }

    public Map<Long, MetricSchemaResp> generateMetricIdAndDescPair(List<Long> metricIds,
                                                                   DomainSchemaResp domainSchemaDesc) {
        Map<Long, MetricSchemaResp> metricIdAndDescPair = new HashMap<>();
        List<MetricSchemaResp> metricDescList = generateMetricDesc(metricIds, domainSchemaDesc);

        metricDescList.stream().forEach(metricDesc -> metricIdAndDescPair.put(metricDesc.getId(), metricDesc));
        return metricIdAndDescPair;
    }

    public List<Long> generateAllDimIdList(DomainSchemaResp domainSchemaDesc) {
        if (Objects.isNull(domainSchemaDesc) || CollectionUtils.isEmpty(domainSchemaDesc.getDimensions())) {
            return new ArrayList<>();
        }
        Map<Long, List<DimSchemaResp>> dimIdAndDescPair = domainSchemaDesc.getDimensions()
                .stream().collect(Collectors.groupingBy(DimSchemaResp::getId));
        return new ArrayList<>(dimIdAndDescPair.keySet());
    }

    public List<Long> generateAllMetricIdList(DomainSchemaResp domainSchemaDesc) {
        Map<Long, List<MetricSchemaResp>> metricIdAndDescPair = domainSchemaDesc.getMetrics()
                .stream().collect(Collectors.groupingBy(MetricSchemaResp::getId));
        return new ArrayList<>(metricIdAndDescPair.keySet());
    }

    public ChatConfigDO chatConfig2DO(ChatConfig chaConfig) {
        ChatConfigDO chaConfigDO = new ChatConfigDO();
        BeanUtils.copyProperties(chaConfig, chaConfigDO);

        chaConfigDO.setDefaultMetrics(JsonUtil.toString(chaConfig.getDefaultMetrics()));
        chaConfigDO.setVisibility(JsonUtil.toString(chaConfig.getVisibility()));
        chaConfigDO.setEntity(JsonUtil.toString(chaConfig.getEntity()));
        chaConfigDO.setKnowledgeInfo(JsonUtil.toString(chaConfig.getKnowledgeInfos()));

        if (Objects.isNull(chaConfig.getStatus())) {
            chaConfigDO.setStatus(null);
        } else {
            chaConfigDO.setStatus(chaConfig.getStatus().getCode());
        }

        chaConfigDO.setCreatedBy(chaConfig.getRecordInfo().getCreatedBy());
        chaConfigDO.setCreatedAt(chaConfig.getRecordInfo().getCreatedAt());
        chaConfigDO.setUpdatedBy(chaConfig.getRecordInfo().getUpdatedBy());
        chaConfigDO.setUpdatedAt(chaConfig.getRecordInfo().getUpdatedAt());

        return chaConfigDO;
    }

    public ChatConfigInfo chatConfigDO2Descriptor(ChatConfigDO chaConfigDO) {
        ChatConfigInfo chaConfigDescriptor = new ChatConfigInfo();
        BeanUtils.copyProperties(chaConfigDO, chaConfigDescriptor);

        chaConfigDescriptor.setDefaultMetrics(
                JsonUtil.toList(chaConfigDO.getDefaultMetrics(), DefaultMetricInfo.class));
        chaConfigDescriptor.setVisibility(JsonUtil.toObject(chaConfigDO.getVisibility(), ItemVisibility.class));
        chaConfigDescriptor.setEntity(JsonUtil.toObject(chaConfigDO.getEntity(), Entity.class));
        chaConfigDescriptor.setDictionaryInfos(JsonUtil.toList(chaConfigDO.getKnowledgeInfo(), KnowledgeInfo.class));
        chaConfigDescriptor.setStatusEnum(StatusEnum.of(chaConfigDO.getStatus()));

        chaConfigDescriptor.setCreatedBy(chaConfigDO.getCreatedBy());
        chaConfigDescriptor.setCreatedAt(chaConfigDO.getCreatedAt());
        chaConfigDescriptor.setUpdatedBy(chaConfigDO.getUpdatedBy());
        chaConfigDescriptor.setUpdatedAt(chaConfigDO.getUpdatedAt());

        return chaConfigDescriptor;
    }
}
