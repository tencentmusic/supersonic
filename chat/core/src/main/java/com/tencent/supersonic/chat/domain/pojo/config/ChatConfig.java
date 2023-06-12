package com.tencent.supersonic.chat.domain.pojo.config;

import com.tencent.supersonic.common.enums.StatusEnum;
import com.tencent.supersonic.common.util.RecordInfo;
import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ChatConfig {

    /**
     * database auto-increment primary key
     */
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
    private List<KnowledgeInfo> knowledgeInfos;

    /**
     * available status
     */
    private StatusEnum status;

    /**
     * about createdBy, createdAt, updatedBy, updatedAt
     */
    private RecordInfo recordInfo;

}