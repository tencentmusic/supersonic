package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ReportDeliveryRecordResp {
    private Long id;
    private String deliveryKey;
    private Long scheduleId;
    private Long executionId;
    private Long configId;
    private String deliveryType;
    private String status;
    private String fileLocation;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetries;
    private Date nextRetryAt;
    private Date startedAt;
    private Date completedAt;
    private Long deliveryTimeMs;
    private Long tenantId;
    private Date createdAt;
}
