package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class BusinessTopicVO {
    private Long id;
    private String name;
    private String description;
    private Integer priority;
    private Long ownerId;
    private String ownerName;
    private String defaultDeliveryConfigIds;
    private Integer enabled;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;

    // Aggregated counts
    private int fixedReportCount;
    private int alertRuleCount;
    private int scheduleCount;

    // Item details (populated on detail view)
    private List<TopicItem> items;

    @Data
    public static class TopicItem {
        private Long itemId;
        private String itemType;
        private String itemName;
    }
}
