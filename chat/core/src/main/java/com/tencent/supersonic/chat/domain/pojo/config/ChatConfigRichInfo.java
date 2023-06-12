package com.tencent.supersonic.chat.domain.pojo.config;

import com.tencent.supersonic.common.enums.StatusEnum;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class ChatConfigRichInfo {

    private Long id;

    private Long domainId;

    private String name;
    private String bizName;

    /**
     * default metrics information about the domain
     */
    private List<DefaultMetric> defaultMetrics;

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
    private List<KnowledgeInfo> dictionaryInfos;

    /**
     * available status
     */
    private StatusEnum statusEnum;

    private String createdBy;
    private String updatedBy;
    private Date createdAt;
    private Date updatedAt;
}