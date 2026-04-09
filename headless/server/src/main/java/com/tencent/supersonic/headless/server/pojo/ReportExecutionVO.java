package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class ReportExecutionVO {
    private Long id;
    private Long scheduleId;
    private Integer attempt;
    private String status;
    private Date startTime;
    private Date endTime;
    private String resultLocation;
    private String errorMessage;
    private Long rowCount;
    private Long executionTimeMs;
    private Long templateVersion;

    // Extracted from executionSnapshot JSON
    private String templateName;
    private String triggerType;
    private boolean hasPreview;
}
