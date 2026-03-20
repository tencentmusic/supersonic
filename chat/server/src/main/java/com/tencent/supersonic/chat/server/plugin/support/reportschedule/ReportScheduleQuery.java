package com.tencent.supersonic.chat.server.plugin.support.reportschedule;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.chat.api.plugin.PluginParseResult;
import com.tencent.supersonic.chat.api.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.plugin.support.PluginSemanticQuery;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.chat.utils.QueryReqBuilder;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleConfirmationDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.ReportDeliveryService;
import com.tencent.supersonic.headless.server.service.ReportScheduleConfirmationService;
import com.tencent.supersonic.headless.server.service.ReportScheduleService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.AFTERNOON;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.CANCEL;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.CONFIRM;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.CRON_PATTERNS;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.DAY_OF_WEEK;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.PAUSE;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.SCHEDULE_ID_PATTERN;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.STATUS;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.TIME_PATTERN;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.TRIGGER;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords.TRIGGER_NOW;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.CONFIRM_CANCEL;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.CONFIRM_CREATE;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.CONFIRM_CREATE_WITH_TRIGGER;
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
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.SUCCESS_CREATED_WITH_TRIGGER;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.SUCCESS_PAUSED;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.SUCCESS_RESUMED;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.SUCCESS_TRIGGERED;
import static com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleMessages.UNKNOWN_INTENT;

