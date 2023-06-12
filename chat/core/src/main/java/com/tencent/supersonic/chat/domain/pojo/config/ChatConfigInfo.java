package com.tencent.supersonic.chat.domain.pojo.config;

import com.tencent.supersonic.common.enums.StatusEnum;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class ChatConfigInfo {

    private Long id;

    private Long domainId;

    /**
     * default metrics information about the domain
     */
    private List<DefaultMetricInfo> defaultMetrics;

    /**
     * invisible dimensions/metrics
     */
    private ItemVisibility visibility;

    /**
     * the entity info about the domain
     */
    private Entity entity;

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