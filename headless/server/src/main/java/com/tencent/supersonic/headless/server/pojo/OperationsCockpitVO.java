package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Aggregated read model for the daily operations cockpit (see product doc §5.1).
 */
@Data
public class OperationsCockpitVO {

    private List<TopicSummary> topics = new ArrayList<>();
    /** Fixed reports prioritized for "today" — subscribed first, then at-risk highlights */
    private List<FixedReportSummary> keyReports = new ArrayList<>();
    /** Alert events pending resolution */
    private List<AlertEventSummary> pendingAlertEvents = new ArrayList<>();
    /** Reports with reliability consumption issues */
    private List<FixedReportSummary> reliabilityRisks = new ArrayList<>();

    private long pendingAlertEventCount;

    @Data
    public static class TopicSummary {
        private Long id;
        private String name;
        private String description;
        private Integer priority;
        private int fixedReportCount;
        private int alertRuleCount;
        private int scheduleCount;
    }

    @Data
    public static class FixedReportSummary {
        private Long datasetId;
        private String reportName;
        private String domainName;
        private String consumptionStatus;
        private Date latestResultTime;
        private boolean subscribed;
    }

    @Data
    public static class AlertEventSummary {
        private Long id;
        private Long ruleId;
        private String severity;
        private String resolutionStatus;
        private String message;
        private Date createdAt;
    }
}
