package com.tencent.supersonic.headless.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.ConnectionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ConnectionEventDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.DataSyncExecutionDO;
import com.tencent.supersonic.headless.server.pojo.ConfiguredCatalog;
import com.tencent.supersonic.headless.server.pojo.DiscoveredSchema;
import com.tencent.supersonic.headless.server.pojo.SchemaChange;

import java.util.List;

public interface ConnectionService {

    // CRUD operations
    ConnectionDO createConnection(ConnectionDO connection, User user);

    ConnectionDO updateConnection(Long id, ConnectionDO connection, User user);

    void deleteConnection(Long id, User user);

    ConnectionDO getConnectionById(Long id);

    Page<ConnectionDO> listConnections(Page<ConnectionDO> page, Long sourceDbId, Long destDbId,
            String status);

    // Lifecycle operations
    void pauseConnection(Long id, User user);

    void resumeConnection(Long id, User user);

    void deprecateConnection(Long id, String reason, User user);

    void markBroken(Long id, String errorMessage);

    // Schema operations
    DiscoveredSchema discoverSchema(Long id, User user);

    SchemaChange detectSchemaChanges(Long id);

    void applySchemaChanges(Long id, ConfiguredCatalog newCatalog, User user);

    // Sync operations
    void triggerSync(Long id, User user);

    void executeSync(Long connectionId);

    void resetState(Long id, List<String> streamNames, User user);

    // Timeline
    Page<ConnectionEventDO> getTimeline(Long id, Page<ConnectionEventDO> page, String eventType);

    // Jobs
    Page<DataSyncExecutionDO> getJobHistory(Long connectionId, Page<DataSyncExecutionDO> page);
}