/**
 * Plugin query for natural-language report scheduling. Handles create, list, cancel, pause, resume,
 * trigger, and status intents via a two-step confirmation flow.
 *
 * <p>
 * AI-generated logic — reviewed 2026-03-20. Key invariants:
 * <ul>
 * <li>Intent extraction is keyword-based (no LLM) — see {@link ScheduleKeywords}.
 * <li>CREATE always goes through pending confirmation before persistence.
 * <li>Tenant isolation is delegated to {@code TenantSqlInterceptor} at the DB layer.
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportScheduleQuery extends PluginSemanticQuery {

    public static final String QUERY_MODE = "REPORT_SCHEDULE";

    private static final SimpleDateFormat DATE_FORMATTER =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final int DEFAULT_RETRY_COUNT = 3;

    @Value("${s2.schedule.confirmation.expire-ms:300000}")
    private long confirmationExpireMs;

    private final ReportScheduleService scheduleService;
    private final ReportScheduleConfirmationService confirmationService;
    private final ChatManageService chatManageService;
    private final DataSetService dataSetService;
    private final ReportDeliveryService deliveryService;

    @PostConstruct
    public void register() {
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
        Integer chatId = pluginParseResult.getChatId();
        Long currentUserId = pluginParseResult.getUserId();

        ScheduleIntent intent = extractIntent(queryText);

        ReportScheduleResp response;
        try {
            response = switch (intent) {
                case CONFIRM -> handleConfirm(chatId, currentUserId);
                case CREATE -> handleCreate(queryText, chatId, pluginParseResult, currentUserId);
                case LIST -> handleList();
                case CANCEL -> handleCancel(queryText, chatId, currentUserId);
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

    /** Returns true if there is a non-expired pending confirmation for the given user+chat. */
    public static boolean hasPendingConfirmation(Long userId, Integer chatId) {
        ReportScheduleConfirmationService confirmationService =
                ContextUtils.getBean(ReportScheduleConfirmationService.class);
        return confirmationService != null && confirmationService.hasPending(userId, chatId);
    }

    private void savePendingConfirmation(Integer chatId, ScheduleIntent intent,
            Map<String, Object> params, Long userId, Long tenantId,
            ReportSubscriptionSource source) {
        long now = System.currentTimeMillis();
        ReportScheduleConfirmationDO confirmation = new ReportScheduleConfirmationDO();
        confirmation.setUserId(userId);
        confirmation.setChatId(chatId);
        confirmation.setActionType(intent.name());
        confirmation.setTenantId(tenantId);
        confirmation.setPayloadJson(JsonUtil.toString(params));
        confirmation.setCreatedAt(new Date(now));
        confirmation.setExpireAt(new Date(now + confirmationExpireMs));
        if (source != null) {
            confirmation.setSourceQueryId(source.getSourceQueryId());
            confirmation.setSourceParseId(source.getSourceParseId());
            confirmation.setSourceDataSetId(source.getSourceDataSetId());
        }
        confirmationService.createPending(confirmation);
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

        if (ScheduleKeywords.preferCreate(text)) {
            return ScheduleIntent.CREATE;
        }

        if (ScheduleKeywords.preferList(text)) {
            return ScheduleIntent.LIST;
        }

        if (TRIGGER.stream().anyMatch(text::contains)) {
            return ScheduleIntent.TRIGGER;
        }

        return ScheduleIntent.UNKNOWN;
    }

    private ReportScheduleResp handleConfirm(Integer chatId, Long userId) {
        ReportScheduleConfirmationDO pending = confirmationService.getLatestPending(userId, chatId);
        if (pending == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CONFIRM).success(false)
                    .message(ERROR_NO_PENDING).build();
        }
        confirmationService.updateStatus(pending.getId(), "CONFIRMED");
        Map<String, Object> params = JsonUtil.toObject(pending.getPayloadJson(), Map.class);
        ScheduleIntent intent = ScheduleIntent.valueOf(pending.getActionType());

        return switch (intent) {
            case CREATE -> executeCreate(params);
            case CANCEL -> executeCancel(params);
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

        ReportScheduleDO schedule = new ReportScheduleDO();
        schedule.setName(scheduleName);
        schedule.setDatasetId(datasetId);
        schedule.setCronExpression(cronExpression);
        schedule.setQueryConfig(queryConfig);
        schedule.setOutputFormat(outputFormat);
        schedule.setDeliveryConfigIds(deliveryConfigIds);
        schedule.setEnabled(true);
        schedule.setRetryCount(DEFAULT_RETRY_COUNT);
        schedule.setOwnerId(ownerId);
        schedule.setTenantId(tenantId);
        schedule.setCreatedBy(createdBy);

        ReportScheduleDO created = scheduleService.createSchedule(schedule);
        String cronDesc = describeCron(cronExpression);
        String channelName = resolveChannelName(deliveryConfigIds);

        boolean triggerNow = Boolean.TRUE.equals(params.get("triggerNow"));
        if (triggerNow) {
            scheduleService.triggerNow(created.getId());
        }

        String successMsg = triggerNow
                ? String.format(SUCCESS_CREATED_WITH_TRIGGER, created.getId(), cronDesc,
                        channelName, created.getId())
                : String.format(SUCCESS_CREATED, created.getId(), cronDesc, channelName,
                        created.getId());

        return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(true)
                .message(successMsg).scheduleId(created.getId()).cronExpression(cronExpression)
                .cronDescription(cronDesc).build();
    }

    private ReportScheduleResp executeCancel(Map<String, Object> params) {
        Object rawScheduleId = params.get("scheduleId");
        if (!(rawScheduleId instanceof Number)) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CANCEL).success(false)
                    .message(String.format(ERROR_OPERATION_FAILED, "scheduleId missing")).build();
        }
        Long scheduleId = ((Number) rawScheduleId).longValue();

        scheduleService.deleteSchedule(scheduleId);

        return ReportScheduleResp.builder().intent(ScheduleIntent.CANCEL).success(true)
                .message(String.format(SUCCESS_CANCELLED, scheduleId)).scheduleId(scheduleId)
                .build();
    }

    private ReportScheduleResp handleCreate(String queryText, Integer chatId,
            PluginParseResult pluginParseResult, Long currentUserId) {
        String cronExpression = parseCronExpression(queryText);

        if (cronExpression == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(false)
                    .message(ERROR_SPECIFY_FREQUENCY).needConfirm(false).build();
        }

        ReportSubscriptionSource source = resolveSubscriptionSource(pluginParseResult);
        if (source == null || source.getSourceDataSetId() == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(false)
                    .message(ERROR_SPECIFY_REPORT_CONTENT).needConfirm(false).build();
        }
        Long scheduleDatasetId = source.getSourceDataSetId();

        String queryConfig = source.getQueryConfigSnapshot();
        if (StringUtils.isBlank(queryConfig)) {
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
        String dataSetName = getDataSetName(scheduleDatasetId);
        String scheduleName = buildScheduleName(dataSetName, cronDescription);
        boolean triggerNow = TRIGGER_NOW.stream().anyMatch(queryText::contains);

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
        params.put("triggerNow", triggerNow);
        params.put("sourceQueryId", source.getSourceQueryId());
        params.put("sourceParseId", source.getSourceParseId());
        params.put("sourceDataSetId", source.getSourceDataSetId());
        params.put("sourceSummary", source.getSummaryText());
        savePendingConfirmation(chatId, ScheduleIntent.CREATE, params, currentUserId,
                pluginParseResult.getTenantId(), source);

        ReportScheduleResp.ConfirmAction confirmAction = ReportScheduleResp.ConfirmAction.builder()
                .action("CREATE_SCHEDULE").params(params).build();

        String displayName =
                StringUtils.isNotBlank(source.getSummaryText()) ? source.getSummaryText()
                        : dataSetName != null ? dataSetName : String.valueOf(scheduleDatasetId);
        String confirmMsg = triggerNow
                ? String.format(CONFIRM_CREATE_WITH_TRIGGER, displayName, cronDescription)
                : String.format(CONFIRM_CREATE, displayName, cronDescription);

        return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(true)
                .message(confirmMsg).needConfirm(true).confirmAction(confirmAction)
                .cronExpression(cronExpression).cronDescription(cronDescription).build();
    }

    private ReportScheduleResp handleList() {
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

    private ReportScheduleResp handleCancel(String queryText, Integer chatId, Long currentUserId) {
        Long scheduleId = extractScheduleId(queryText);
        if (scheduleId == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CANCEL).success(false)
                    .message(ERROR_SPECIFY_CANCEL_ID).build();
        }

        ReportScheduleDO schedule = scheduleService.getScheduleById(scheduleId);

        if (schedule == null) {
            return ReportScheduleResp.builder().intent(ScheduleIntent.CANCEL).success(false)
                    .message(String.format(ERROR_SCHEDULE_NOT_FOUND, scheduleId)).build();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("scheduleId", scheduleId);
        savePendingConfirmation(chatId, ScheduleIntent.CANCEL, params, currentUserId,
                schedule.getTenantId(), null);

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

        String normalizedQuery = queryText.replace('：', ':');
        String baseCron = null;
        for (Map.Entry<String, String> entry : CRON_PATTERNS.entrySet()) {
            if (normalizedQuery.contains(entry.getKey())) {
                baseCron = entry.getValue();
                break;
            }
        }

        if (baseCron == null) {
            return null;
        }

        // Try to extract specific time
        Matcher timeMatcher = TIME_PATTERN.matcher(normalizedQuery);
        if (timeMatcher.find()) {
            String period = timeMatcher.group(1);
            int hour = Integer.parseInt(timeMatcher.group(2));
            String minuteStr = timeMatcher.group(4); // non-null only for H:MM format
            String halfHour = timeMatcher.group(5); // non-null only for "X点半"

            // Adjust hour based on period
            if (period != null && AFTERNOON.contains(period) && hour < 12) {
                hour += 12;
            }

            // Replace hour (and minute if specified) in cron expression
            String[] parts = baseCron.split(" ");
            parts[2] = String.valueOf(hour);
            if (minuteStr != null) {
                parts[1] = minuteStr;
            } else if ("半".equals(halfHour)) {
                parts[1] = "30";
            }
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

    private ReportSubscriptionSource resolveSubscriptionSource(
            PluginParseResult pluginParseResult) {
        if (pluginParseResult == null || pluginParseResult.getChatId() == null) {
            return null;
        }
        try {
            Long currentQueryId = pluginParseResult.getQueryId();
            for (QueryResp query : chatManageService
                    .getChatQueries(pluginParseResult.getChatId())) {
                if (currentQueryId != null && currentQueryId.equals(query.getQuestionId())) {
                    continue;
                }
                if (query.getParseInfos() == null) {
                    continue;
                }
                for (SemanticParseInfo parseInfo : query.getParseInfos()) {
                    if (!isSchedulable(parseInfo)) {
                        continue;
                    }
                    String queryConfig = buildQueryConfig(parseInfo);
                    if (StringUtils.isBlank(queryConfig)) {
                        continue;
                    }
                    String summary =
                            StringUtils.isNotBlank(query.getQueryText()) ? query.getQueryText()
                                    : getDataSetName(parseInfo.getDataSetId());
                    return ReportSubscriptionSource.builder().sourceQueryId(query.getQuestionId())
                            .sourceParseId(parseInfo.getId())
                            .sourceDataSetId(parseInfo.getDataSetId()).sourceType("QUERY_RESULT")
                            .queryConfigSnapshot(queryConfig).summaryText(summary).build();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve report subscription source for chat {}: {}",
                    pluginParseResult.getChatId(), e.getMessage());
        }
        return null;
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

    private String buildScheduleName(String dataSetName, String cronDescription) {
        if (dataSetName == null || dataSetName.isBlank()) {
            return String.format(SCHEDULE_NAME_TEMPLATE, cronDescription);
        }
        String name = dataSetName + " " + cronDescription;
        if (name.length() > 40) {
            name = name.substring(0, 40);
        }
        return String.format(SCHEDULE_NAME_TEMPLATE, name);
    }

    private String getDataSetName(Long datasetId) {
        if (datasetId == null) {
            return null;
        }
        try {
            DataSetResp dataSet = dataSetService.getDataSet(datasetId);
            return dataSet != null ? dataSet.getName() : null;
        } catch (Exception e) {
            log.warn("Failed to get dataset name for id {}: {}", datasetId, e.getMessage());
            return null;
        }
    }

    private String resolveChannelName(String deliveryConfigIds) {
        if (deliveryConfigIds == null || deliveryConfigIds.isBlank()) {
            return "—";
        }
        try {
            long configId = Long.parseLong(deliveryConfigIds.split(",")[0].trim());
            ReportDeliveryConfigDO config = deliveryService.getConfigById(configId);
            return config != null && config.getName() != null ? config.getName()
                    : deliveryConfigIds;
        } catch (Exception e) {
            log.warn("Failed to resolve channel name for config ids {}: {}", deliveryConfigIds,
                    e.getMessage());
            return deliveryConfigIds;
        }
    }
}
