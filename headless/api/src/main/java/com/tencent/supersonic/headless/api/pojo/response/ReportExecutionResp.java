package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ReportExecutionResp {
    private Long id;
    private Long scheduleId;
    private Integer attempt;
    private String status;
    private Date startTime;
    private Date endTime;
    private String resultLocation;
    private String errorMessage;
    private Long rowCount;
    private String sqlHash;
    private Long tenantId;
    private String executionSnapshot;
    private Long templateVersion;
    private String engineVersion;
    private Long scanRows;
    private Long executionTimeMs;
    private Long ioBytes;
}
