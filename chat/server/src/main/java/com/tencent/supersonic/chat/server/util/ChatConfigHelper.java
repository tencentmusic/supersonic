package com.tencent.supersonic.chat.server.util;

import static com.tencent.supersonic.common.pojo.Constants.ADMIN_LOWER;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.request.ChatAggConfigReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigBaseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigEditReqReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatDetailConfigReq;
import com.tencent.supersonic.chat.api.pojo.request.ItemVisibility;
import com.tencent.supersonic.chat.api.pojo.request.RecommendedQuestionReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;
import com.tencent.supersonic.chat.server.config.ChatConfig;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatConfigDO;
import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.util.JsonUtil;
import java.util.ArrayList;
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
public class ChatConfigHelper {

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

    public List<Long> generateAllDimIdList(DataSetSchema modelSchema) {
        if (Objects.isNull(modelSchema) || CollectionUtils.isEmpty(modelSchema.getDimensions())) {
            return new ArrayList<>();
        }
        Map<Long, List<SchemaElement>> dimIdAndDescPair = modelSchema.getDimensions()
                .stream().collect(Collectors.groupingBy(SchemaElement::getId));
        return new ArrayList<>(dimIdAndDescPair.keySet());
    }

    public List<Long> generateAllMetricIdList(DataSetSchema modelSchema) {
        if (Objects.isNull(modelSchema) || CollectionUtils.isEmpty(modelSchema.getMetrics())) {
            return new ArrayList<>();
        }
        Map<Long, List<SchemaElement>> metricIdAndDescPair = modelSchema.getMetrics()
                .stream().collect(Collectors.groupingBy(SchemaElement::getId));
        return new ArrayList<>(metricIdAndDescPair.keySet());
    }

    public ChatConfigDO chatConfig2DO(ChatConfig chatConfig) {
        ChatConfigDO chatConfigDO = new ChatConfigDO();
        BeanUtils.copyProperties(chatConfig, chatConfigDO);

        chatConfigDO.setChatAggConfig(JsonUtil.toString(chatConfig.getChatAggConfig()));
        chatConfigDO.setChatDetailConfig(JsonUtil.toString(chatConfig.getChatDetailConfig()));
        chatConfigDO.setRecommendedQuestions(JsonUtil.toString(chatConfig.getRecommendedQuestions()));

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

    public ChatConfigResp chatConfigDO2Descriptor(Long modelId, ChatConfigDO chatConfigDO) {
        ChatConfigResp chatConfigDescriptor = new ChatConfigResp();

        if (Objects.isNull(chatConfigDO)) {
            // deal empty chatConfigDO
            return generateEmptyChatConfigResp(modelId);
        }

        BeanUtils.copyProperties(chatConfigDO, chatConfigDescriptor);

        chatConfigDescriptor.setChatDetailConfig(
                JsonUtil.toObject(chatConfigDO.getChatDetailConfig(), ChatDetailConfigReq.class));
        chatConfigDescriptor.setChatAggConfig(
                JsonUtil.toObject(chatConfigDO.getChatAggConfig(), ChatAggConfigReq.class));
        chatConfigDescriptor.setRecommendedQuestions(
                JsonUtil.toList(chatConfigDO.getRecommendedQuestions(), RecommendedQuestionReq.class));
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

    private ChatConfigResp generateEmptyChatConfigResp(Long modelId) {
        ChatConfigResp chatConfigResp = new ChatConfigResp();
        chatConfigResp.setModelId(modelId);
        chatConfigResp.setChatDetailConfig(generateEmptyChatDetailConfigResp());
        chatConfigResp.setChatAggConfig(generateEmptyChatAggConfigResp());
        return chatConfigResp;
    }

    private ChatDetailConfigReq generateEmptyChatDetailConfigResp() {
        ChatDetailConfigReq chatDetailConfig = new ChatDetailConfigReq();
        ItemVisibility visibility = new ItemVisibility();
        chatDetailConfig.setVisibility(visibility);
        return chatDetailConfig;
    }

    private ChatAggConfigReq generateEmptyChatAggConfigResp() {
        ChatAggConfigReq chatAggConfig = new ChatAggConfigReq();
        ItemVisibility visibility = new ItemVisibility();
        chatAggConfig.setVisibility(visibility);
        return chatAggConfig;
    }
}
