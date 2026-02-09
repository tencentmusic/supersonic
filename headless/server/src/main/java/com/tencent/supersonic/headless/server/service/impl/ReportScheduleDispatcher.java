package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportScheduleMapper;
import com.tencent.supersonic.headless.server.pojo.ReportExecutionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportScheduleDispatcher {

    private final ReportScheduleMapper scheduleMapper;
    private final ReportExecutionOrchestrator orchestrator;
    private final ReportExecutionContextBuilder contextBuilder;

    public void dispatch(Long scheduleId) {
        ReportScheduleDO schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            log.warn("Schedule not found: {}", scheduleId);
            return;
        }
        if (!Boolean.TRUE.equals(schedule.getEnabled())) {
            log.info("Schedule {} is disabled, skipping", scheduleId);
            return;
        }
        executeWithRetry(schedule);

        schedule.setLastExecutionTime(new Date());
        scheduleMapper.updateById(schedule);
    }

    private void executeWithRetry(ReportScheduleDO schedule) {
        int maxAttempts = (schedule.getRetryCount() != null ? schedule.getRetryCount() : 3) + 1;
        int retryInterval = schedule.getRetryInterval() != null ? schedule.getRetryInterval() : 30;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ReportExecutionContext ctx = contextBuilder.buildFromSchedule(schedule, attempt);
                orchestrator.execute(ctx);
                return;
            } catch (Exception e) {
                if (attempt < maxAttempts) {
                    long delay = retryInterval * (1L << (attempt - 1));
                    log.warn("Schedule {} attempt {}/{} failed, retry in {}s", schedule.getId(),
                            attempt, maxAttempts, delay, e);
                    try {
                        Thread.sleep(delay * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted for schedule {}", schedule.getId());
                        return;
                    }
                } else {
                    log.error("Schedule {} all {} attempts exhausted", schedule.getId(),
                            maxAttempts, e);
                }
            }
        }
    }
}
