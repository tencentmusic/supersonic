package com.tencent.supersonic.headless.server.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.core.adaptor.db.DbAdaptor;
import com.tencent.supersonic.headless.core.adaptor.db.DbAdaptorFactory;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import com.tencent.supersonic.headless.core.pojo.JdbcDataSource;
import com.tencent.supersonic.headless.server.manager.QuartzJobManager;
import com.tencent.supersonic.headless.server.persistence.dataobject.ConnectionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ConnectionEventDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.DataSyncExecutionDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ConnectionEventMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ConnectionMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.DataSyncExecutionMapper;
import com.tencent.supersonic.headless.server.pojo.ConfiguredCatalog;
import com.tencent.supersonic.headless.server.pojo.ConnectionEventType;
import com.tencent.supersonic.headless.server.pojo.ConnectionStatus;
import com.tencent.supersonic.headless.server.pojo.DiscoveredSchema;
import com.tencent.supersonic.headless.server.pojo.DiscoveredSchema.DiscoveredColumn;
import com.tencent.supersonic.headless.server.pojo.DiscoveredSchema.DiscoveredTable;
import com.tencent.supersonic.headless.server.pojo.PoolType;
import com.tencent.supersonic.headless.server.pojo.ScheduleType;
import com.tencent.supersonic.headless.server.pojo.SchemaChange;
import com.tencent.supersonic.headless.server.pojo.SchemaChangeStatus;
import com.tencent.supersonic.headless.server.pojo.SyncMode;
import com.tencent.supersonic.headless.server.service.ConnectionService;
import com.tencent.supersonic.headless.server.service.DatabaseService;
import com.tencent.supersonic.headless.server.service.SchemaChangeDetector;
import com.tencent.supersonic.headless.server.task.ConnectionSyncJob;
import com.tencent.supersonic.headless.server.utils.DatabaseConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobDataMap;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ConnectionServiceImpl extends ServiceImpl<ConnectionMapper, ConnectionDO>
        implements ConnectionService {

    private static final String GROUP = "CONNECTION";
    private static final String KEY_PREFIX = "connection_";
    private static final int DEFAULT_BATCH_SIZE = 5000;
    private static final int DEFAULT_FETCH_SIZE = 1000;

    private final QuartzJobManager quartzJobManager;
    private final ConnectionEventMapper eventMapper;
    private final DataSyncExecutionMapper executionMapper;
    private final DatabaseService databaseService;
    private final JdbcDataSource jdbcDataSource;
    private final SchemaChangeDetector schemaChangeDetector;

    public ConnectionServiceImpl(QuartzJobManager quartzJobManager,
            ConnectionEventMapper eventMapper, DataSyncExecutionMapper executionMapper,
            DatabaseService databaseService, JdbcDataSource jdbcDataSource,
            SchemaChangeDetector schemaChangeDetector) {
        this.quartzJobManager = quartzJobManager;
        this.eventMapper = eventMapper;
        this.executionMapper = executionMapper;
        this.databaseService = databaseService;
        this.jdbcDataSource = jdbcDataSource;
        this.schemaChangeDetector = schemaChangeDetector;
    }

    // ========== CRUD Operations ==========

    @Override
    public ConnectionDO createConnection(ConnectionDO connection, User user) {
        connection.setCreatedAt(new Date());
        connection.setUpdatedAt(new Date());
        connection.setCreatedBy(user.getName());
        connection.setTenantId(user.getTenantId());

        if (connection.getStatus() == null) {
            connection.setStatus(ConnectionStatus.ACTIVE.name());
        }
        if (connection.getRetryCount() == null) {
            connection.setRetryCount(3);
        }
        if (connection.getScheduleType() == null) {
            connection.setScheduleType(ScheduleType.MANUAL.name());
        }
        if (connection.getSchemaChangeStatus() == null) {
            connection.setSchemaChangeStatus(SchemaChangeStatus.NO_CHANGE.name());
        }

        baseMapper.insert(connection);

        // Create Quartz job if scheduled
        if (!ScheduleType.MANUAL.name().equals(connection.getScheduleType())
                && StringUtils.isNotBlank(connection.getCronExpression())) {
            createQuartzJob(connection);
        }

        recordEvent(connection.getId(), ConnectionEventType.CONFIG_UPDATED, user,
                JSON.toJSONString(Map.of("action", "CREATED")));

        log.info("Connection created: id={}, name={}", connection.getId(), connection.getName());
        return connection;
    }

    @Override
    public ConnectionDO updateConnection(Long id, ConnectionDO connection, User user) {
        ConnectionDO existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Connection not found: " + id);
        }

        // Preserve immutable fields
        connection.setId(id);
        connection.setCreatedAt(existing.getCreatedAt());
        connection.setCreatedBy(existing.getCreatedBy());
        connection.setTenantId(existing.getTenantId());
        connection.setUpdatedAt(new Date());
        connection.setUpdatedBy(user.getName());

        // Handle schedule changes
        boolean scheduleChanged = !StringUtils.equals(existing.getCronExpression(),
                connection.getCronExpression())
                || !StringUtils.equals(existing.getScheduleType(), connection.getScheduleType());

        if (scheduleChanged) {
            // Delete old job if exists
            if (StringUtils.isNotBlank(existing.getQuartzJobKey())) {
                quartzJobManager.deleteJob(existing.getQuartzJobKey());
                connection.setQuartzJobKey(null);
            }

            // Create new job if scheduled
            if (!ScheduleType.MANUAL.name().equals(connection.getScheduleType())
                    && StringUtils.isNotBlank(connection.getCronExpression())) {
                createQuartzJob(connection);
            }
        } else {
            connection.setQuartzJobKey(existing.getQuartzJobKey());
        }

        baseMapper.updateById(connection);

        recordEvent(id, ConnectionEventType.CONFIG_UPDATED, user,
                JSON.toJSONString(Map.of("action", "UPDATED", "scheduleChanged", scheduleChanged)));

        log.info("Connection updated: id={}", id);
        return connection;
    }

    @Override
    public void deleteConnection(Long id, User user) {
        ConnectionDO connection = baseMapper.selectById(id);
        if (connection == null) {
            return;
        }

        if (StringUtils.isNotBlank(connection.getQuartzJobKey())) {
            quartzJobManager.deleteJob(connection.getQuartzJobKey());
        }

        baseMapper.deleteById(id);
        log.info("Connection deleted: id={}", id);
    }

    @Override
    public ConnectionDO getConnectionById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public Page<ConnectionDO> listConnections(Page<ConnectionDO> page, Long sourceDbId,
            Long destDbId, String status) {
        QueryWrapper<ConnectionDO> wrapper = new QueryWrapper<>();
        if (sourceDbId != null) {
            wrapper.lambda().eq(ConnectionDO::getSourceDatabaseId, sourceDbId);
        }
        if (destDbId != null) {
            wrapper.lambda().eq(ConnectionDO::getDestinationDatabaseId, destDbId);
        }
        if (StringUtils.isNotBlank(status)) {
            wrapper.lambda().eq(ConnectionDO::getStatus, status);
        }
        wrapper.lambda().orderByDesc(ConnectionDO::getCreatedAt);
        return baseMapper.selectPage(page, wrapper);
    }

    // ========== Lifecycle Operations ==========

    @Override
    public void pauseConnection(Long id, User user) {
        ConnectionDO connection = baseMapper.selectById(id);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found: " + id);
        }

        if (StringUtils.isNotBlank(connection.getQuartzJobKey())) {
            quartzJobManager.pauseJob(connection.getQuartzJobKey());
        }

        String previousStatus = connection.getStatus();
        connection.setStatus(ConnectionStatus.PAUSED.name());
        connection.setStatusUpdatedAt(new Date());
        connection.setUpdatedAt(new Date());
        connection.setUpdatedBy(user.getName());
        baseMapper.updateById(connection);

        recordEvent(id, ConnectionEventType.STATUS_CHANGED, user, JSON.toJSONString(
                Map.of("previousStatus", previousStatus, "newStatus", ConnectionStatus.PAUSED)));

        log.info("Connection paused: id={}", id);
    }

    @Override
    public void resumeConnection(Long id, User user) {
        ConnectionDO connection = baseMapper.selectById(id);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found: " + id);
        }

        if (StringUtils.isNotBlank(connection.getQuartzJobKey())) {
            quartzJobManager.resumeJob(connection.getQuartzJobKey());
        }

        String previousStatus = connection.getStatus();
        connection.setStatus(ConnectionStatus.ACTIVE.name());
        connection.setStatusUpdatedAt(new Date());
        connection.setStatusMessage(null);
        connection.setUpdatedAt(new Date());
        connection.setUpdatedBy(user.getName());
        baseMapper.updateById(connection);

        recordEvent(id, ConnectionEventType.STATUS_CHANGED, user, JSON.toJSONString(
                Map.of("previousStatus", previousStatus, "newStatus", ConnectionStatus.ACTIVE)));

        log.info("Connection resumed: id={}", id);
    }

    @Override
    public void deprecateConnection(Long id, String reason, User user) {
        ConnectionDO connection = baseMapper.selectById(id);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found: " + id);
        }

        if (StringUtils.isNotBlank(connection.getQuartzJobKey())) {
            quartzJobManager.deleteJob(connection.getQuartzJobKey());
            connection.setQuartzJobKey(null);
        }

        String previousStatus = connection.getStatus();
        connection.setStatus(ConnectionStatus.DEPRECATED.name());
        connection.setStatusUpdatedAt(new Date());
        connection.setStatusMessage(reason);
        connection.setUpdatedAt(new Date());
        connection.setUpdatedBy(user.getName());
        baseMapper.updateById(connection);

        recordEvent(id, ConnectionEventType.STATUS_CHANGED, user,
                JSON.toJSONString(Map.of("previousStatus", previousStatus, "newStatus",
                        ConnectionStatus.DEPRECATED, "reason", reason != null ? reason : "")));

        log.info("Connection deprecated: id={}, reason={}", id, reason);
    }

    @Override
    public void markBroken(Long id, String errorMessage) {
        ConnectionDO connection = baseMapper.selectById(id);
        if (connection == null) {
            return;
        }

        if (StringUtils.isNotBlank(connection.getQuartzJobKey())) {
            quartzJobManager.pauseJob(connection.getQuartzJobKey());
        }

        connection.setStatus(ConnectionStatus.BROKEN.name());
        connection.setStatusUpdatedAt(new Date());
        connection.setStatusMessage(truncateMessage(errorMessage));
        connection.setUpdatedAt(new Date());
        baseMapper.updateById(connection);

        recordEvent(id, ConnectionEventType.STATUS_CHANGED, null, JSON.toJSONString(Map
                .of("newStatus", ConnectionStatus.BROKEN, "error", truncateMessage(errorMessage))));

        log.warn("Connection marked as broken: id={}, error={}", id, errorMessage);
    }

    // ========== Schema Operations ==========

    @Override
    public DiscoveredSchema discoverSchema(Long id, User user) {
        ConnectionDO connection = baseMapper.selectById(id);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found: " + id);
        }

        DatabaseResp sourceDb = databaseService.getDatabase(connection.getSourceDatabaseId());
        DbAdaptor dbAdaptor = DbAdaptorFactory.getEngineAdaptor(sourceDb.getType());
        ConnectInfo connectInfo = DatabaseConverter.getConnectInfo(sourceDb);

        DiscoveredSchema schema = new DiscoveredSchema();
        List<DiscoveredTable> discoveredTables = new ArrayList<>();

        try {
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
        } catch (SQLException e) {
            log.error("Schema discovery failed for connection={}", id, e);
            throw new RuntimeException("Schema discovery failed: " + e.getMessage(), e);
        }

        schema.setTables(discoveredTables);

        // Update connection with discovered schema
        connection.setDiscoveredCatalog(JSON.toJSONString(schema));
        connection.setDiscoveredCatalogAt(new Date());
        connection.setUpdatedAt(new Date());
        if (user != null) {
            connection.setUpdatedBy(user.getName());
        }
        baseMapper.updateById(connection);

        recordEvent(id, ConnectionEventType.SCHEMA_DETECTED, user,
                JSON.toJSONString(Map.of("tableCount", discoveredTables.size())));

        log.info("Discovered {} tables for connection={}", discoveredTables.size(), id);
        return schema;
    }

    @Override
    public SchemaChange detectSchemaChanges(Long id) {
        ConnectionDO connection = baseMapper.selectById(id);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found: " + id);
        }

        SchemaChange result = new SchemaChange();
        result.setStatus(SchemaChangeStatus.NO_CHANGE);
        result.setChanges(new ArrayList<>());

        if (StringUtils.isBlank(connection.getConfiguredCatalog())
                || StringUtils.isBlank(connection.getDiscoveredCatalog())) {
            return result;
        }

        // Parse discovered catalog into DiscoveredSchema
        DiscoveredSchema discoveredSchema;
        try {
            discoveredSchema =
                    JSON.parseObject(connection.getDiscoveredCatalog(), DiscoveredSchema.class);
        } catch (Exception e) {
            log.warn("Failed to parse discovered catalog for connection {}", id, e);
            return result;
        }

        // Delegate to SchemaChangeDetector
        result = schemaChangeDetector.detectChanges(connection.getConfiguredCatalog(),
                discoveredSchema);

        // Update connection if changes detected
        if (result.getStatus() != SchemaChangeStatus.NO_CHANGE) {
            connection.setSchemaChangeStatus(result.getStatus().name());
            connection.setSchemaChangeDetail(JSON.toJSONString(result.getChanges()));
            connection.setUpdatedAt(new Date());
            baseMapper.updateById(connection);
        }

        return result;
    }

    @Override
    public void applySchemaChanges(Long id, ConfiguredCatalog newCatalog, User user) {
        ConnectionDO connection = baseMapper.selectById(id);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found: " + id);
        }

        connection.setConfiguredCatalog(JSON.toJSONString(newCatalog));
        connection.setSchemaChangeStatus(SchemaChangeStatus.NO_CHANGE.name());
        connection.setSchemaChangeDetail(null);
        connection.setUpdatedAt(new Date());
        connection.setUpdatedBy(user.getName());
        baseMapper.updateById(connection);

        recordEvent(id, ConnectionEventType.CONFIG_UPDATED, user,
                JSON.toJSONString(Map.of("action", "SCHEMA_APPLIED")));

        log.info("Schema changes applied for connection={}", id);
    }

    // ========== Sync Operations ==========

    @Override
    public void triggerSync(Long id, User user) {
        ConnectionDO connection = baseMapper.selectById(id);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found: " + id);
        }

        if (ConnectionStatus.DEPRECATED.name().equals(connection.getStatus())) {
            throw new IllegalStateException("Cannot sync deprecated connection: " + id);
        }

        if (StringUtils.isNotBlank(connection.getQuartzJobKey())) {
            quartzJobManager.triggerJob(connection.getQuartzJobKey());
        } else {
            // No quartz job - execute directly (for MANUAL schedule type)
            executeSync(id);
        }

        recordEvent(id, ConnectionEventType.SYNC_STARTED, user,
                JSON.toJSONString(Map.of("triggerType", "MANUAL")));

        log.info("Sync triggered for connection={}", id);
    }

    @Override
    public void executeSync(Long connectionId) {
        ConnectionDO connection = baseMapper.selectById(connectionId);
        if (connection == null) {
            log.warn("Connection not found: {}", connectionId);
            return;
        }

        DataSyncExecutionDO execution = new DataSyncExecutionDO();
        execution.setConnectionId(connectionId);
        execution.setStatus("RUNNING");
        execution.setStartTime(new Date());
        execution.setTenantId(connection.getTenantId());
        executionMapper.insert(execution);

        long totalRowsRead = 0;
        long totalRowsWritten = 0;

        try {
            DatabaseResp sourceDb = databaseService.getDatabase(connection.getSourceDatabaseId());
            DatabaseResp targetDb =
                    databaseService.getDatabase(connection.getDestinationDatabaseId());

            JSONObject catalog = StringUtils.isNotBlank(connection.getConfiguredCatalog())
                    ? JSON.parseObject(connection.getConfiguredCatalog())
                    : new JSONObject();
            JSONArray streams = catalog.getJSONArray("streams");

            if (streams == null || streams.isEmpty()) {
                log.info("No streams configured for connection={}", connectionId);
                execution.setStatus("SUCCESS");
                execution.setEndTime(new Date());
                execution.setRowsRead(0L);
                execution.setRowsWritten(0L);
                executionMapper.updateById(execution);

                recordEvent(connectionId, ConnectionEventType.SYNC_COMPLETED, null,
                        JSON.toJSONString(Map.of("status", "SUCCESS", "rowsRead", 0, "rowsWritten",
                                0, "jobId", execution.getId())));
                return;
            }

            for (int i = 0; i < streams.size(); i++) {
                JSONObject stream = streams.getJSONObject(i);
                String sourceTable = stream.getString("streamName");
                String targetTable = stream.getString("destinationTable");
                if (StringUtils.isBlank(targetTable)) {
                    targetTable = sourceTable;
                }
                String syncModeStr = stream.getString("syncMode");
                SyncMode syncMode = parseSyncMode(syncModeStr);
                String cursorField = stream.getString("cursorField");
                String primaryKey = stream.getString("primaryKey");
                int batchSize = stream.getIntValue("batchSize");
                if (batchSize <= 0) {
                    batchSize = DEFAULT_BATCH_SIZE;
                }
                String preSql = stream.getString("preSql");
                String columns = stream.getString("columns");
                if (StringUtils.isBlank(columns)) {
                    columns = "*";
                }

                Boolean selected = stream.getBoolean("selected");
                if (Boolean.FALSE.equals(selected)) {
                    continue;
                }

                log.info("Syncing stream: {} -> {} (mode={})", sourceTable, targetTable, syncMode);

                long[] result = syncTable(sourceDb, targetDb, sourceTable, targetTable, columns,
                        syncMode, cursorField, primaryKey, batchSize, preSql, execution,
                        connectionId);
                totalRowsRead += result[0];
                totalRowsWritten += result[1];
            }

            execution.setStatus("SUCCESS");
            execution.setEndTime(new Date());
            execution.setRowsRead(totalRowsRead);
            execution.setRowsWritten(totalRowsWritten);
            executionMapper.updateById(execution);

            // Clear broken status on success
            if (ConnectionStatus.BROKEN.name().equals(connection.getStatus())) {
                connection.setStatus(ConnectionStatus.ACTIVE.name());
                connection.setStatusMessage(null);
                connection.setStatusUpdatedAt(new Date());
                connection.setUpdatedAt(new Date());
                baseMapper.updateById(connection);
            }

            recordEvent(connectionId, ConnectionEventType.SYNC_COMPLETED, null,
                    JSON.toJSONString(Map.of("status", "SUCCESS", "rowsRead", totalRowsRead,
                            "rowsWritten", totalRowsWritten, "jobId", execution.getId())));

            log.info("Sync completed for connection={}: read={}, written={}", connectionId,
                    totalRowsRead, totalRowsWritten);

        } catch (Exception e) {
            log.error("Sync failed for connection={}", connectionId, e);
            execution.setStatus("FAILED");
            execution.setEndTime(new Date());
            execution.setRowsRead(totalRowsRead);
            execution.setRowsWritten(totalRowsWritten);
            execution.setErrorMessage(truncateMessage(e.getMessage()));
            executionMapper.updateById(execution);

            // Mark connection as broken after multiple failures
            markBrokenIfNeeded(connectionId, e.getMessage());

            recordEvent(connectionId, ConnectionEventType.SYNC_COMPLETED, null,
                    JSON.toJSONString(Map.of("status", "FAILED", "error",
                            truncateMessage(e.getMessage()), "jobId", execution.getId())));
        }
    }

    private void markBrokenIfNeeded(Long connectionId, String errorMessage) {
        // Check recent failures
        QueryWrapper<DataSyncExecutionDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(DataSyncExecutionDO::getConnectionId, connectionId)
                .eq(DataSyncExecutionDO::getStatus, "FAILED")
                .orderByDesc(DataSyncExecutionDO::getEndTime).last("LIMIT 3");
        List<DataSyncExecutionDO> recentFailures = executionMapper.selectList(wrapper);

        if (recentFailures.size() >= 3) {
            markBroken(connectionId, "Multiple consecutive failures: " + errorMessage);
        }
    }

    private long[] syncTable(DatabaseResp sourceDb, DatabaseResp targetDb, String sourceTable,
            String targetTable, String columns, SyncMode syncMode, String cursorField,
            String primaryKey, int batchSize, String preSql, DataSyncExecutionDO execution,
            Long connectionId) throws SQLException {

        try (Connection sourceConn = createConnection(sourceDb);
                Connection targetConn = createConnection(targetDb)) {

            switch (syncMode) {
                case FULL:
                    executeStatement(targetConn, "TRUNCATE TABLE " + targetTable);
                    break;
                case PARTITION_OVERWRITE:
                    if (StringUtils.isNotBlank(preSql)) {
                        executeStatement(targetConn, preSql);
                    }
                    break;
                default:
                    break;
            }

            String readSql =
                    buildReadSql(columns, sourceTable, syncMode, cursorField, connectionId);
            log.info("Read SQL: {}", readSql);

            sourceConn.setAutoCommit(false);
            long rowsRead = 0;
            long rowsWritten = 0;
            String maxWatermark = null;

            try (Statement stmt = sourceConn.createStatement()) {
                stmt.setFetchSize(DEFAULT_FETCH_SIZE);
                try (ResultSet rs = stmt.executeQuery(readSql)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    String writeSql =
                            buildWriteSql(targetTable, meta, syncMode, primaryKey, targetDb);

                    targetConn.setAutoCommit(false);
                    try (PreparedStatement writeStmt = targetConn.prepareStatement(writeSql)) {

                        while (rs.next()) {
                            rowsRead++;
                            for (int c = 1; c <= colCount; c++) {
                                writeStmt.setObject(c, rs.getObject(c));
                            }
                            writeStmt.addBatch();

                            if (StringUtils.isNotBlank(cursorField)) {
                                String val = rs.getString(cursorField);
                                if (val != null && (maxWatermark == null
                                        || val.compareTo(maxWatermark) > 0)) {
                                    maxWatermark = val;
                                }
                            }

                            if (rowsRead % batchSize == 0) {
                                int[] results = writeStmt.executeBatch();
                                rowsWritten += countInserted(results);
                                targetConn.commit();
                            }
                        }

                        if (rowsRead % batchSize != 0) {
                            int[] results = writeStmt.executeBatch();
                            rowsWritten += countInserted(results);
                            targetConn.commit();
                        }
                    }
                }
            }

            if (maxWatermark != null) {
                execution.setWatermarkValue(maxWatermark);
            }

            log.info("Stream sync complete: {} -> {} read={}, written={}", sourceTable, targetTable,
                    rowsRead, rowsWritten);
            return new long[] {rowsRead, rowsWritten};
        }
    }

    @Override
    public void resetState(Long id, List<String> streamNames, User user) {
        ConnectionDO connection = baseMapper.selectById(id);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found: " + id);
        }

        // Clear state for specified streams or all
        if (StringUtils.isNotBlank(connection.getState())) {
            if (streamNames == null || streamNames.isEmpty()) {
                connection.setState(null);
            } else {
                JSONObject state = JSON.parseObject(connection.getState());
                for (String stream : streamNames) {
                    state.remove(stream);
                }
                connection.setState(state.isEmpty() ? null : state.toJSONString());
            }
        }

        connection.setUpdatedAt(new Date());
        connection.setUpdatedBy(user.getName());
        baseMapper.updateById(connection);

        recordEvent(id, ConnectionEventType.STATE_RESET, user,
                JSON.toJSONString(Map.of("streams", streamNames != null ? streamNames : "ALL")));

        log.info("State reset for connection={}, streams={}", id, streamNames);
    }

    // ========== Timeline & Jobs ==========

    @Override
    public Page<ConnectionEventDO> getTimeline(Long id, Page<ConnectionEventDO> page,
            String eventType) {
        QueryWrapper<ConnectionEventDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ConnectionEventDO::getConnectionId, id);
        if (StringUtils.isNotBlank(eventType)) {
            wrapper.lambda().eq(ConnectionEventDO::getEventType, eventType);
        }
        wrapper.lambda().orderByDesc(ConnectionEventDO::getEventTime);
        return eventMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<DataSyncExecutionDO> getJobHistory(Long connectionId,
            Page<DataSyncExecutionDO> page) {
        QueryWrapper<DataSyncExecutionDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(DataSyncExecutionDO::getConnectionId, connectionId)
                .orderByDesc(DataSyncExecutionDO::getStartTime);
        return executionMapper.selectPage(page, wrapper);
    }

    // ========== Helper Methods ==========

    private void createQuartzJob(ConnectionDO connection) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("connectionId", connection.getId());
        jobDataMap.put("tenantId", connection.getTenantId());

        String quartzJobKey = quartzJobManager.createJob(GROUP, KEY_PREFIX, connection.getId(),
                ConnectionSyncJob.class, connection.getCronExpression(), jobDataMap);

        connection.setQuartzJobKey(quartzJobKey);
        baseMapper.updateById(connection);
    }

    private void recordEvent(Long connectionId, ConnectionEventType eventType, User user,
            String eventData) {
        ConnectionEventDO event = new ConnectionEventDO();
        event.setConnectionId(connectionId);
        event.setEventType(eventType.name());
        event.setEventTime(new Date());
        event.setEventData(eventData);
        if (user != null) {
            event.setUserId(user.getId());
            event.setUserName(user.getName());
        }

        ConnectionDO connection = baseMapper.selectById(connectionId);
        if (connection != null) {
            event.setTenantId(connection.getTenantId());
        }

        eventMapper.insert(event);
    }

    private String buildReadSql(String columns, String sourceTable, SyncMode syncMode,
            String cursorField, Long connectionId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(columns).append(" FROM ").append(sourceTable);

        if ((syncMode == SyncMode.INCREMENTAL || syncMode == SyncMode.INCREMENTAL_DEDUP)
                && StringUtils.isNotBlank(cursorField)) {
            String lastWatermark = getLastSuccessWatermark(connectionId);
            if (lastWatermark != null) {
                sql.append(" WHERE ").append(cursorField).append(" > '").append(lastWatermark)
                        .append("'");
            }
            sql.append(" ORDER BY ").append(cursorField);
        }
        return sql.toString();
    }

    private String getLastSuccessWatermark(Long connectionId) {
        QueryWrapper<DataSyncExecutionDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(DataSyncExecutionDO::getConnectionId, connectionId)
                .eq(DataSyncExecutionDO::getStatus, "SUCCESS")
                .isNotNull(DataSyncExecutionDO::getWatermarkValue)
                .orderByDesc(DataSyncExecutionDO::getEndTime).last("LIMIT 1");
        DataSyncExecutionDO last = executionMapper.selectOne(wrapper);
        return last != null ? last.getWatermarkValue() : null;
    }

    private String buildWriteSql(String targetTable, ResultSetMetaData meta, SyncMode syncMode,
            String primaryKey, DatabaseResp targetDb) throws SQLException {
        int colCount = meta.getColumnCount();
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= colCount; i++) {
            columns.add(meta.getColumnName(i));
        }

        boolean useUpsert =
                (syncMode == SyncMode.INCREMENTAL || syncMode == SyncMode.INCREMENTAL_DEDUP)
                        && StringUtils.isNotBlank(primaryKey);

        if (useUpsert) {
            List<String> primaryKeys = parsePrimaryKeys(primaryKey);
            DbAdaptor dbAdaptor = DbAdaptorFactory.getEngineAdaptor(targetDb.getType());
            return dbAdaptor.buildUpsertSql(targetTable, columns, primaryKeys);
        }

        return buildInsertSql(targetTable, columns);
    }

    private String buildInsertSql(String targetTable, List<String> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(targetTable).append(" (");
        sb.append(String.join(", ", columns));
        sb.append(") VALUES (");
        sb.append(String.join(", ", columns.stream().map(c -> "?").toList()));
        sb.append(")");
        return sb.toString();
    }

    private List<String> parsePrimaryKeys(String primaryKey) {
        List<String> keys = new ArrayList<>();
        if (StringUtils.isBlank(primaryKey)) {
            return keys;
        }
        for (String key : primaryKey.split(",")) {
            String trimmed = key.trim();
            if (StringUtils.isNotBlank(trimmed)) {
                keys.add(trimmed);
            }
        }
        return keys;
    }

    private SyncMode parseSyncMode(String mode) {
        if (StringUtils.isBlank(mode)) {
            return SyncMode.FULL;
        }
        try {
            return SyncMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown sync mode '{}', defaulting to FULL", mode);
            return SyncMode.FULL;
        }
    }

    private Connection createConnection(DatabaseResp database) throws SQLException {
        PoolType syncPool = PoolType.SYNC;
        return jdbcDataSource.getDataSource(database, syncPool.name(), syncPool.getMaxActive(),
                syncPool.getMaxWaitMs()).getConnection();
    }

    private void executeStatement(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            log.info("Execute target SQL: {}", sql);
            stmt.execute(sql);
        }
    }

    private long countInserted(int[] batchResults) {
        long count = 0;
        for (int r : batchResults) {
            if (r >= 0) {
                count += r;
            } else if (r == Statement.SUCCESS_NO_INFO) {
                count++;
            }
        }
        return count;
    }

    private String truncateMessage(String msg) {
        if (msg == null) {
            return "Unknown error";
        }
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
