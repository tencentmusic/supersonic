package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;

@Data
public class ReportScheduleReq {
    private Long id;
    private String name;
    private Long datasetId;
    private String queryConfig;
    private String outputFormat;
    private String cronExpression;
    private Boolean enabled;
    private Long ownerId;
    private Integer retryCount;
    private Integer retryInterval;
    private Long templateVersion;
    private String deliveryConfigIds;
}
