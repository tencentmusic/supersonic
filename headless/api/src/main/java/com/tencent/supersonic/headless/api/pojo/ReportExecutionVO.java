package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

import java.util.Date;
import java.util.List;

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

    // Rollup from s2_report_delivery_record, grouped by execution_id. Decoupled from `status`
    // (which
    // reflects query execution only) because a successful query may still have one channel fail.
    private List<String> channelTypes;
    private String deliveryRollup; // NONE / DELIVERED / PARTIAL / FAILED / IN_PROGRESS
    private Integer deliverySuccessCount;
    private Integer deliveryTotalCount;
}
