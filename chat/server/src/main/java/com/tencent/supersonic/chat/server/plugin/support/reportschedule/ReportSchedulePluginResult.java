package com.tencent.supersonic.chat.server.plugin.support.reportschedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response model for report schedule plugin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSchedulePluginResult {

    private ScheduleIntent intent;
    private String message;
    private boolean success;

    /** Whether user confirmation is needed before executing the action */
    private boolean needConfirm;

    /** Action to execute after user confirms */
    private ConfirmAction confirmAction;

    // CREATE response fields
    private Long scheduleId;
    private String scheduleName;
    private String cronExpression;
    private String cronDescription;
    private String nextExecutionTime;

    // LIST response fields
    private List<ScheduleSummary> schedules;

    // STATUS response fields
    private List<ExecutionSummary> executions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleSummary {
        private Long id;
        private String name;
        private Long datasetId;
        private String datasetName;
        private String cronExpression;
        private String cronDescription;
        private Boolean enabled;
        private String lastExecutionTime;
        private String lastExecutionStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionSummary {
        private Long id;
        private String startTime;
        private String endTime;
        private String status;
        private String errorMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmAction {
        private String action;
        private Map<String, Object> params;
    }
}
