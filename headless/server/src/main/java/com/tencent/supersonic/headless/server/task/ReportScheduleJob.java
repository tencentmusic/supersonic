package com.tencent.supersonic.headless.server.task;

import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.server.service.impl.ReportScheduleDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class ReportScheduleJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long scheduleId = context.getMergedJobDataMap().getLong("scheduleId");
        Object tenantIdObj = context.getMergedJobDataMap().get("tenantId");
        Long tenantId = tenantIdObj instanceof Number ? ((Number) tenantIdObj).longValue() : null;

        boolean manual = context.getMergedJobDataMap().containsKey("manual")
                && Boolean.TRUE.equals(context.getMergedJobDataMap().get("manual"));

        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
        }
        try {
            ReportScheduleDispatcher dispatcher =
                    ContextUtils.getBean(ReportScheduleDispatcher.class);
            dispatcher.dispatch(scheduleId, manual);
        } catch (Exception e) {
            log.error("ReportScheduleJob failed for scheduleId={}", scheduleId, e);
            throw new JobExecutionException(e);
        } finally {
            TenantContext.clear();
        }
    }
}
