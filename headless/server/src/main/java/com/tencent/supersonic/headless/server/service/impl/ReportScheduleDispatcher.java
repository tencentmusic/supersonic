package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.headless.server.metrics.TemplateReportMetrics;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportScheduleMapper;
import com.tencent.supersonic.headless.server.pojo.ReportExecutionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportScheduleDispatcher {

    private final ReportScheduleMapper scheduleMapper;
    private final ReportExecutionOrchestrator orchestrator;
    private final ReportExecutionContextBuilder contextBuilder;
    @Autowired(required = false)
    private TemplateReportMetrics reportMetrics;

    public void dispatch(Long scheduleId) {
        ReportScheduleDO schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            log.warn("Schedule not found: {}", scheduleId);
            recordScheduleDispatch("not_found");
            return;
        }
        if (!Boolean.TRUE.equals(schedule.getEnabled())) {
            log.info("Schedule {} is disabled, skipping", scheduleId);
            recordScheduleDispatch("disabled");
            return;
        }
        boolean success = executeWithRetry(schedule);

        schedule.setLastExecutionTime(new Date());
        scheduleMapper.updateById(schedule);
        recordScheduleDispatch(success ? "success" : "error");
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
