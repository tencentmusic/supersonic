package com.tencent.supersonic.headless.server.task;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.core.adaptor.db.DbAdaptor;
import com.tencent.supersonic.headless.core.adaptor.db.DbAdaptorFactory;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import com.tencent.supersonic.headless.server.persistence.dataobject.ConnectionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ConnectionEventDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ConnectionEventMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ConnectionMapper;
import com.tencent.supersonic.headless.server.pojo.ConnectionEventType;
import com.tencent.supersonic.headless.server.pojo.ConnectionStatus;
import com.tencent.supersonic.headless.server.pojo.DiscoveredSchema;
import com.tencent.supersonic.headless.server.pojo.DiscoveredSchema.DiscoveredColumn;
import com.tencent.supersonic.headless.server.pojo.DiscoveredSchema.DiscoveredTable;
import com.tencent.supersonic.headless.server.pojo.SchemaChange;
import com.tencent.supersonic.headless.server.pojo.SchemaChangeStatus;
import com.tencent.supersonic.headless.server.service.DatabaseService;
import com.tencent.supersonic.headless.server.service.SchemaChangeDetector;
import com.tencent.supersonic.headless.server.utils.DatabaseConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Scheduled task that detects schema changes for all active connections. Runs daily at 2:00 AM to
 * minimize impact on production workloads.
 * <p>
 * For each active connection: 1. Discovers current schema from source database 2. Compares with
 * configured catalog 3. Updates schemaChangeStatus if changes detected 4. Records a SCHEMA_DETECTED
 * event
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SchemaChangeDetectionTask {

    private final ConnectionMapper connectionMapper;
    private final ConnectionEventMapper eventMapper;
    private final DatabaseService databaseService;
    private final SchemaChangeDetector schemaChangeDetector;

    /**
     * Run schema change detection daily at 2:00 AM.
     */
    @Scheduled(cron = "${s2.connection.schema-detection.cron:0 0 2 * * ?}")
    public void detectSchemaChanges() {
        log.info("Starting scheduled schema change detection task");

        List<ConnectionDO> activeConnections = getActiveConnections();
        log.info("Found {} active connections to check", activeConnections.size());

        int checkedCount = 0;
        int changesDetected = 0;
        int errorsEncountered = 0;

        for (ConnectionDO connection : activeConnections) {
            try {
                boolean hasChanges = checkConnectionSchema(connection);
                checkedCount++;
                if (hasChanges) {
                    changesDetected++;
                }
            } catch (Exception e) {
                log.error("Failed to check schema for connection id={}, name={}",
                        connection.getId(), connection.getName(), e);
                errorsEncountered++;
            }
        }

        log.info("Schema change detection task completed: checked={}, changes={}, errors={}",
                checkedCount, changesDetected, errorsEncountered);
    }

    /**
     * Get all active connections that should be checked.
     */
    private List<ConnectionDO> getActiveConnections() {
        QueryWrapper<ConnectionDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ConnectionDO::getStatus, ConnectionStatus.ACTIVE.name());
        return connectionMapper.selectList(wrapper);
    }

    /**
     * Check schema for a single connection.
     *
     * @return true if changes were detected
     */
    private boolean checkConnectionSchema(ConnectionDO connection) {
        log.debug("Checking schema for connection id={}, name={}", connection.getId(),
                connection.getName());

        // Skip if no configured catalog
        if (StringUtils.isBlank(connection.getConfiguredCatalog())) {
            log.debug("Connection {} has no configured catalog, skipping", connection.getId());
            return false;
        }

        // Discover current schema
        DiscoveredSchema currentSchema;
        try {
            currentSchema = discoverSchema(connection.getSourceDatabaseId());
        } catch (Exception e) {
            log.warn("Failed to discover schema for connection {}: {}", connection.getId(),
                    e.getMessage());
            return false;
        }

        // Detect changes
        SchemaChange change = schemaChangeDetector.detectChanges(connection.getConfiguredCatalog(),
                currentSchema);

        // Update connection if changes detected
        if (change.getStatus() != SchemaChangeStatus.NO_CHANGE) {
            updateConnectionWithChanges(connection, currentSchema, change);
            return true;
        } else {
            // Clear any previous change status
            if (!SchemaChangeStatus.NO_CHANGE.name().equals(connection.getSchemaChangeStatus())) {
                connection.setSchemaChangeStatus(SchemaChangeStatus.NO_CHANGE.name());
                connection.setSchemaChangeDetail(null);
                connection.setUpdatedAt(new Date());
                connectionMapper.updateById(connection);
            }
        }

        return false;
    }

    /**
     * Update connection with detected schema changes.
     */
    private void updateConnectionWithChanges(ConnectionDO connection,
            DiscoveredSchema currentSchema, SchemaChange change) {
        connection.setDiscoveredCatalog(JSON.toJSONString(currentSchema));
        connection.setDiscoveredCatalogAt(new Date());
        connection.setSchemaChangeStatus(change.getStatus().name());
        connection.setSchemaChangeDetail(JSON.toJSONString(change.getChanges()));
        connection.setUpdatedAt(new Date());

        connectionMapper.updateById(connection);

        // Record event
        String summary = schemaChangeDetector.generateChangeSummary(change);
        recordEvent(connection.getId(), connection.getTenantId(), change.getStatus(), summary,
                change.getChanges().size());

        log.info("Detected {} schema changes for connection id={}, name={}: {} change(s)",
                change.getStatus(), connection.getId(), connection.getName(),
                change.getChanges().size());
    }

    /**
     * Discover schema from source database.
     */
    private DiscoveredSchema discoverSchema(Long databaseId) throws SQLException {
        DatabaseResp sourceDb = databaseService.getDatabase(databaseId);
        if (sourceDb == null) {
            throw new IllegalArgumentException("Database not found: " + databaseId);
        }

        DbAdaptor dbAdaptor = DbAdaptorFactory.getEngineAdaptor(sourceDb.getType());
        ConnectInfo connectInfo = DatabaseConverter.getConnectInfo(sourceDb);

        DiscoveredSchema schema = new DiscoveredSchema();
        List<DiscoveredTable> discoveredTables = new ArrayList<>();

        String catalog = sourceDb.getDatabase();
        List<String> tableNames = dbAdaptor.getTables(connectInfo, catalog, catalog);

        for (String tableName : tableNames) {
            DiscoveredTable table = new DiscoveredTable();
            table.setTableName(tableName);

            List<DBColumn> dbColumns =
                    dbAdaptor.getColumns(connectInfo, catalog, catalog, tableName);
            List<DiscoveredColumn> columns = new ArrayList<>();
            for (DBColumn col : dbColumns) {
                DiscoveredColumn dc = new DiscoveredColumn();
                dc.setColumnName(col.getColumnName());
                dc.setColumnType(col.getDataType());
                dc.setColumnComment(col.getComment());
                dc.setNullable(true);
                columns.add(dc);
            }
            table.setColumns(columns);
            discoveredTables.add(table);
        }

        schema.setTables(discoveredTables);
        return schema;
    }

    /**
     * Record a schema detection event.
     */
    private void recordEvent(Long connectionId, Long tenantId, SchemaChangeStatus status,
            String summary, int changeCount) {
        ConnectionEventDO event = new ConnectionEventDO();
        event.setConnectionId(connectionId);
        event.setEventType(ConnectionEventType.SCHEMA_DETECTED.name());
        event.setEventTime(new Date());
        event.setEventData(JSON.toJSONString(
                Map.of("status", status.name(), "changeCount", changeCount, "summary", summary)));
        event.setTenantId(tenantId);

        eventMapper.insert(event);
    }

    /**
     * Manually trigger schema detection for a specific connection. Useful for testing or on-demand
     * checks.
     */
    public SchemaChange detectForConnection(Long connectionId) {
        ConnectionDO connection = connectionMapper.selectById(connectionId);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found: " + connectionId);
        }

        if (StringUtils.isBlank(connection.getConfiguredCatalog())) {
            SchemaChange result = new SchemaChange();
            result.setStatus(SchemaChangeStatus.NO_CHANGE);
            result.setChanges(new ArrayList<>());
            return result;
        }

        DiscoveredSchema currentSchema;
        try {
            currentSchema = discoverSchema(connection.getSourceDatabaseId());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to discover schema: " + e.getMessage(), e);
        }

        SchemaChange change = schemaChangeDetector.detectChanges(connection.getConfiguredCatalog(),
                currentSchema);

        if (change.getStatus() != SchemaChangeStatus.NO_CHANGE) {
            updateConnectionWithChanges(connection, currentSchema, change);
        }

        return change;
    }
}
