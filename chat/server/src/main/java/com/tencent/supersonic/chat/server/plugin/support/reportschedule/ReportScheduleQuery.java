package com.tencent.supersonic.chat.server.plugin.support.reportschedule;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.chat.api.plugin.PluginParseResult;
import com.tencent.supersonic.chat.api.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.service.ChatContextService;
import com.tencent.supersonic.chat.server.plugin.support.PluginSemanticQuery;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.chat.utils.QueryReqBuilder;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.service.ReportDeliveryService;
import com.tencent.supersonic.headless.server.service.ReportScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.AFTERNOON;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.CANCEL;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.CONFIRM;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.CREATE;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.CRON_PATTERNS;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.DAY_OF_WEEK;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.LIST_PREFIX;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.LIST_SUFFIX;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.PAUSE;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.SCHEDULE_ID_PATTERN;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.STATUS;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.TIME_PATTERN;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.TRIGGER;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.CONFIRM_CANCEL;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.CONFIRM_CREATE;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.CRON_DAILY;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.CRON_HOURLY;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.CRON_MONTHLY;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.CRON_UNKNOWN;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.CRON_WEEKLY;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.ERROR_NO_DELIVERY_CONFIG;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.ERROR_NO_PENDING;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.ERROR_NO_PERMISSION;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.ERROR_OPERATION_FAILED;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.ERROR_SCHEDULE_NOT_FOUND;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.ERROR_SESSION_MISSING;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.ERROR_SPECIFY_CANCEL_ID;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.ERROR_SPECIFY_FREQUENCY;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.ERROR_SPECIFY_PAUSE_ID;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.ERROR_SPECIFY_REPORT_CONTENT;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.ERROR_SPECIFY_RESUME_ID;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.ERROR_SPECIFY_STATUS_ID;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.ERROR_SPECIFY_TRIGGER_ID;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.ERROR_UNSUPPORTED_CONFIRM;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.ERROR_UNSUPPORTED_REPORT_CONTEXT;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.EXECUTION_EMPTY;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.EXECUTION_ERROR;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.EXECUTION_HEADER;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.LIST_EMPTY;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.LIST_HEADER;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.LIST_ITEM;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.SCHEDULE_NAME_TEMPLATE;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.STATUS_PAUSED;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.STATUS_RUNNING;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.SUCCESS_CANCELLED;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.SUCCESS_CREATED;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.SUCCESS_PAUSED;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.SUCCESS_RESUMED;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.SUCCESS_TRIGGERED;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.UNKNOWN_INTENT;

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

    /** Pending confirmations cache, keyed by "userId_dataSetId" to avoid cross-user collisions */
    private static final ConcurrentHashMap<String, PendingConfirmation> PENDING_CONFIRMATIONS =
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
        Long currentUserId = pluginParseResult.getUserId();

        cleanupExpiredConfirmations();

        ScheduleIntent intent = extractIntent(queryText);

        ReportScheduleResp response;
        try {
            response = switch (intent) {
                case CONFIRM -> handleConfirm(dataSetId, currentUserId);
                case CREATE -> handleCreate(queryText, dataSetId, pluginParseResult, currentUserId);
                case LIST -> handleList();
                case CANCEL -> handleCancel(queryText, dataSetId, currentUserId);
                case PAUSE -> handlePause(queryText, currentUserId);
                case RESUME -> handleResume(queryText, currentUserId);
                case TRIGGER -> handleTrigger(queryText, currentUserId);
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
            Map<String, Object> params, Long userId) {
        if (dataSetId == null) {
            return;
        }
        String key = (userId != null ? userId : 0L) + "_" + dataSetId;
        long now = System.currentTimeMillis();
        PendingConfirmation pending =
                PendingConfirmation.builder().dataSetId(dataSetId).intent(intent).params(params)
                        .createdAt(now).expireAt(now + CONFIRMATION_EXPIRE_MS).build();
        PENDING_CONFIRMATIONS.put(key, pending);
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

        // Status intent
        if (STATUS.stream().anyMatch(text::contains)) {
            return ScheduleIntent.STATUS;
        }

        // List intent (checked before TRIGGER to avoid "现在有哪些" being misclassified)
        if (LIST_PREFIX.stream().anyMatch(text::contains)
                && LIST_SUFFIX.stream().anyMatch(text::contains)) {
            return ScheduleIntent.LIST;
        }

        // Create intent
        if (CREATE.stream().anyMatch(text::contains)) {
            return ScheduleIntent.CREATE;
        }

        // Trigger intent (checked last among action intents — keywords like "现在/立即" are common)
        if (TRIGGER.stream().anyMatch(text::contains)) {
            return ScheduleIntent.TRIGGER;
        }

        return ScheduleIntent.UNKNOWN;
    }

    private ReportScheduleResp handleConfirm(Long dataSetId, Long userId) {
        if (dataSetId == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CONFIRM).success(false)
                    .message(ERROR_SESSION_MISSING).build();
        }

        String key = (userId != null ? userId : 0L) + "_" + dataSetId;
        PendingConfirmation pending = PENDING_CONFIRMATIONS.remove(key);
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
        Object rawDatasetId = params.get("datasetId");
        if (!(rawDatasetId instanceof Number)) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(false)
                    .message(String.format(ERROR_OPERATION_FAILED, "datasetId missing")).build();
        }
        Long datasetId = ((Number) rawDatasetId).longValue();
        String cronExpression = (String) params.get("cronExpression");
        String outputFormat = (String) params.getOrDefault("outputFormat", "EXCEL");
        String queryConfig = (String) params.get("queryConfig");
        String deliveryConfigIds = (String) params.get("deliveryConfigIds");
        Long ownerId = params.get("ownerId")instanceof Number number ? number.longValue() : null;
        Long tenantId = params.get("tenantId")instanceof Number number ? number.longValue() : null;
        String createdBy = (String) params.get("createdBy");
        String scheduleName = (String) params.get("scheduleName");

        ReportScheduleService scheduleService = ContextUtils.getBean(ReportScheduleService.class);

        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setName(scheduleName);
        schedule.setDatasetId(datasetId);
        schedule.setCronExpression(cronExpression);
        schedule.setQueryConfig(queryConfig);
        schedule.setOutputFormat(outputFormat);
        schedule.setDeliveryConfigIds(deliveryConfigIds);
        schedule.setEnabled(true);
        schedule.setRetryCount(3);
        schedule.setOwnerId(ownerId);
        schedule.setTenantId(tenantId);
        schedule.setCreatedBy(createdBy);

        ReportScheduleDO created = scheduleService.createSchedule(schedule);
        String cronDesc = describeCron(cronExpression);

        return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(true)
                .message(String.format(SUCCESS_CREATED, created.getId(), cronDesc, created.getId()))
                .scheduleId(created.getId()).cronExpression(cronExpression)
                .cronDescription(cronDesc).build();
    }

    private ReportScheduleResp executeCancel(Map<String, Object> params) {
        Object rawScheduleId = params.get("scheduleId");
        if (!(rawScheduleId instanceof Number)) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CANCEL).success(false)
                    .message(String.format(ERROR_OPERATION_FAILED, "scheduleId missing")).build();
        }
        Long scheduleId = ((Number) rawScheduleId).longValue();

        ReportScheduleService scheduleService = ContextUtils.getBean(ReportScheduleService.class);
        scheduleService.deleteSchedule(scheduleId);

        return ReportScheduleResp.builder().intent(ScheduleIntent.CANCEL).success(true)
                .message(String.format(SUCCESS_CANCELLED, scheduleId)).scheduleId(scheduleId)
                .build();
    }

    private ReportScheduleResp handleCreate(String queryText, Long dataSetId,
            PluginParseResult pluginParseResult, Long currentUserId) {
        String cronExpression = parseCronExpression(queryText);

        if (cronExpression == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(false)
                    .message(ERROR_SPECIFY_FREQUENCY).needConfirm(false).build();
        }

        SemanticParseInfo baseParseInfo = resolveBaseParseInfo(pluginParseResult);
        if (baseParseInfo == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(false)
                    .message(ERROR_SPECIFY_REPORT_CONTENT).needConfirm(false).build();
        }
        Long scheduleDatasetId = baseParseInfo.getDataSetId();

        String queryConfig = buildQueryConfig(baseParseInfo);
        if (queryConfig == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(false)
                    .message(ERROR_UNSUPPORTED_REPORT_CONTEXT).needConfirm(false).build();
        }

        String deliveryConfigIds = resolveDeliveryConfigIds(pluginParseResult);
        if (StringUtils.isBlank(deliveryConfigIds)) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(false)
                    .message(ERROR_NO_DELIVERY_CONFIG).needConfirm(false).build();
        }

        String cronDescription = describeCron(cronExpression);
        String outputFormat = parseOutputFormat(queryText);
        String scheduleName = buildScheduleName(pluginParseResult, cronDescription);

        Map<String, Object> params = new HashMap<>();
        params.put("datasetId", scheduleDatasetId);
        params.put("cronExpression", cronExpression);
        params.put("queryText", queryText);
        params.put("queryConfig", queryConfig);
        params.put("outputFormat", outputFormat);
        params.put("deliveryConfigIds", deliveryConfigIds);
        params.put("scheduleName", scheduleName);
        params.put("ownerId", pluginParseResult.getUserId());
        params.put("tenantId", pluginParseResult.getTenantId());
        params.put("createdBy", pluginParseResult.getUserName());
        savePendingConfirmation(scheduleDatasetId, ScheduleIntent.CREATE, params, currentUserId);

        ReportScheduleResp.ConfirmAction confirmAction = ReportScheduleResp.ConfirmAction.builder()
                .action("CREATE_SCHEDULE").params(params).build();

        return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(true)
                .message(String.format(CONFIRM_CREATE, scheduleDatasetId, cronDescription))
                .needConfirm(true).confirmAction(confirmAction).cronExpression(cronExpression)
                .cronDescription(cronDescription).build();
    }

    private ReportScheduleResp handleList() {
        ReportScheduleService scheduleService = ContextUtils.getBean(ReportScheduleService.class);

        // Pass null to list ALL schedules for the current tenant (TenantSqlInterceptor filters by
        // tenant)
        Page<ReportScheduleDO> page = new Page<>(1, 20);
        Page<ReportScheduleDO> result = scheduleService.getScheduleList(page, null, null);

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

    private ReportScheduleResp handleCancel(String queryText, Long dataSetId, Long currentUserId) {
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
        savePendingConfirmation(dataSetId, ScheduleIntent.CANCEL, params, currentUserId);

        return ReportScheduleResp.builder().intent(ScheduleIntent.CANCEL).success(true)
                .message(String.format(CONFIRM_CANCEL, schedule.getName(), scheduleId))
                .needConfirm(true)
                .confirmAction(ReportScheduleResp.ConfirmAction.builder().action("CANCEL_SCHEDULE")
                        .params(params).build())
                .scheduleId(scheduleId).scheduleName(schedule.getName()).build();
    }

    private ReportScheduleResp handlePause(String queryText, Long currentUserId) {
        Long scheduleId = extractScheduleId(queryText);
        if (scheduleId == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.PAUSE).success(false)
                    .message(ERROR_SPECIFY_PAUSE_ID).build();
        }

        ReportScheduleService scheduleService = ContextUtils.getBean(ReportScheduleService.class);
        ReportScheduleDO schedule = scheduleService.getScheduleById(scheduleId);
        if (schedule == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.PAUSE).success(false)
                    .message(String.format(ERROR_SCHEDULE_NOT_FOUND, scheduleId)).build();
        }
        if (schedule.getOwnerId() != null && currentUserId != null
                && !schedule.getOwnerId().equals(currentUserId)) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.PAUSE).success(false)
                    .message(ERROR_NO_PERMISSION).build();
        }

        scheduleService.pauseSchedule(scheduleId);

        return ReportScheduleResp.builder().intent(ScheduleIntent.PAUSE).success(true)
                .message(String.format(SUCCESS_PAUSED, scheduleId, scheduleId))
                .scheduleId(scheduleId).build();
    }

    private ReportScheduleResp handleResume(String queryText, Long currentUserId) {
        Long scheduleId = extractScheduleId(queryText);
        if (scheduleId == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.RESUME).success(false)
                    .message(ERROR_SPECIFY_RESUME_ID).build();
        }

        ReportScheduleService scheduleService = ContextUtils.getBean(ReportScheduleService.class);
        ReportScheduleDO schedule = scheduleService.getScheduleById(scheduleId);
        if (schedule == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.RESUME).success(false)
                    .message(String.format(ERROR_SCHEDULE_NOT_FOUND, scheduleId)).build();
        }
        if (schedule.getOwnerId() != null && currentUserId != null
                && !schedule.getOwnerId().equals(currentUserId)) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.RESUME).success(false)
                    .message(ERROR_NO_PERMISSION).build();
        }

        scheduleService.resumeSchedule(scheduleId);

        return ReportScheduleResp.builder().intent(ScheduleIntent.RESUME).success(true)
                .message(String.format(SUCCESS_RESUMED, scheduleId)).scheduleId(scheduleId).build();
    }

    private ReportScheduleResp handleTrigger(String queryText, Long currentUserId) {
        Long scheduleId = extractScheduleId(queryText);
        if (scheduleId == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.TRIGGER).success(false)
                    .message(ERROR_SPECIFY_TRIGGER_ID).build();
        }

        ReportScheduleService scheduleService = ContextUtils.getBean(ReportScheduleService.class);
        ReportScheduleDO schedule = scheduleService.getScheduleById(scheduleId);
        if (schedule == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.TRIGGER).success(false)
                    .message(String.format(ERROR_SCHEDULE_NOT_FOUND, scheduleId)).build();
        }
        if (schedule.getOwnerId() != null && currentUserId != null
                && !schedule.getOwnerId().equals(currentUserId)) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.TRIGGER).success(false)
                    .message(ERROR_NO_PERMISSION).build();
        }

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

    private SemanticParseInfo resolveBaseParseInfo(PluginParseResult pluginParseResult) {
        if (pluginParseResult == null || pluginParseResult.getChatId() == null) {
            return null;
        }
        ChatContextService chatContextService = ContextUtils.getBean(ChatContextService.class);
        ChatContext chatContext =
                chatContextService.getOrCreateContext(pluginParseResult.getChatId());
        if (chatContext == null || chatContext.getParseInfo() == null) {
            return null;
        }
        SemanticParseInfo baseParseInfo = chatContext.getParseInfo();
        return isSchedulable(baseParseInfo) ? baseParseInfo : null;
    }

    private boolean isSchedulable(SemanticParseInfo parseInfo) {
        if (parseInfo == null || parseInfo.getDataSetId() == null) {
            return false;
        }
        if (QUERY_MODE.equals(parseInfo.getQueryMode())) {
            return false;
        }
        if (parseInfo.getSqlInfo() != null && (org.apache.commons.lang3.StringUtils
                .isNotBlank(parseInfo.getSqlInfo().getQuerySQL())
                || org.apache.commons.lang3.StringUtils
                        .isNotBlank(parseInfo.getSqlInfo().getCorrectedS2SQL()))) {
            return true;
        }
        return parseInfo.getMetrics() != null && !parseInfo.getMetrics().isEmpty();
    }

    private String buildQueryConfig(SemanticParseInfo parseInfo) {
        if (parseInfo == null || parseInfo.getDataSetId() == null) {
            return null;
        }
        if (parseInfo.getQueryType() != null
                && com.tencent.supersonic.common.pojo.enums.QueryType.AGGREGATE
                        .equals(parseInfo.getQueryType())
                && parseInfo.getMetrics() != null && !parseInfo.getMetrics().isEmpty()) {
            QueryStructReq structReq = QueryReqBuilder.buildStructReq(parseInfo);
            structReq.setDataSetId(parseInfo.getDataSetId());
            return JsonUtil.toString(structReq);
        }
        // SQL-based 查询含硬编码时间过滤，不支持直接订阅，返回 null 触发 ERROR_UNSUPPORTED_REPORT_CONTEXT
        // if (parseInfo.getSqlInfo() != null) { ... }
        if (parseInfo.getMetrics() != null && !parseInfo.getMetrics().isEmpty()) {
            QueryStructReq structReq = QueryReqBuilder.buildStructReq(parseInfo);
            structReq.setDataSetId(parseInfo.getDataSetId());
            return JsonUtil.toString(structReq);
        }
        return null;
    }

    private String parseOutputFormat(String queryText) {
        if (queryText == null) {
            return "EXCEL";
        }
        String lower = queryText.toLowerCase();
        if (lower.contains("csv")) {
            return "CSV";
        }
        return "EXCEL";
    }

    private String resolveDeliveryConfigIds(PluginParseResult pluginParseResult) {
        if (pluginParseResult == null || pluginParseResult.getTenantId() == null) {
            return null;
        }
        ReportDeliveryService deliveryService = ContextUtils.getBean(ReportDeliveryService.class);
        List<ReportDeliveryConfigDO> configs = deliveryService.getConfigList(new Page<>(1, 100))
                .getRecords().stream().filter(config -> Boolean.TRUE.equals(config.getEnabled()))
                .filter(config -> pluginParseResult.getTenantId().equals(config.getTenantId()))
                .toList();
        if (configs.isEmpty()) {
            return null;
        }
        // Use only the first (default) enabled delivery config.
        // Silently adding all configs would push to unintended channels when the tenant has many.
        return String.valueOf(configs.get(0).getId());
    }

    private String buildScheduleName(PluginParseResult pluginParseResult, String cronDescription) {
        String base = pluginParseResult != null ? pluginParseResult.getQueryText() : null;
        if (base == null || base.isBlank()) {
            return String.format(SCHEDULE_NAME_TEMPLATE, cronDescription);
        }
        String compact = base.replaceAll("\\s+", " ").trim();
        if (compact.length() > 32) {
            compact = compact.substring(0, 32);
        }
        return String.format("定时报表 - %s", compact);
    }
}
