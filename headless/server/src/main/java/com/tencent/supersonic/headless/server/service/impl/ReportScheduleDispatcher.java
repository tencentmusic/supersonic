package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.headless.server.metrics.TemplateReportMetrics;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportScheduleMapper;
import com.tencent.supersonic.headless.server.pojo.ReportExecutionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportScheduleDispatcher {

    private final ReportScheduleMapper scheduleMapper;
    private final ReportExecutionMapper executionMapper;
    private final ReportExecutionOrchestrator orchestrator;
    private final ReportExecutionContextBuilder contextBuilder;
    @Autowired(required = false)
    private TemplateReportMetrics reportMetrics;

    @Value("${s2.report.schedule.max-concurrent-per-tenant:5}")
    private int maxConcurrentPerTenant;

    public void dispatch(Long scheduleId) {
        dispatch(scheduleId, false);
    }

    public void dispatch(Long scheduleId, boolean manual) {
        ReportScheduleDO schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            log.warn("Schedule not found: {}", scheduleId);
            recordScheduleDispatch("not_found");
            return;
        }
        if (!manual && !Boolean.TRUE.equals(schedule.getEnabled())) {
            log.info("Schedule {} is disabled, skipping", scheduleId);
            recordScheduleDispatch("disabled");
            return;
        }

        Long tenantId = schedule.getTenantId();

        // AG-14: enforce per-tenant concurrency limit using the DB as the source of truth.
        // This works correctly in clustered Quartz deployments (multiple JVM nodes) because the
        // count is read from the shared database rather than a process-local semaphore.
        long running = countRunningExecutions(tenantId);
        if (running >= maxConcurrentPerTenant) {
            log.warn(
                    "Tenant {} has {} running executions (limit {}), "
                            + "skipping scheduleId={} — will retry on next cron trigger",
                    tenantId, running, maxConcurrentPerTenant, scheduleId);
            recordScheduleDispatch("skipped_concurrency_limit");
            return;
        }

        boolean success = executeWithRetry(schedule);
        schedule.setLastExecutionTime(new Date());
        scheduleMapper.updateById(schedule);
        recordScheduleDispatch(success ? "success" : "error");
    }

    /** Count RUNNING executions for the given tenant in the shared DB. */
    private long countRunningExecutions(Long tenantId) {
        LambdaQueryWrapper<ReportExecutionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportExecutionDO::getTenantId, tenantId).eq(ReportExecutionDO::getStatus,
                "RUNNING");
        return executionMapper.selectCount(wrapper);
    }

    private boolean executeWithRetry(ReportScheduleDO schedule) {
        int maxAttempts = (schedule.getRetryCount() != null ? schedule.getRetryCount() : 3) + 1;
        int retryInterval = schedule.getRetryInterval() != null ? schedule.getRetryInterval() : 30;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ReportExecutionContext ctx = contextBuilder.buildFromSchedule(schedule, attempt);
                orchestrator.execute(ctx);
                return true;
            } catch (Exception e) {
                if (attempt < maxAttempts) {
                    long delay = retryInterval * (1L << (attempt - 1));
                    log.warn("Schedule {} attempt {}/{} failed, retry in {}s", schedule.getId(),
                            attempt, maxAttempts, delay, e);
                    try {
                        TimeUnit.SECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted for schedule {}", schedule.getId());
                        return false;
                    }
                } else {
                    log.error("Schedule {} all {} attempts exhausted", schedule.getId(),
                            maxAttempts, e);
                    if (reportMetrics != null) {
                        reportMetrics.recordScheduleRetryExhausted();
                    }
                }
            }
        }
        return false;
    }

    private void recordScheduleDispatch(String result) {
        if (reportMetrics != null) {
            reportMetrics.recordScheduleDispatch(result);
        }
    }
}
