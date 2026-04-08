package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class FixedReportVO {

    // --- Identity (from SemanticDeployment) ---
    private Long deploymentId;
    private Long datasetId;
    private String reportName;
    private String description;
    private String domainName;

    // --- Latest result (from most recent execution across all schedules) ---
    private Date latestResultTime;
    private String latestResultStatus; // SUCCESS / FAILED / null
    private String latestErrorMessage;
    private Long latestRowCount;
    /** Whether the last successful result is older than 2x the shortest cron interval */
    private boolean resultExpired;
    /** If latest execution failed but a previous success exists */
    private Date previousSuccessTime;

    // --- Schedule summary ---
    private int scheduleCount;
    private int enabledScheduleCount;

    // --- Delivery summary ---
    private List<DeliverySummaryItem> deliveryChannels = List.of();

    // --- Subscription ---
    private boolean subscribed;

    // --- Consumption status (derived) ---
    private String consumptionStatus;
    // AVAILABLE / NO_RESULT / EXPIRED / RECENTLY_FAILED / NO_DELIVERY / PARTIAL_CHANNEL_ERROR

    @Data
    public static class DeliverySummaryItem {
        private Long configId;
        private String configName;
        private String deliveryType;
        private boolean enabled;
    }
}
