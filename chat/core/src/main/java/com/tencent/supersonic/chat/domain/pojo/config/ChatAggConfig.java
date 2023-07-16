package com.tencent.supersonic.chat.domain.pojo.config;

import lombok.Data;

import java.util.List;

@Data
public class ChatAggConfig {

    /**
     * invisible dimensions/metrics
     */
    private ItemVisibility visibility;

    /**
     * information about dictionary about the domain
     */
    private List<KnowledgeInfo> knowledgeInfos;

    private KnowledgeAdvancedConfig globalKnowledgeConfig;

    private ChatDefaultConfig chatDefaultConfig;

}