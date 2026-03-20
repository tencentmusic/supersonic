package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for execution snapshot (audit replay).
 * <p>
 * Assembled from s2_report_execution.execution_snapshot (JSON) + s2_report_delivery_record rows.
 */
@Data
public class ExecutionSnapshotResp {

    private Long executionId;

    /** Template / schedule name at the time of execution. */
    private String templateName;

    /** Semantic template version at the time of execution. */
    private Long templateVersion;

    /** Trigger type: SCHEDULE / MANUAL / WEB / AGENT / API */
    private String triggerType;

    /** Wall-clock start time of the execution. */
    private Date executedAt;

    /** Total wall-clock duration in milliseconds. */
    private Long durationMs;

    /** Final status: SUCCESS / FAILED / RUNNING / PENDING */
    private String status;

    /**
     * Resolved parameters used during this execution. Sourced from
     * ReportExecutionContext.resolvedParams.
     */
    private Map<String, Object> params;

    /**
     * Executed SQL (desensitised: JDBC connection strings replaced by [DB:***]). May be null for
     * structured-query executions where no raw SQL was captured.
     */
    private String sql;

    /** Total number of result rows produced. */
    private Long resultRowCount;

    /**
     * Preview of up to 20 result rows, column-filtered by the requesting user's column-level
     * permissions (columns the user cannot see are omitted).
     */
    private List<Map<String, Object>> resultPreview;

    /** Push / delivery records associated with this execution. */
    private List<DeliveryRecordItem> deliveryRecords;

    // -------------------------------------------------------------------------

    @Data
    public static class DeliveryRecordItem {
        private Long recordId;
        private String channelType;
        private String status;
        private Date deliveredAt;
        private String errorMessage;
    }
}
