package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ReportDeliveryConfigResp {
    private Long id;
    private String name;
    private String deliveryType;
    private String deliveryConfig;
    private Boolean enabled;
    private String description;
    private Integer consecutiveFailures;
    private Integer maxConsecutiveFailures;
    private String disabledReason;
    private Long tenantId;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
    private String updatedBy;
}
