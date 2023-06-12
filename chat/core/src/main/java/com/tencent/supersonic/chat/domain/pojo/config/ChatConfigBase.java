package com.tencent.supersonic.chat.domain.pojo.config;

import com.tencent.supersonic.common.enums.StatusEnum;
import java.util.List;
import lombok.Data;
import lombok.ToString;

/**
 * extended information command about domain
 */
@Data
@ToString
public class ChatConfigBase {

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
    private StatusEnum status;

}