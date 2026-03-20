package com.tencent.supersonic.headless.server.task;

import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.server.service.impl.AlertCheckDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class AlertCheckJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long ruleId = context.getMergedJobDataMap().getLong("ruleId");
        Long tenantId = context.getMergedJobDataMap().getLong("tenantId");

        TenantContext.setTenantId(tenantId);
        try {
            AlertCheckDispatcher dispatcher = ContextUtils.getBean(AlertCheckDispatcher.class);
            dispatcher.dispatch(ruleId);
        } catch (Exception e) {
            log.error("AlertCheckJob failed for ruleId={}", ruleId, e);
            throw new JobExecutionException(e);
        } finally {
            TenantContext.clear();
        }
    }
}
