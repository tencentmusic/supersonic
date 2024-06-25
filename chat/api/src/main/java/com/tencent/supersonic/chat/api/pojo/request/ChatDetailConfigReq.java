package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;

import java.util.List;

@Data
public class ChatDetailConfigReq {

    /**
     * invisible dimensions/metrics
     * 一些指标维度不展示
     */
    private ItemVisibility visibility;

    /**
     * information about dictionary about the model
     *一些模型的字典信息
     */
    private List<KnowledgeInfoReq> knowledgeInfos;

    private KnowledgeAdvancedConfig globalKnowledgeConfig;

    private ChatDefaultConfigReq chatDefaultConfig;

}