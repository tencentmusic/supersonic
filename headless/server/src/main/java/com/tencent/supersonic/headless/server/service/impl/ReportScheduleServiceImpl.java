package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.ReportExecutionVO;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportExecutionResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp;
import com.tencent.supersonic.headless.api.service.ReportScheduleService;
import com.tencent.supersonic.headless.server.manager.QuartzJobManager;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryRecordDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryRecordMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportScheduleMapper;
import com.tencent.supersonic.headless.server.pojo.DeliveryStatus;
import com.tencent.supersonic.headless.server.pojo.ExecutionSnapshotData;
import com.tencent.supersonic.headless.server.pojo.ReportExecutionContext;
import com.tencent.supersonic.headless.server.service.DataSetAuthService;
import com.tencent.supersonic.headless.server.service.mapper.ReportDtoMappers;
import com.tencent.supersonic.headless.server.task.ReportScheduleJob;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobDataMap;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportScheduleServiceImpl extends ServiceImpl<ReportScheduleMapper, ReportScheduleDO>
        implements ReportScheduleService {

    private static final String GROUP = "REPORT";
    private static final String KEY_PREFIX = "report_";

    private final QuartzJobManager quartzJobManager;
    private final ReportExecutionMapper executionMapper;
    private final ReportExecutionContextBuilder contextBuilder;
    private final ReportExecutionOrchestrator orchestrator;
    private final UserService userService;
    private final DataSetAuthService dataSetAuthService;
    private final ReportDeliveryRecordMapper deliveryRecordMapper;

    public ReportScheduleServiceImpl(QuartzJobManager quartzJobManager,
            ReportExecutionMapper executionMapper, ReportExecutionContextBuilder contextBuilder,
            ReportExecutionOrchestrator orchestrator, UserService userService,
            DataSetAuthService dataSetAuthService,
            ReportDeliveryRecordMapper deliveryRecordMapper) {
        this.quartzJobManager = quartzJobManager;
        this.executionMapper = executionMapper;
        this.contextBuilder = contextBuilder;
        this.orchestrator = orchestrator;
        this.userService = userService;
        this.dataSetAuthService = dataSetAuthService;
        this.deliveryRecordMapper = deliveryRecordMapper;
    }

    @PostConstruct
    public void recoverAndWarnOnStartup() {
        QueryWrapper<ReportScheduleDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().isNull(ReportScheduleDO::getOwnerId);
        long orphanCount = baseMapper.selectCount(wrapper);
        if (orphanCount > 0) {
            log.warn(
                    "[ReportSchedule] {} schedule(s) have owner_id=NULL and will fail at execution "
                            + "time. Run a data migration to assign a valid owner before enabling them.",
                    orphanCount);
        }

        // Re-register enabled schedules whose Quartz job was lost (e.g. after a restart with
        // initialize-schema:always, or manual DB inserts). Also covers records with null
        // quartz_job_key (never registered, or failed mid-create).
        QueryWrapper<ReportScheduleDO> enabledWrapper = new QueryWrapper<>();
        enabledWrapper.lambda().eq(ReportScheduleDO::getEnabled, true);
        List<ReportScheduleDO> enabled = baseMapper.selectList(enabledWrapper);
        int recovered = 0;
        for (ReportScheduleDO schedule : enabled) {
            String expectedKey = GROUP + "." + KEY_PREFIX + schedule.getId();
            if (!quartzJobManager.jobExists(expectedKey)) {
                try {
                    ensureJobRegistered(schedule);
                    recovered++;
                } catch (Exception e) {
                    log.error("[ReportSchedule] Failed to recover Quartz job for schedule id={}",
                            schedule.getId(), e);
                }
            }
        }
        if (recovered > 0) {
            log.info("[ReportSchedule] Recovered {} orphaned Quartz job(s) on startup.", recovered);
        }
    }

    @Override
    public ReportScheduleResp createSchedule(ReportScheduleReq req, User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("current user is required to create a schedule");
        }
        User owner = userService.getUserById(user.getId());
        if (owner == null) {
            throw new IllegalArgumentException("Owner user not found for id=" + user.getId());
        }
        if (req.getDatasetId() != null
                && !dataSetAuthService.checkDataSetViewPermission(req.getDatasetId(), owner)) {
            throw new InvalidPermissionException("您没有该数据集的权限，请联系管理员申请");
        }

        ReportScheduleDO schedule = ReportDtoMappers.toDO(req);
        schedule.setOwnerId(owner.getId());
        schedule.setTenantId(owner.getTenantId());
        schedule.setCreatedBy(owner.getName());
        schedule.setCreatedAt(new Date());
        schedule.setUpdatedAt(new Date());
        if (schedule.getEnabled() == null) {
            schedule.setEnabled(true);
        }
        if (schedule.getRetryCount() == null) {
            schedule.setRetryCount(3);
        }
        if (schedule.getRetryInterval() == null) {
            schedule.setRetryInterval(30);
        }
        validateQueryConfig(schedule.getQueryConfig());
        baseMapper.insert(schedule);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("scheduleId", schedule.getId());
        jobDataMap.put("tenantId", schedule.getTenantId());

        String quartzJobKey = quartzJobManager.createJob(GROUP, KEY_PREFIX, schedule.getId(),
                ReportScheduleJob.class, schedule.getCronExpression(), jobDataMap);

        schedule.setQuartzJobKey(quartzJobKey);
        baseMapper.updateById(schedule);
        return ReportDtoMappers.toResp(schedule);
    }

    @Override
    public ReportScheduleResp updateSchedule(ReportScheduleReq req, User user) {
        // Load existing once — reused for both permission check and Quartz reschedule
        ReportScheduleDO existing = baseMapper.selectById(req.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Schedule not found: " + req.getId());
        }
        checkOwnership(existing, user);

        // Pre-flight: if datasetId is being changed, verify the requesting user has view permission
        if (req.getDatasetId() != null) {
            if (!dataSetAuthService.checkDataSetViewPermission(req.getDatasetId(), user)) {
                throw new InvalidPermissionException("您没有该数据集的权限，请联系管理员申请");
            }
        }

        validateQueryConfig(req.getQueryConfig());
        String originalCron = existing.getCronExpression();
        String originalQuartzKey = existing.getQuartzJobKey();
        // Copy caller-provided fields into the loaded DO; preserve server-owned fields.
        BeanUtils.copyProperties(req, existing, "id", "ownerId", "tenantId", "createdBy",
                "createdAt", "quartzJobKey", "lastExecutionTime", "nextExecutionTime");
        existing.setUpdatedAt(new Date());
        baseMapper.updateById(existing);

        // Reschedule Quartz using quartzJobKey from DB (not from request body — frontend never
        // sends it)
        // Only reschedule when cron actually changed to avoid unnecessary Quartz operations
        if (existing.getCronExpression() != null && originalQuartzKey != null
                && !existing.getCronExpression().equals(originalCron)) {
            reschedule(existing.getId(), existing.getCronExpression());
        }
        return ReportDtoMappers.toResp(existing);
    }

    /**
     * Validates the queryConfig JSON when it can be parsed as a {@link QueryStructReq}. Ensures
     * date-related fields are consistent with the chosen {@link DateConf.DateMode}. Silently skips
     * validation if the JSON represents a different config format (e.g. SqlTemplateConfig or
     * QuerySqlReq).
     */
    private void validateQueryConfig(String queryConfig) {
        if (StringUtils.isBlank(queryConfig)) {
            return;
        }
        QueryStructReq req;
        try {
            req = JsonUtil.toObject(queryConfig, QueryStructReq.class);
        } catch (Exception e) {
            // Not a QueryStructReq — may be SqlTemplateConfig or QuerySqlReq; skip validation
            return;
        }
        if (req == null || req.getDateInfo() == null) {
            return;
        }
        DateConf dateInfo = req.getDateInfo();
        DateConf.DateMode mode = dateInfo.getDateMode();
        if (mode == null) {
            return;
        }
        switch (mode) {
            case BETWEEN:
                if (StringUtils.isBlank(dateInfo.getDateField())) {
                    throw new IllegalArgumentException("请选择或填写日期字段");
                }
                if (StringUtils.isBlank(dateInfo.getStartDate())
                        || StringUtils.isBlank(dateInfo.getEndDate())) {
                    throw new IllegalArgumentException("请选择日期范围");
                }
                break;
            case RECENT:
                if (StringUtils.isBlank(dateInfo.getDateField())) {
                    throw new IllegalArgumentException("请选择或填写日期字段");
                }
                if (dateInfo.getUnit() == null || dateInfo.getUnit() <= 0) {
                    throw new IllegalArgumentException("请输入最近 N 天的天数");
                }
                break;
            case ALL:
                if (req.getQueryType() == QueryType.DETAIL) {
                    throw new IllegalArgumentException("明细调度不支持 ALL 模式，请选择固定区间或最近 N 天");
                }
                break;
            default:
                break;
        }
        if (req.getQueryType() == QueryType.DETAIL) {
            if (CollectionUtils.isEmpty(req.getDimensions())
                    && CollectionUtils.isEmpty(req.getGroups())) {
                throw new IllegalArgumentException("明细调度需要至少一个查询列");
            }
            if (req.getLimit() <= 0) {
                throw new IllegalArgumentException("明细调度需要有效的 limit");
            }
        }
    }

    @Override
    public void deleteSchedule(Long id, User user) {
        ReportScheduleDO schedule = baseMapper.selectById(id);
        if (schedule == null) {
            return;
        }
        checkOwnership(schedule, user);
        quartzJobManager.deleteJob(schedule.getQuartzJobKey());
        baseMapper.deleteById(id);
    }

    @Override
    public ReportScheduleResp getScheduleById(Long id, User user) {
        ReportScheduleDO schedule = baseMapper.selectById(id);
        if (schedule == null) {
            return null;
        }
        checkReadPermission(schedule, user);
        return ReportDtoMappers.toResp(schedule);
    }

    @Override
    public Page<ReportScheduleResp> getScheduleList(Page<ReportScheduleResp> page, Long datasetId,
            Boolean enabled, User user) {
        QueryWrapper<ReportScheduleDO> wrapper = new QueryWrapper<>();
        if (datasetId != null) {
            wrapper.lambda().eq(ReportScheduleDO::getDatasetId, datasetId);
        }
        if (enabled != null) {
            wrapper.lambda().eq(ReportScheduleDO::getEnabled, enabled);
        }
        if (!user.isSuperAdmin()) {
            wrapper.lambda().eq(ReportScheduleDO::getOwnerId, user.getId());
        }
        wrapper.lambda().orderByDesc(ReportScheduleDO::getCreatedAt);
        Page<ReportScheduleDO> doPage = new Page<>(page.getCurrent(), page.getSize());
        Page<ReportScheduleDO> result = baseMapper.selectPage(doPage, wrapper);
        return ReportDtoMappers.toRespPage(result);
    }

    @Override
    public void pauseSchedule(Long id, User user) {
        ReportScheduleDO schedule = baseMapper.selectById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule not found: " + id);
        }
        checkOwnership(schedule, user);
        quartzJobManager.pauseJob(schedule.getQuartzJobKey());
        schedule.setEnabled(false);
        schedule.setUpdatedAt(new Date());
        baseMapper.updateById(schedule);
    }

    @Override
    public void resumeSchedule(Long id, User user) {
        ReportScheduleDO schedule = baseMapper.selectById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule not found: " + id);
        }
        checkOwnership(schedule, user);
        ensureJobRegistered(schedule);
        quartzJobManager.resumeJob(schedule.getQuartzJobKey());
        schedule.setEnabled(true);
        schedule.setUpdatedAt(new Date());
        baseMapper.updateById(schedule);
    }

    @Override
    public void triggerNow(Long id, User user) {
        ReportScheduleDO schedule = baseMapper.selectById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule not found: " + id);
        }
        checkOwnership(schedule, user);
        ensureJobRegistered(schedule);
        JobDataMap triggerData = new JobDataMap();
        triggerData.put("manual", true);
        quartzJobManager.triggerJob(schedule.getQuartzJobKey(), triggerData);
    }

    @Override
    public void reschedule(Long id, String newCron) {
        ReportScheduleDO schedule = baseMapper.selectById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule not found: " + id);
        }
        ensureJobRegistered(schedule);
        quartzJobManager.rescheduleJob(schedule.getQuartzJobKey(), newCron);
        schedule.setCronExpression(newCron);
        schedule.setUpdatedAt(new Date());
        baseMapper.updateById(schedule);
    }

    @Override
    public Page<ReportExecutionResp> getExecutionList(Page<ReportExecutionResp> page,
            Long scheduleId, String status, User user) {
        Page<ReportExecutionDO> doPage = new Page<>(page.getCurrent(), page.getSize());
        Page<ReportExecutionDO> result =
                queryExecutionListInternal(doPage, scheduleId, status, user);
        return ReportDtoMappers.toExecutionRespPage(result);
    }

    private Page<ReportExecutionDO> queryExecutionListInternal(Page<ReportExecutionDO> doPage,
            Long scheduleId, String status, User user) {
        List<Long> allowedScheduleIds = getReadableScheduleIds(user, scheduleId);
        if (CollectionUtils.isEmpty(allowedScheduleIds)) {
            doPage.setRecords(List.of());
            doPage.setTotal(0L);
            return doPage;
        }
        QueryWrapper<ReportExecutionDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().in(ReportExecutionDO::getScheduleId, allowedScheduleIds);
        if (status != null) {
            wrapper.lambda().eq(ReportExecutionDO::getStatus, status);
        }
        wrapper.lambda().orderByDesc(ReportExecutionDO::getStartTime);
        return executionMapper.selectPage(doPage, wrapper);
    }

    @Override
    public ReportExecutionResp getExecutionById(Long scheduleId, Long id, User user) {
        ReportExecutionDO execution = executionMapper.selectById(id);
        if (execution == null) {
            return null;
        }
        if (scheduleId != null && !scheduleId.equals(execution.getScheduleId())) {
            return null;
        }
        ReportScheduleDO schedule = baseMapper.selectById(execution.getScheduleId());
        if (schedule == null) {
            return null;
        }
        checkReadPermission(schedule, user);
        return ReportDtoMappers.toResp(execution);
    }

    /**
     * Ensures the Quartz job for the given schedule exists and the DB key is correct.
     *
     * <p>
     * The canonical key is always {@code REPORT.report_{id}} — this is the only format produced by
     * {@link #createSchedule}. If the DB record has a null or stale key, it is corrected here so
     * that subsequent {@code triggerJob / resumeJob} calls use the right value.
     *
     * <p>
     * If the Quartz job is missing (or partially corrupted — e.g. orphaned trigger without job),
     * {@link QuartzJobManager#recreateJob} cleans up and rebuilds atomically.
     */
    private void ensureJobRegistered(ReportScheduleDO schedule) {
        String expectedKey = GROUP + "." + KEY_PREFIX + schedule.getId();

        // Always normalise the DB key so callers can rely on schedule.getQuartzJobKey().
        if (!expectedKey.equals(schedule.getQuartzJobKey())) {
            schedule.setQuartzJobKey(expectedKey);
            schedule.setUpdatedAt(new Date());
            baseMapper.updateById(schedule);
        }

        if (quartzJobManager.jobExists(expectedKey)) {
            return;
        }
        log.warn("[ReportSchedule] Quartz job missing for schedule id={}, repairing",
                schedule.getId());
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("scheduleId", schedule.getId());
        jobDataMap.put("tenantId", schedule.getTenantId());
        quartzJobManager.recreateJob(GROUP, KEY_PREFIX, schedule.getId(), ReportScheduleJob.class,
                schedule.getCronExpression(), jobDataMap);
    }

    private void checkOwnership(ReportScheduleDO schedule, User user) {
        if (user.isSuperAdmin()) {
            return;
        }
        if (!user.getId().equals(schedule.getOwnerId())) {
            throw new InvalidPermissionException("只有调度创建人或管理员才能执行此操作");
        }
    }

    private void checkReadPermission(ReportScheduleDO schedule, User user) {
        if (user == null) {
            throw new InvalidPermissionException("未登录用户无权访问该调度");
        }
        if (user.isSuperAdmin()) {
            return;
        }
        if (!user.getId().equals(schedule.getOwnerId())) {
            throw new InvalidPermissionException("只有调度创建人或管理员才能查看此调度");
        }
    }

    private List<Long> getReadableScheduleIds(User user, Long scheduleId) {
        if (scheduleId != null) {
            ReportScheduleDO schedule = baseMapper.selectById(scheduleId);
            if (schedule == null) {
                return List.of();
            }
            checkReadPermission(schedule, user);
            return List.of(scheduleId);
        }
        QueryWrapper<ReportScheduleDO> wrapper = new QueryWrapper<>();
        if (!user.isSuperAdmin()) {
            wrapper.lambda().eq(ReportScheduleDO::getOwnerId, user.getId());
        }
        List<ReportScheduleDO> schedules = baseMapper.selectList(wrapper);
        return schedules.stream().map(ReportScheduleDO::getId).toList();
    }

    @Override
    public void executeReport(Long scheduleId, User user) {
        ReportScheduleDO schedule = baseMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule not found: " + scheduleId);
        }
        checkOwnership(schedule, user);
        ReportExecutionContext ctx = contextBuilder.buildManualFromSchedule(schedule, user);
        orchestrator.execute(ctx);
    }

    @Override
    public Page<ReportExecutionVO> getExecutionVOList(Page<ReportExecutionResp> page,
            Long scheduleId, String status, User user) {
        Page<ReportExecutionDO> initial = new Page<>(page.getCurrent(), page.getSize());
        Page<ReportExecutionDO> doPage =
                queryExecutionListInternal(initial, scheduleId, status, user);
        Page<ReportExecutionVO> voPage =
                new Page<>(doPage.getCurrent(), doPage.getSize(), doPage.getTotal());

        List<ReportExecutionDO> records = doPage.getRecords();
        Map<Long, List<ReportDeliveryRecordDO>> rollupByExec = loadDeliveryRecordsByExecutionIds(
                records.stream().map(ReportExecutionDO::getId).toList());

        voPage.setRecords(records.stream().map(e -> toVO(e, rollupByExec.get(e.getId()))).toList());
        return voPage;
    }

    private Map<Long, List<ReportDeliveryRecordDO>> loadDeliveryRecordsByExecutionIds(
            List<Long> executionIds) {
        if (CollectionUtils.isEmpty(executionIds)) {
            return Collections.emptyMap();
        }
        QueryWrapper<ReportDeliveryRecordDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().in(ReportDeliveryRecordDO::getExecutionId, executionIds);
        List<ReportDeliveryRecordDO> all = deliveryRecordMapper.selectList(wrapper);
        return all.stream().collect(Collectors.groupingBy(ReportDeliveryRecordDO::getExecutionId));
    }

    private ReportExecutionVO toVO(ReportExecutionDO execution,
            List<ReportDeliveryRecordDO> deliveryRecords) {
        ReportExecutionVO vo = new ReportExecutionVO();
        vo.setId(execution.getId());
        vo.setScheduleId(execution.getScheduleId());
        vo.setAttempt(execution.getAttempt());
        vo.setStatus(execution.getStatus());
        vo.setStartTime(execution.getStartTime());
        vo.setEndTime(execution.getEndTime());
        vo.setResultLocation(execution.getResultLocation());
        vo.setErrorMessage(execution.getErrorMessage());
        vo.setRowCount(execution.getRowCount());
        vo.setExecutionTimeMs(execution.getExecutionTimeMs());
        vo.setTemplateVersion(execution.getTemplateVersion());

        if (StringUtils.isNotBlank(execution.getExecutionSnapshot())) {
            try {
                ExecutionSnapshotData snapshot = JsonUtil.toObject(execution.getExecutionSnapshot(),
                        ExecutionSnapshotData.class);
                if (snapshot != null && snapshot.getContext() != null) {
                    ReportExecutionContext ctx = snapshot.getContext();
                    vo.setTemplateName(ctx.getScheduleName());
                    vo.setTriggerType(ctx.getSource() != null ? ctx.getSource().name() : null);
                }
                vo.setHasPreview(snapshot != null && snapshot.getResultPreview() != null
                        && !snapshot.getResultPreview().isEmpty());
            } catch (Exception e) {
                log.debug("Failed to parse executionSnapshot for execution id={}: {}",
                        execution.getId(), e.getMessage());
            }
        }

        applyDeliveryRollup(vo, deliveryRecords);
        return vo;
    }

    /**
     * Rollup 规则（多渠道场景下执行.status 只反映查询执行成败，投递在这里单独聚合）：
     *
     * <ul>
     * <li>无记录 → NONE, channelTypes 空
     * <li>任一 PENDING/SENDING → IN_PROGRESS
     * <li>全 SUCCESS → DELIVERED
     * <li>全 FAILED → FAILED
     * <li>SUCCESS + FAILED 混合 → PARTIAL
     * </ul>
     *
     * channelTypes 按 deliveryType 去重，保留首次出现顺序（同一渠道重试可能产生多条记录）。
     */
    private void applyDeliveryRollup(ReportExecutionVO vo,
            List<ReportDeliveryRecordDO> deliveryRecords) {
        if (CollectionUtils.isEmpty(deliveryRecords)) {
            vo.setChannelTypes(new ArrayList<>());
            vo.setDeliveryRollup("NONE");
            vo.setDeliverySuccessCount(0);
            vo.setDeliveryTotalCount(0);
            return;
        }

        // 每个 configId 只保留最新一次结果，避免被重试产生的历史记录拉偏 rollup。
        Map<Long, ReportDeliveryRecordDO> latestByConfig = new LinkedHashMap<>();
        for (ReportDeliveryRecordDO r : deliveryRecords) {
            latestByConfig.merge(r.getConfigId(), r, (a, b) -> {
                Date ta = b.getCompletedAt() != null ? b.getCompletedAt() : b.getCreatedAt();
                Date tb = a.getCompletedAt() != null ? a.getCompletedAt() : a.getCreatedAt();
                if (ta == null || tb == null) {
                    return b;
                }
                return ta.after(tb) ? b : a;
            });
        }

        Set<String> channels = new LinkedHashSet<>();
        int successCount = 0;
        int failedCount = 0;
        int inProgressCount = 0;
        for (ReportDeliveryRecordDO r : latestByConfig.values()) {
            if (StringUtils.isNotBlank(r.getDeliveryType())) {
                channels.add(r.getDeliveryType());
            }
            if (DeliveryStatus.SUCCESS.name().equals(r.getStatus())) {
                successCount++;
            } else if (DeliveryStatus.FAILED.name().equals(r.getStatus())) {
                failedCount++;
            } else {
                inProgressCount++;
            }
        }

        int total = latestByConfig.size();
        String rollup;
        if (inProgressCount > 0) {
            rollup = "IN_PROGRESS";
        } else if (failedCount == 0) {
            rollup = "DELIVERED";
        } else if (successCount == 0) {
            rollup = "FAILED";
        } else {
            rollup = "PARTIAL";
        }

        vo.setChannelTypes(new ArrayList<>(channels));
        vo.setDeliveryRollup(rollup);
        vo.setDeliverySuccessCount(successCount);
        vo.setDeliveryTotalCount(total);
    }
}
