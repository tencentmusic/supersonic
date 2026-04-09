package com.tencent.supersonic.headless.server.task;

import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.server.service.ConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @deprecated Use {@link ConnectionSyncJob} instead. Kept temporarily for backward compatibility
 *             with existing Quartz schedules that reference this class name.
 */
@Deprecated(since = "1.5", forRemoval = true)
@Slf4j
@DisallowConcurrentExecution
public class DataSyncJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // Support both legacy key "syncConfigId" and new key "connectionId"
        Long connectionId;
        if (context.getMergedJobDataMap().containsKey("connectionId")) {
            connectionId = context.getMergedJobDataMap().getLong("connectionId");
        } else {
            connectionId = context.getMergedJobDataMap().getLong("syncConfigId");
        }
        Long tenantId = context.getMergedJobDataMap().getLong("tenantId");

        TenantContext.setTenantId(tenantId);
        try {
            log.info("DataSyncJob started: connectionId={}, tenantId={}", connectionId, tenantId);
            ConnectionService service = ContextUtils.getBean(ConnectionService.class);
            service.executeSync(connectionId);
            log.info("DataSyncJob completed: connectionId={}", connectionId);
        } catch (Exception e) {
            log.error("DataSyncJob failed for connectionId={}", connectionId, e);
            throw new JobExecutionException(e);
        } finally {
            TenantContext.clear();
        }
    }
}
