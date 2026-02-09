package com.tencent.supersonic.chat.server.plugin.support.reportschedule;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.plugin.PluginParseResult;
import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.plugin.support.PluginSemanticQuery;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.service.ReportScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.*;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.*;

/**
 * Plugin query implementation for report scheduling. Handles natural language requests for
 * creating, listing, canceling, and managing scheduled reports.
 */
@Slf4j
@Component
public class ReportScheduleQuery extends PluginSemanticQuery {

    public static final String QUERY_MODE = "REPORT_SCHEDULE";

    private static final SimpleDateFormat DATE_FORMATTER =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /** Pending confirmations cache, keyed by dataSetId */
    private static final ConcurrentHashMap<Long, PendingConfirmation> PENDING_CONFIRMATIONS =
            new ConcurrentHashMap<>();

    /** Confirmation expiration time: 5 minutes */
    private static final long CONFIRMATION_EXPIRE_MS = 5 * 60 * 1000;

    public ReportScheduleQuery() {
        PluginQueryManager.register(QUERY_MODE, this);
    }

    @Override
    public QueryResult build() {
        QueryResult queryResult = new QueryResult();
        queryResult.setQueryMode(QUERY_MODE);

        Map<String, Object> properties = parseInfo.getProperties();
        PluginParseResult pluginParseResult = JsonUtil.toObject(
                JsonUtil.toString(properties.get(Constants.CONTEXT)), PluginParseResult.class);

        String queryText = pluginParseResult.getQueryText();
        Long dataSetId = parseInfo.getDataSetId();

        cleanupExpiredConfirmations();

        ScheduleIntent intent = extractIntent(queryText);

        ReportScheduleResp response;
        try {
            response = switch (intent) {
                case CONFIRM -> handleConfirm(dataSetId);
                case CREATE -> handleCreate(queryText, dataSetId);
                case LIST -> handleList();
                case CANCEL -> handleCancel(queryText, dataSetId);
                case PAUSE -> handlePause(queryText);
                case RESUME -> handleResume(queryText);
                case TRIGGER -> handleTrigger(queryText);
                case STATUS -> handleStatus(queryText);
                default -> buildUnknownResponse();
            };
            queryResult.setQueryState(QueryState.SUCCESS);
        } catch (Exception e) {
            log.error("Error handling report schedule request", e);
            response = ReportScheduleResp.builder().intent(intent).success(false)
                    .message(String.format(ERROR_OPERATION_FAILED, e.getMessage())).build();
            queryResult.setQueryState(QueryState.SEARCH_EXCEPTION);
        }

        queryResult.setResponse(response);
        return queryResult;
    }

    private void cleanupExpiredConfirmations() {
        PENDING_CONFIRMATIONS.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private void savePendingConfirmation(Long dataSetId, ScheduleIntent intent,
            Map<String, Object> params) {
        if (dataSetId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        PendingConfirmation pending =
                PendingConfirmation.builder().dataSetId(dataSetId).intent(intent).params(params)
                        .createdAt(now).expireAt(now + CONFIRMATION_EXPIRE_MS).build();
        PENDING_CONFIRMATIONS.put(dataSetId, pending);
    }

    private ScheduleIntent extractIntent(String queryText) {
        if (queryText == null) {
            return ScheduleIntent.UNKNOWN;
        }

        String text = queryText.trim().toLowerCase();

        // Confirm intent - exact match
        if (CONFIRM.stream().anyMatch(text::equals)) {
            return ScheduleIntent.CONFIRM;
        }

        // Cancel intent
        if (CANCEL.stream().anyMatch(text::contains)) {
            return ScheduleIntent.CANCEL;
        }

        // Pause intent
        if (PAUSE.stream().anyMatch(text::contains)) {
            return ScheduleIntent.PAUSE;
        }

        // Resume intent
        if (ScheduleKeywords.RESUME.stream().anyMatch(text::contains)) {
            return ScheduleIntent.RESUME;
        }

        // Trigger intent
        if (TRIGGER.stream().anyMatch(text::contains)) {
            return ScheduleIntent.TRIGGER;
        }

        // Status intent
        if (STATUS.stream().anyMatch(text::contains)) {
            return ScheduleIntent.STATUS;
        }

        // List intent
        if (LIST_PREFIX.stream().anyMatch(text::contains)
                && LIST_SUFFIX.stream().anyMatch(text::contains)) {
            return ScheduleIntent.LIST;
        }

        // Create intent
        if (CREATE.stream().anyMatch(text::contains)) {
            return ScheduleIntent.CREATE;
        }

        return ScheduleIntent.UNKNOWN;
    }

    private ReportScheduleResp handleConfirm(Long dataSetId) {
        if (dataSetId == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CONFIRM).success(false)
                    .message(ERROR_SESSION_MISSING).build();
        }

        PendingConfirmation pending = PENDING_CONFIRMATIONS.remove(dataSetId);
        if (pending == null || pending.isExpired()) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CONFIRM).success(false)
                    .message(ERROR_NO_PENDING).build();
        }

