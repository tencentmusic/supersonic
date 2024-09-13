package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;

import java.util.List;

@Data
public class ChatAggConfigReq {

    /** invisible dimensions/metrics */
    private ItemVisibility visibility;

    /** information about dictionary about the model */
    private List<KnowledgeInfoReq> knowledgeInfos;

    private KnowledgeAdvancedConfig globalKnowledgeConfig;

    private ChatDefaultConfigReq chatDefaultConfig;
}
