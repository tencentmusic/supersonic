package com.tencent.supersonic.chat.config;

import lombok.Data;

import java.util.List;

@Data
public class ChatAggRichConfig {

    /**
     * invisible dimensions/metrics
     */
    private ItemVisibilityInfo visibility;

    /**
     * information about dictionary about the domain
     */
    private List<KnowledgeInfo> knowledgeInfos;

    private KnowledgeAdvancedConfig globalKnowledgeConfig;

    private ChatDefaultRichConfig chatDefaultConfig;

}