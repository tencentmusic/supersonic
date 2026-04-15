package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ReportScheduleResp {
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
    private String quartzJobKey;
    private Date lastExecutionTime;
    private Date nextExecutionTime;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
    private Long tenantId;
}
