package com.tencent.supersonic.chat.config;

import lombok.Data;

import java.util.List;

@Data
public class ChatDetailRichConfig {

    /**
     * invisible dimensions/metrics
     */
    private ItemVisibilityInfo visibility;

    /**
     * the entity info about the domain
     */
    private EntityRichInfo entity;

    /**
     * information about dictionary about the domain
     */
    private List<KnowledgeInfo> knowledgeInfos;

    private KnowledgeAdvancedConfig globalKnowledgeConfig;

    private ChatDefaultRichConfig chatDefaultConfig;


}