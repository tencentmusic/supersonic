package com.tencent.supersonic.headless.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.headless.server.persistence.dataobject.DataSyncConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.DataSyncExecutionDO;
import com.tencent.supersonic.headless.server.pojo.DiscoveredSchema;

/**
 * Legacy DataSync service interface.
 *
 * @deprecated Use {@link ConnectionService} instead. This service will be removed in version 2.0.
 */
@Deprecated(since = "1.5", forRemoval = true)
public interface DataSyncService {

    DataSyncConfigDO createSyncConfig(DataSyncConfigDO config);

    DataSyncConfigDO updateSyncConfig(DataSyncConfigDO config);

    void deleteSyncConfig(Long id);

    DataSyncConfigDO getSyncConfigById(Long id);

    Page<DataSyncConfigDO> getSyncConfigList(Page<DataSyncConfigDO> page);

    void pauseSync(Long id);

    void resumeSync(Long id);

    void triggerSync(Long id);

    void executeSync(Long configId);

    Page<DataSyncExecutionDO> getExecutionList(Page<DataSyncExecutionDO> page, Long syncConfigId);

    DiscoveredSchema discoverSchema(Long configId);

    /**
     * Discover schema for a database (without requiring a sync config). Used in the wizard flow
     * before sync config is created.
     */
    DiscoveredSchema discoverSchemaByDatabaseId(Long databaseId);
}
