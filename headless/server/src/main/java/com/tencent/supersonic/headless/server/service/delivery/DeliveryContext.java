package com.tencent.supersonic.headless.server.service.delivery;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Context information for report delivery.
 */
@Data
@Builder
public class DeliveryContext {
    private Long scheduleId;
    private Long executionId;
    private String scheduleName;
    private String reportName;
    private String fileLocation;
    private String outputFormat;
    private Long rowCount;
    private Long tenantId;
    private String executionTime;

    // Alert-specific fields (non-null when delivering alert notifications)
    private Long alertRuleId; // Non-null when this is an alert delivery; use instead of scheduleId
    private String alertContent; // Non-null when this is an alert delivery (not a report delivery)
    private String alertSeverity; // Highest severity: "CRITICAL" or "WARNING"
    private Integer alertedCount; // Number of triggered alert events
    private Integer totalChecked; // Total rows evaluated
    private String alertRuleName; // Name of the alert rule
    private List<Long> alertEventIds;
}
