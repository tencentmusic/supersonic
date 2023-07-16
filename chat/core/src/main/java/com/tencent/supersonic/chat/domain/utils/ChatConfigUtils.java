package com.tencent.supersonic.chat.domain.utils;

import static com.tencent.supersonic.common.constant.Constants.ADMIN_LOWER;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.domain.pojo.config.*;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;
import com.tencent.supersonic.chat.domain.dataobject.ChatConfigDO;
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

    public ChatConfig newChatConfig(ChatConfigBaseReq extendBaseCmd, User user) {
        ChatConfig chatConfig = new ChatConfig();
        BeanUtils.copyProperties(extendBaseCmd, chatConfig);
        RecordInfo recordInfo = new RecordInfo();
        String creator = (Objects.isNull(user) || Strings.isEmpty(user.getName())) ? ADMIN_LOWER : user.getName();
        recordInfo.createdBy(creator);
        chatConfig.setRecordInfo(recordInfo);
        chatConfig.setStatus(StatusEnum.ONLINE);
        return chatConfig;
    }


    public ChatConfig editChatConfig(ChatConfigEditReqReq extendEditCmd, User facadeUser) {
        ChatConfig chatConfig = new ChatConfig();

        BeanUtils.copyProperties(extendEditCmd, chatConfig);
        RecordInfo recordInfo = new RecordInfo();
        String user = (Objects.isNull(facadeUser) || Strings.isEmpty(facadeUser.getName()))
                ? ADMIN_LOWER : facadeUser.getName();
        recordInfo.updatedBy(user);
        chatConfig.setRecordInfo(recordInfo);
        return chatConfig;
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
        if (Objects.isNull(domainSchemaDesc) || CollectionUtils.isEmpty(domainSchemaDesc.getMetrics())) {
            return new ArrayList<>();
        }
        Map<Long, List<MetricSchemaResp>> metricIdAndDescPair = domainSchemaDesc.getMetrics()
                .stream().collect(Collectors.groupingBy(MetricSchemaResp::getId));
        return new ArrayList<>(metricIdAndDescPair.keySet());
    }

    public ChatConfigDO chatConfig2DO(ChatConfig chatConfig) {
        ChatConfigDO chatConfigDO = new ChatConfigDO();
        BeanUtils.copyProperties(chatConfig, chatConfigDO);

        chatConfigDO.setChatAggConfig(JsonUtil.toString(chatConfig.getChatAggConfig()));
        chatConfigDO.setChatDetailConfig(JsonUtil.toString(chatConfig.getChatDetailConfig()));

        if (Objects.isNull(chatConfig.getStatus())) {
            chatConfigDO.setStatus(null);
        } else {
            chatConfigDO.setStatus(chatConfig.getStatus().getCode());
        }

        chatConfigDO.setCreatedBy(chatConfig.getRecordInfo().getCreatedBy());
        chatConfigDO.setCreatedAt(chatConfig.getRecordInfo().getCreatedAt());
        chatConfigDO.setUpdatedBy(chatConfig.getRecordInfo().getUpdatedBy());
        chatConfigDO.setUpdatedAt(chatConfig.getRecordInfo().getUpdatedAt());

        return chatConfigDO;
    }

    public ChatConfigResp chatConfigDO2Descriptor(Long domainId, ChatConfigDO chatConfigDO) {
        ChatConfigResp chatConfigDescriptor = new ChatConfigResp();

        if (Objects.isNull(chatConfigDO)) {
            // deal empty chatConfigDO
            return generateEmptyChatConfigResp(domainId);
        }

        BeanUtils.copyProperties(chatConfigDO, chatConfigDescriptor);

        chatConfigDescriptor.setChatDetailConfig(JsonUtil.toObject(chatConfigDO.getChatDetailConfig(), ChatDetailConfig.class));
        chatConfigDescriptor.setChatAggConfig(JsonUtil.toObject(chatConfigDO.getChatAggConfig(), ChatAggConfig.class));
        chatConfigDescriptor.setStatusEnum(StatusEnum.of(chatConfigDO.getStatus()));

        chatConfigDescriptor.setCreatedBy(chatConfigDO.getCreatedBy());
        chatConfigDescriptor.setCreatedAt(chatConfigDO.getCreatedAt());
        chatConfigDescriptor.setUpdatedBy(chatConfigDO.getUpdatedBy());
        chatConfigDescriptor.setUpdatedAt(chatConfigDO.getUpdatedAt());


        if (Strings.isEmpty(chatConfigDO.getChatAggConfig())) {
            chatConfigDescriptor.setChatAggConfig(generateEmptyChatAggConfigResp());
        }

        if (Strings.isEmpty(chatConfigDO.getChatDetailConfig())) {
            chatConfigDescriptor.setChatDetailConfig(generateEmptyChatDetailConfigResp());
        }
        return chatConfigDescriptor;
    }

    private ChatConfigResp generateEmptyChatConfigResp(Long domainId) {
        ChatConfigResp chatConfigResp = new ChatConfigResp();
        chatConfigResp.setDomainId(domainId);
        chatConfigResp.setChatDetailConfig(generateEmptyChatDetailConfigResp());
        chatConfigResp.setChatAggConfig(generateEmptyChatAggConfigResp());
        return chatConfigResp;
    }

    private ChatDetailConfig generateEmptyChatDetailConfigResp() {
        ChatDetailConfig chatDetailConfig = new ChatDetailConfig();
        ItemVisibility visibility = new ItemVisibility();
        chatDetailConfig.setVisibility(visibility);
        return chatDetailConfig;
    }

    private ChatAggConfig generateEmptyChatAggConfigResp() {
        ChatAggConfig chatAggConfig = new ChatAggConfig();
        ItemVisibility visibility = new ItemVisibility();
        chatAggConfig.setVisibility(visibility);
        return chatAggConfig;
    }
}
