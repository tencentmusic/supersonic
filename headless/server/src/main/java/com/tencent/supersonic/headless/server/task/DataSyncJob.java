package com.tencent.supersonic.headless.server.task;

import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.server.service.DataSyncService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
@DisallowConcurrentExecution
public class DataSyncJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long configId = context.getMergedJobDataMap().getLong("syncConfigId");
        Long tenantId = context.getMergedJobDataMap().getLong("tenantId");

        TenantContext.setTenantId(tenantId);
        try {
            DataSyncService service = ContextUtils.getBean(DataSyncService.class);
            service.executeSync(configId);
        } catch (Exception e) {
            log.error("DataSyncJob failed for configId={}", configId, e);
            throw new JobExecutionException(e);
        } finally {
            TenantContext.clear();
        }
    }
}