        return switch (pending.getIntent()) {
            case CREATE -> executeCreate(pending.getParams());
            case CANCEL -> executeCancel(pending.getParams());
            default -> ReportScheduleResp.builder().intent(ScheduleIntent.CONFIRM).success(false)
                    .message(ERROR_UNSUPPORTED_CONFIRM).build();
        };
    }

    private ReportScheduleResp executeCreate(Map<String, Object> params) {
        Long datasetId = ((Number) params.get("datasetId")).longValue();
        String cronExpression = (String) params.get("cronExpression");

        ReportScheduleService scheduleService = ContextUtils.getBean(ReportScheduleService.class);

        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setName(String.format(SCHEDULE_NAME_TEMPLATE, describeCron(cronExpression)));
        schedule.setDatasetId(datasetId);
        schedule.setCronExpression(cronExpression);
        schedule.setEnabled(true);
        schedule.setRetryCount(3);

        ReportScheduleDO created = scheduleService.createSchedule(schedule);
        String cronDesc = describeCron(cronExpression);

        return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(true)
                .message(String.format(SUCCESS_CREATED, created.getId(), cronDesc, created.getId()))
                .scheduleId(created.getId()).cronExpression(cronExpression)
                .cronDescription(cronDesc).build();
    }

    private ReportScheduleResp executeCancel(Map<String, Object> params) {
        Long scheduleId = ((Number) params.get("scheduleId")).longValue();

        ReportScheduleService scheduleService = ContextUtils.getBean(ReportScheduleService.class);
        scheduleService.deleteSchedule(scheduleId);

        return ReportScheduleResp.builder().intent(ScheduleIntent.CANCEL).success(true)
                .message(String.format(SUCCESS_CANCELLED, scheduleId)).scheduleId(scheduleId)
                .build();
    }

    private ReportScheduleResp handleCreate(String queryText, Long dataSetId) {
        String cronExpression = parseCronExpression(queryText);

        if (cronExpression == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(false)
                    .message(ERROR_SPECIFY_FREQUENCY).needConfirm(false).build();
        }

        String cronDescription = describeCron(cronExpression);

        Map<String, Object> params = new HashMap<>();
        params.put("datasetId", dataSetId);
        params.put("cronExpression", cronExpression);
        params.put("queryText", queryText);
        savePendingConfirmation(dataSetId, ScheduleIntent.CREATE, params);

        ReportScheduleResp.ConfirmAction confirmAction = ReportScheduleResp.ConfirmAction.builder()
                .action("CREATE_SCHEDULE").params(params).build();

        return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(true)
                .message(String.format(CONFIRM_CREATE, dataSetId, cronDescription))
                .needConfirm(true).confirmAction(confirmAction).cronExpression(cronExpression)
                .cronDescription(cronDescription).build();
    }

    private ReportScheduleResp handleList() {
        ReportScheduleService scheduleService = ContextUtils.getBean(ReportScheduleService.class);

        Long datasetId = parseInfo.getDataSetId();
        Page<ReportScheduleDO> page = new Page<>(1, 20);
        Page<ReportScheduleDO> result = scheduleService.getScheduleList(page, datasetId, null);

        List<ReportScheduleResp.ScheduleSummary> summaries = new ArrayList<>();
        for (ReportScheduleDO schedule : result.getRecords()) {
            summaries.add(ReportScheduleResp.ScheduleSummary.builder().id(schedule.getId())
                    .name(schedule.getName()).datasetId(schedule.getDatasetId())
                    .cronExpression(schedule.getCronExpression())
                    .cronDescription(describeCron(schedule.getCronExpression()))
                    .enabled(schedule.getEnabled()).build());
        }

        String message;
        if (summaries.isEmpty()) {
            message = LIST_EMPTY;
        } else {
            StringBuilder sb = new StringBuilder(String.format(LIST_HEADER, summaries.size()));
            for (ReportScheduleResp.ScheduleSummary s : summaries) {
                String status =
                        Boolean.TRUE.equals(s.getEnabled()) ? STATUS_RUNNING : STATUS_PAUSED;
                sb.append(String.format(LIST_ITEM, s.getId(), s.getName(), s.getCronDescription(),
                        status));
            }
            message = sb.toString();
        }

        return ReportScheduleResp.builder().intent(ScheduleIntent.LIST).success(true)
                .message(message).schedules(summaries).build();
    }

    private ReportScheduleResp handleCancel(String queryText, Long dataSetId) {
        Long scheduleId = extractScheduleId(queryText);
        if (scheduleId == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CANCEL).success(false)
                    .message(ERROR_SPECIFY_CANCEL_ID).build();
        }

        ReportScheduleService scheduleService = ContextUtils.getBean(ReportScheduleService.class);
        ReportScheduleDO schedule = scheduleService.getScheduleById(scheduleId);

        if (schedule == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CANCEL).success(false)
                    .message(String.format(ERROR_SCHEDULE_NOT_FOUND, scheduleId)).build();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("scheduleId", scheduleId);
        savePendingConfirmation(dataSetId, ScheduleIntent.CANCEL, params);

        return ReportScheduleResp.builder().intent(ScheduleIntent.CANCEL).success(true)
                .message(String.format(CONFIRM_CANCEL, schedule.getName(), scheduleId))
                .needConfirm(true)
                .confirmAction(ReportScheduleResp.ConfirmAction.builder().action("CANCEL_SCHEDULE")
                        .params(params).build())
                .scheduleId(scheduleId).scheduleName(schedule.getName()).build();
    }

    private ReportScheduleResp handlePause(String queryText) {
        Long scheduleId = extractScheduleId(queryText);
        if (scheduleId == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.PAUSE).success(false)
                    .message(ERROR_SPECIFY_PAUSE_ID).build();
        }

        ReportScheduleService scheduleService = ContextUtils.getBean(ReportScheduleService.class);
        scheduleService.pauseSchedule(scheduleId);

        return ReportScheduleResp.builder().intent(ScheduleIntent.PAUSE).success(true)
                .message(String.format(SUCCESS_PAUSED, scheduleId, scheduleId))
                .scheduleId(scheduleId).build();
    }

    private ReportScheduleResp handleResume(String queryText) {
        Long scheduleId = extractScheduleId(queryText);
        if (scheduleId == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.RESUME).success(false)
                    .message(ERROR_SPECIFY_RESUME_ID).build();
        }

        ReportScheduleService scheduleService = ContextUtils.getBean(ReportScheduleService.class);
        scheduleService.resumeSchedule(scheduleId);

        return ReportScheduleResp.builder().intent(ScheduleIntent.RESUME).success(true)
                .message(String.format(SUCCESS_RESUMED, scheduleId)).scheduleId(scheduleId).build();
    }

    private ReportScheduleResp handleTrigger(String queryText) {
        Long scheduleId = extractScheduleId(queryText);
        if (scheduleId == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.TRIGGER).success(false)
                    .message(ERROR_SPECIFY_TRIGGER_ID).build();
        }

        ReportScheduleService scheduleService = ContextUtils.getBean(ReportScheduleService.class);
        scheduleService.triggerNow(scheduleId);

        return ReportScheduleResp.builder().intent(ScheduleIntent.TRIGGER).success(true)
                .message(String.format(SUCCESS_TRIGGERED, scheduleId)).scheduleId(scheduleId)
                .build();
    }

    private ReportScheduleResp handleStatus(String queryText) {
        Long scheduleId = extractScheduleId(queryText);
        if (scheduleId == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.STATUS).success(false)
                    .message(ERROR_SPECIFY_STATUS_ID).build();
        }

        ReportScheduleService scheduleService = ContextUtils.getBean(ReportScheduleService.class);
        Page<ReportExecutionDO> page = new Page<>(1, 10);
        Page<ReportExecutionDO> result = scheduleService.getExecutionList(page, scheduleId, null);

        List<ReportScheduleResp.ExecutionSummary> summaries = new ArrayList<>();
        for (ReportExecutionDO exec : result.getRecords()) {
            summaries.add(ReportScheduleResp.ExecutionSummary.builder().id(exec.getId())
                    .startTime(formatDate(exec.getStartTime()))
                    .endTime(formatDate(exec.getEndTime())).status(exec.getStatus())
                    .errorMessage(exec.getErrorMessage()).build());
        }

        String message;
        if (summaries.isEmpty()) {
            message = String.format(EXECUTION_EMPTY, scheduleId);
        } else {
            StringBuilder sb = new StringBuilder(String.format(EXECUTION_HEADER, scheduleId));
            for (ReportScheduleResp.ExecutionSummary e : summaries) {
                String statusIcon = "SUCCESS".equals(e.getStatus()) ? "✓" : "✗";
                sb.append("\n").append(statusIcon).append(" ").append(e.getStartTime());
                if (e.getErrorMessage() != null) {
                    sb.append(String.format(EXECUTION_ERROR, e.getErrorMessage()));
                }
            }
            message = sb.toString();
        }

        return ReportScheduleResp.builder().intent(ScheduleIntent.STATUS).success(true)
                .message(message).scheduleId(scheduleId).executions(summaries).build();
    }

    private ReportScheduleResp buildUnknownResponse() {
        return ReportScheduleResp.builder().intent(ScheduleIntent.UNKNOWN).success(false)
                .message(UNKNOWN_INTENT).build();
    }

    private Long extractScheduleId(String queryText) {
        if (queryText == null) {
            return null;
        }
        Matcher matcher = SCHEDULE_ID_PATTERN.matcher(queryText);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }

    private String parseCronExpression(String queryText) {
        if (queryText == null) {
            return null;
        }

        String baseCron = null;
        for (Map.Entry<String, String> entry : CRON_PATTERNS.entrySet()) {
            if (queryText.contains(entry.getKey())) {
                baseCron = entry.getValue();
                break;
            }
        }

        if (baseCron == null) {
            return null;
        }

        // Try to extract specific time
        Matcher timeMatcher = TIME_PATTERN.matcher(queryText);
        if (timeMatcher.find()) {
            String period = timeMatcher.group(1);
            int hour = Integer.parseInt(timeMatcher.group(2));

            // Adjust hour based on period
            if (period != null && AFTERNOON.contains(period) && hour < 12) {
                hour += 12;
            }

            // Replace hour in cron expression
            String[] parts = baseCron.split(" ");
            parts[2] = String.valueOf(hour);
            return String.join(" ", parts);
        }

        return baseCron;
    }

    private String describeCron(String cronExpression) {
        if (cronExpression == null) {
            return CRON_UNKNOWN;
        }

        String[] parts = cronExpression.split(" ");
        if (parts.length < 6) {
            return cronExpression;
        }

        String minute = parts[1];
        String hour = parts[2];
        String dayOfMonth = parts[3];
        String dayOfWeek = parts[5];

        StringBuilder desc = new StringBuilder();

        if (!"?".equals(dayOfWeek) && !"*".equals(dayOfWeek)) {
            desc.append(String.format(CRON_WEEKLY, DAY_OF_WEEK.getOrDefault(dayOfWeek, dayOfWeek)));
        } else if (!"*".equals(dayOfMonth) && !"?".equals(dayOfMonth)) {
            desc.append(String.format(CRON_MONTHLY, dayOfMonth));
        } else if ("*".equals(hour)) {
            return CRON_HOURLY;
        } else {
            desc.append(CRON_DAILY);
        }

        desc.append(String.format("%02d:%02d", Integer.parseInt(hour), Integer.parseInt(minute)));

        return desc.toString();
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        synchronized (DATE_FORMATTER) {
            return DATE_FORMATTER.format(date);
        }
    }
}
