package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.manager.QuartzJobManager;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportScheduleMapper;
import com.tencent.supersonic.headless.server.pojo.ReportExecutionContext;
import com.tencent.supersonic.headless.server.service.ReportScheduleService;
import com.tencent.supersonic.headless.server.task.ReportScheduleJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.springframework.stereotype.Service;

import java.util.Date;

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

    public ReportScheduleServiceImpl(QuartzJobManager quartzJobManager,
            ReportExecutionMapper executionMapper, ReportExecutionContextBuilder contextBuilder,
            ReportExecutionOrchestrator orchestrator) {
        this.quartzJobManager = quartzJobManager;
        this.executionMapper = executionMapper;
        this.contextBuilder = contextBuilder;
        this.orchestrator = orchestrator;
    }

    @Override
    public ReportScheduleDO createSchedule(ReportScheduleDO schedule) {
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
        baseMapper.insert(schedule);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("scheduleId", schedule.getId());
        jobDataMap.put("tenantId", schedule.getTenantId());

        String quartzJobKey = quartzJobManager.createJob(GROUP, KEY_PREFIX, schedule.getId(),
                ReportScheduleJob.class, schedule.getCronExpression(), jobDataMap);

        schedule.setQuartzJobKey(quartzJobKey);
        baseMapper.updateById(schedule);
        return schedule;
    }

    @Override
    public ReportScheduleDO updateSchedule(ReportScheduleDO schedule) {
        schedule.setUpdatedAt(new Date());
        baseMapper.updateById(schedule);

        if (schedule.getCronExpression() != null && schedule.getQuartzJobKey() != null) {
            reschedule(schedule.getId(), schedule.getCronExpression());
        }
        return schedule;
    }

    @Override
    public void deleteSchedule(Long id) {
        ReportScheduleDO schedule = baseMapper.selectById(id);
        if (schedule == null) {
            return;
        }
        quartzJobManager.deleteJob(schedule.getQuartzJobKey());
        baseMapper.deleteById(id);
    }

    @Override
    public ReportScheduleDO getScheduleById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public Page<ReportScheduleDO> getScheduleList(Page<ReportScheduleDO> page, Long datasetId,
            Boolean enabled) {
        QueryWrapper<ReportScheduleDO> wrapper = new QueryWrapper<>();
        if (datasetId != null) {
            wrapper.lambda().eq(ReportScheduleDO::getDatasetId, datasetId);
        }
        if (enabled != null) {
            wrapper.lambda().eq(ReportScheduleDO::getEnabled, enabled);
        }
        wrapper.lambda().orderByDesc(ReportScheduleDO::getCreatedAt);
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public void pauseSchedule(Long id) {
        ReportScheduleDO schedule = baseMapper.selectById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule not found: " + id);
        }
        quartzJobManager.pauseJob(schedule.getQuartzJobKey());
        schedule.setEnabled(false);
        schedule.setUpdatedAt(new Date());
        baseMapper.updateById(schedule);
    }

    @Override
    public void resumeSchedule(Long id) {
        ReportScheduleDO schedule = baseMapper.selectById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule not found: " + id);
        }
        quartzJobManager.resumeJob(schedule.getQuartzJobKey());
        schedule.setEnabled(true);
        schedule.setUpdatedAt(new Date());
        baseMapper.updateById(schedule);
    }

    @Override
    public void triggerNow(Long id) {
        ReportScheduleDO schedule = baseMapper.selectById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule not found: " + id);
        }
        quartzJobManager.triggerJob(schedule.getQuartzJobKey());
    }

    @Override
    public void reschedule(Long id, String newCron) {
        ReportScheduleDO schedule = baseMapper.selectById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule not found: " + id);
        }
        quartzJobManager.rescheduleJob(schedule.getQuartzJobKey(), newCron);
        schedule.setCronExpression(newCron);
        schedule.setUpdatedAt(new Date());
        baseMapper.updateById(schedule);
    }

    @Override
    public Page<ReportExecutionDO> getExecutionList(Page<ReportExecutionDO> page, Long scheduleId,
            String status) {
        QueryWrapper<ReportExecutionDO> wrapper = new QueryWrapper<>();
        if (scheduleId != null) {
            wrapper.lambda().eq(ReportExecutionDO::getScheduleId, scheduleId);
        }
        if (status != null) {
            wrapper.lambda().eq(ReportExecutionDO::getStatus, status);
        }
        wrapper.lambda().orderByDesc(ReportExecutionDO::getStartTime);
        return executionMapper.selectPage(page, wrapper);
    }

    @Override
    public ReportExecutionDO getExecutionById(Long id) {
        return executionMapper.selectById(id);
    }

    @Override
    public void executeReport(Long scheduleId, User user) {
        ReportScheduleDO schedule = baseMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule not found: " + scheduleId);
        }
        ReportExecutionContext ctx = contextBuilder.buildManualFromSchedule(schedule, user);
        orchestrator.execute(ctx);
    }
}
