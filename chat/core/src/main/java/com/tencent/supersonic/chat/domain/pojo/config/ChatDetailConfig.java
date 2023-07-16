package com.tencent.supersonic.chat.domain.pojo.config;

import lombok.Data;

import java.util.List;

@Data
public class ChatDetailConfig {

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

    /**
     * the entity info about the domain
     */
    private Entity entity;

}