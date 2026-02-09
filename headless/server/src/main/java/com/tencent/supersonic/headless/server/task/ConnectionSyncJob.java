package com.tencent.supersonic.headless.server.task;

import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.server.service.ConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
@DisallowConcurrentExecution
public class ConnectionSyncJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long connectionId = context.getMergedJobDataMap().getLong("connectionId");
        Long tenantId = context.getMergedJobDataMap().getLong("tenantId");

        TenantContext.setTenantId(tenantId);
        try {
            log.info("ConnectionSyncJob started: connectionId={}, tenantId={}", connectionId,
                    tenantId);
            ConnectionService service = ContextUtils.getBean(ConnectionService.class);
            service.executeSync(connectionId);
            log.info("ConnectionSyncJob completed: connectionId={}", connectionId);
        } catch (Exception e) {
            log.error("ConnectionSyncJob failed for connectionId={}", connectionId, e);
            throw new JobExecutionException(e);
        } finally {
            TenantContext.clear();
        }
    }
}
