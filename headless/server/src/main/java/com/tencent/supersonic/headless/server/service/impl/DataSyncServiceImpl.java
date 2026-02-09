package com.tencent.supersonic.headless.server.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.core.adaptor.db.DbAdaptor;
import com.tencent.supersonic.headless.core.adaptor.db.DbAdaptorFactory;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import com.tencent.supersonic.headless.core.pojo.JdbcDataSource;
import com.tencent.supersonic.headless.server.manager.QuartzJobManager;
import com.tencent.supersonic.headless.server.persistence.dataobject.DataSyncConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.DataSyncExecutionDO;
import com.tencent.supersonic.headless.server.persistence.mapper.DataSyncConfigMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.DataSyncExecutionMapper;
import com.tencent.supersonic.headless.server.pojo.DiscoveredSchema;
import com.tencent.supersonic.headless.server.pojo.DiscoveredSchema.DiscoveredColumn;
import com.tencent.supersonic.headless.server.pojo.DiscoveredSchema.DiscoveredTable;
import com.tencent.supersonic.headless.server.pojo.PoolType;
import com.tencent.supersonic.headless.server.pojo.SyncMode;
import com.tencent.supersonic.headless.server.service.DataSyncService;
import com.tencent.supersonic.headless.server.service.DatabaseService;
import com.tencent.supersonic.headless.server.task.DataSyncJob;
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

/**
 * Legacy DataSync service implementation.
 * <p>
 * <b>DEPRECATED:</b> This service is deprecated. Please migrate to
 * {@link com.tencent.supersonic.headless.server.service.ConnectionService}.
 * </p>
 * <p>
 * The new Connection model provides: - Independent lifecycle management (ACTIVE/PAUSED/BROKEN) -
 * Schema change detection (breaking vs non-breaking changes) - Timeline event tracking - Improved
 * watermark/checkpoint management
 * </p>
 *
 * @deprecated Use ConnectionService instead. Will be removed in version 2.0.
 */
@Service
@Slf4j
@Deprecated(since = "1.5", forRemoval = true)
public class DataSyncServiceImpl extends ServiceImpl<DataSyncConfigMapper, DataSyncConfigDO>
        implements DataSyncService {

    private static final String GROUP = "DATA_SYNC";
    private static final String KEY_PREFIX = "data_sync_";
    private static final int DEFAULT_BATCH_SIZE = 5000;
    private static final int DEFAULT_FETCH_SIZE = 1000;
    private static final String DEPRECATION_LOG_MSG =
            "DataSyncService is deprecated. Please migrate to ConnectionService (/api/v1/connections).";

    private final QuartzJobManager quartzJobManager;
    private final DataSyncExecutionMapper executionMapper;
    private final DatabaseService databaseService;
    private final JdbcDataSource jdbcDataSource;

    public DataSyncServiceImpl(QuartzJobManager quartzJobManager,
            DataSyncExecutionMapper executionMapper, DatabaseService databaseService,
            JdbcDataSource jdbcDataSource) {
        this.quartzJobManager = quartzJobManager;
        this.executionMapper = executionMapper;
        this.databaseService = databaseService;
        this.jdbcDataSource = jdbcDataSource;
    }

    @Override
    public DataSyncConfigDO createSyncConfig(DataSyncConfigDO config) {
        log.warn(DEPRECATION_LOG_MSG);
        config.setCreatedAt(new Date());
        config.setUpdatedAt(new Date());
        if (config.getEnabled() == null) {
            config.setEnabled(true);
        }
        if (config.getRetryCount() == null) {
            config.setRetryCount(3);
        }
        baseMapper.insert(config);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("syncConfigId", config.getId());
        jobDataMap.put("tenantId", config.getTenantId());

        String quartzJobKey = quartzJobManager.createJob(GROUP, KEY_PREFIX, config.getId(),
                DataSyncJob.class, config.getCronExpression(), jobDataMap);

        config.setQuartzJobKey(quartzJobKey);
        baseMapper.updateById(config);
        return config;
    }

    @Override
    public DataSyncConfigDO updateSyncConfig(DataSyncConfigDO config) {
        config.setUpdatedAt(new Date());
        baseMapper.updateById(config);
        return config;
    }

    @Override
    public void deleteSyncConfig(Long id) {
        DataSyncConfigDO config = baseMapper.selectById(id);
        if (config == null) {
            return;
        }
        quartzJobManager.deleteJob(config.getQuartzJobKey());
        baseMapper.deleteById(id);
    }

    @Override
    public DataSyncConfigDO getSyncConfigById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public Page<DataSyncConfigDO> getSyncConfigList(Page<DataSyncConfigDO> page) {
        QueryWrapper<DataSyncConfigDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().orderByDesc(DataSyncConfigDO::getCreatedAt);
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public void pauseSync(Long id) {
        DataSyncConfigDO config = baseMapper.selectById(id);
        if (config == null) {
            throw new IllegalArgumentException("Sync config not found: " + id);
        }
        quartzJobManager.pauseJob(config.getQuartzJobKey());
        config.setEnabled(false);
        config.setUpdatedAt(new Date());
        baseMapper.updateById(config);
    }

    @Override
    public void resumeSync(Long id) {
        DataSyncConfigDO config = baseMapper.selectById(id);
        if (config == null) {
            throw new IllegalArgumentException("Sync config not found: " + id);
        }
        quartzJobManager.resumeJob(config.getQuartzJobKey());
        config.setEnabled(true);
        config.setUpdatedAt(new Date());
        baseMapper.updateById(config);
    }

    @Override
    public void triggerSync(Long id) {
        log.warn(DEPRECATION_LOG_MSG);
        DataSyncConfigDO config = baseMapper.selectById(id);
        if (config == null) {
            throw new IllegalArgumentException("Sync config not found: " + id);
        }
        quartzJobManager.triggerJob(config.getQuartzJobKey());
    }

    // ========== executeSync: core data sync logic ==========

    @Override
    public void executeSync(Long configId) {
        DataSyncConfigDO config = baseMapper.selectById(configId);
        if (config == null) {
            log.warn("Sync config not found: {}", configId);
            return;
        }

        DataSyncExecutionDO execution = new DataSyncExecutionDO();
        execution.setSyncConfigId(configId);
        execution.setStatus("RUNNING");
        execution.setStartTime(new Date());
        execution.setTenantId(config.getTenantId());
        executionMapper.insert(execution);

        long totalRowsRead = 0;
        long totalRowsWritten = 0;

        try {
            DatabaseResp sourceDb = databaseService.getDatabase(config.getSourceDatabaseId());
            DatabaseResp targetDb = databaseService.getDatabase(config.getTargetDatabaseId());

            JSONObject syncConfig = StringUtils.isNotBlank(config.getSyncConfig())
                    ? JSON.parseObject(config.getSyncConfig())
                    : new JSONObject();
            JSONArray tables = syncConfig.getJSONArray("tables");

            if (tables == null || tables.isEmpty()) {
                log.info("No tables configured for sync config={}", configId);
                execution.setStatus("SUCCESS");
                execution.setEndTime(new Date());
                execution.setRowsRead(0L);
                execution.setRowsWritten(0L);
                executionMapper.updateById(execution);
                return;
            }

            for (int i = 0; i < tables.size(); i++) {
                JSONObject tableRule = tables.getJSONObject(i);
                String sourceTable = tableRule.getString("source_table");
                String targetTable = tableRule.getString("target_table");
                if (StringUtils.isBlank(targetTable)) {
                    targetTable = sourceTable;
                }
                SyncMode syncMode = parseSyncMode(tableRule.getString("sync_mode"));
                String cursorField = tableRule.getString("cursor_field");
                String primaryKey = tableRule.getString("primary_key");
                int batchSize = tableRule.getIntValue("batch_size");
                if (batchSize <= 0) {
                    batchSize = DEFAULT_BATCH_SIZE;
                }
                String preSql = tableRule.getString("pre_sql");
                String columns = tableRule.getString("columns");
                if (StringUtils.isBlank(columns)) {
                    columns = "*";
                }

                log.info("Syncing table: {} -> {} (mode={})", sourceTable, targetTable, syncMode);

                long[] result = syncTable(sourceDb, targetDb, sourceTable, targetTable, columns,
                        syncMode, cursorField, primaryKey, batchSize, preSql, execution);
                totalRowsRead += result[0];
                totalRowsWritten += result[1];
            }

            execution.setStatus("SUCCESS");
            execution.setEndTime(new Date());
            execution.setRowsRead(totalRowsRead);
            execution.setRowsWritten(totalRowsWritten);
            executionMapper.updateById(execution);

            log.info("Data sync completed for config={}: read={}, written={}", configId,
                    totalRowsRead, totalRowsWritten);
        } catch (Exception e) {
            log.error("Data sync failed for config={}", configId, e);
            execution.setStatus("FAILED");
            execution.setEndTime(new Date());
            execution.setRowsRead(totalRowsRead);
            execution.setRowsWritten(totalRowsWritten);
            execution.setErrorMessage(truncateMessage(e.getMessage()));
            executionMapper.updateById(execution);
        }
    }

    /**
     * Sync a single table from source to target using the specified mode. Returns [rowsRead,
     * rowsWritten]. Uses SYNC pool type with longer timeouts for data synchronization.
     */
    private long[] syncTable(DatabaseResp sourceDb, DatabaseResp targetDb, String sourceTable,
            String targetTable, String columns, SyncMode syncMode, String cursorField,
            String primaryKey, int batchSize, String preSql, DataSyncExecutionDO execution)
            throws SQLException {

        try (Connection sourceConn = createConnection(sourceDb);
                Connection targetConn = createConnection(targetDb)) {

            // Step 1: Execute pre-SQL on target (e.g. TRUNCATE, DELETE partition)
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

            // Step 2: Build read SQL
            String readSql = buildReadSql(columns, sourceTable, syncMode, cursorField,
                    execution.getSyncConfigId());

            log.info("Read SQL: {}", readSql);

            // Step 3: Stream read from source + batch write to target
            sourceConn.setAutoCommit(false);
            long rowsRead = 0;
            long rowsWritten = 0;
            String maxWatermark = null;

            try (Statement stmt = sourceConn.createStatement()) {
                stmt.setFetchSize(DEFAULT_FETCH_SIZE);
                try (ResultSet rs = stmt.executeQuery(readSql)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    // Build write SQL - use UPSERT for INCREMENTAL modes with primary key
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

                            // Track watermark
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
                                log.debug("Batch committed: read={}, written={}", rowsRead,
                                        rowsWritten);
                            }
                        }

                        // Flush remaining
                        if (rowsRead % batchSize != 0) {
                            int[] results = writeStmt.executeBatch();
                            rowsWritten += countInserted(results);
                            targetConn.commit();
                        }
                    }
                }
            }

            // Step 4: Update watermark in execution
            if (maxWatermark != null) {
                execution.setWatermarkValue(maxWatermark);
            }

            log.info("Table sync complete: {} -> {} read={}, written={}", sourceTable, targetTable,
                    rowsRead, rowsWritten);
            return new long[] {rowsRead, rowsWritten};
        }
    }

    private String buildReadSql(String columns, String sourceTable, SyncMode syncMode,
            String cursorField, Long configId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(columns).append(" FROM ").append(sourceTable);

        if ((syncMode == SyncMode.INCREMENTAL || syncMode == SyncMode.INCREMENTAL_DEDUP)
                && StringUtils.isNotBlank(cursorField)) {
            String lastWatermark = getLastSuccessWatermark(configId);
            if (lastWatermark != null) {
                sql.append(" WHERE ").append(cursorField).append(" > '").append(lastWatermark)
                        .append("'");
            }
            sql.append(" ORDER BY ").append(cursorField);
        }
        return sql.toString();
    }

    /**
     * Build the write SQL (INSERT or UPSERT) based on sync mode and database type.
     */
    private String buildWriteSql(String targetTable, ResultSetMetaData meta, SyncMode syncMode,
            String primaryKey, DatabaseResp targetDb) throws SQLException {
        int colCount = meta.getColumnCount();
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= colCount; i++) {
            columns.add(meta.getColumnName(i));
        }

        // Use UPSERT for INCREMENTAL modes when primary key is defined
        boolean useUpsert =
                (syncMode == SyncMode.INCREMENTAL || syncMode == SyncMode.INCREMENTAL_DEDUP)
                        && StringUtils.isNotBlank(primaryKey);

        if (useUpsert) {
            List<String> primaryKeys = parsePrimaryKeys(primaryKey);
            DbAdaptor dbAdaptor = DbAdaptorFactory.getEngineAdaptor(targetDb.getType());
            String upsertSql = dbAdaptor.buildUpsertSql(targetTable, columns, primaryKeys);
            log.info("Using UPSERT SQL: {}", upsertSql);
            return upsertSql;
        }

        // Fall back to simple INSERT
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

    /**
     * Parse comma-separated primary key string into a list.
     */
    private List<String> parsePrimaryKeys(String primaryKey) {
        if (StringUtils.isBlank(primaryKey)) {
            return new ArrayList<>();
        }
        List<String> keys = new ArrayList<>();
        for (String key : primaryKey.split(",")) {
            String trimmed = key.trim();
            if (StringUtils.isNotBlank(trimmed)) {
                keys.add(trimmed);
            }
        }
        return keys;
    }

    // ========== discoverSchema: JDBC metadata discovery ==========

    @Override
    public DiscoveredSchema discoverSchema(Long configId) {
        DataSyncConfigDO config = baseMapper.selectById(configId);
        if (config == null) {
            throw new IllegalArgumentException("Sync config not found: " + configId);
        }

        DatabaseResp sourceDb = databaseService.getDatabase(config.getSourceDatabaseId());
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
                    dc.setNullable(true); // default; JDBC metadata doesn't always reliably provide
                    columns.add(dc);
                }
                table.setColumns(columns);
                discoveredTables.add(table);
            }
        } catch (SQLException e) {
            log.error("Schema discovery failed for sourceDatabaseId={}",
                    config.getSourceDatabaseId(), e);
            throw new RuntimeException("Schema discovery failed: " + e.getMessage(), e);
        }

        schema.setTables(discoveredTables);
        log.info("Discovered {} tables for sync config={}", discoveredTables.size(), configId);
        return schema;
    }

    @Override
    public DiscoveredSchema discoverSchemaByDatabaseId(Long databaseId) {
        DatabaseResp sourceDb = databaseService.getDatabase(databaseId);
        if (sourceDb == null) {
            throw new IllegalArgumentException("Database not found: " + databaseId);
        }

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
            log.error("Schema discovery failed for databaseId={}", databaseId, e);
            throw new RuntimeException("Schema discovery failed: " + e.getMessage(), e);
        }

        schema.setTables(discoveredTables);
        log.info("Discovered {} tables for databaseId={}", discoveredTables.size(), databaseId);
        return schema;
    }

    // ========== Helper methods ==========

    /**
     * Get the watermark value from the last successful execution of this config.
     */
    private String getLastSuccessWatermark(Long configId) {
        QueryWrapper<DataSyncExecutionDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(DataSyncExecutionDO::getSyncConfigId, configId)
                .eq(DataSyncExecutionDO::getStatus, "SUCCESS")
                .isNotNull(DataSyncExecutionDO::getWatermarkValue)
                .orderByDesc(DataSyncExecutionDO::getEndTime).last("LIMIT 1");
        DataSyncExecutionDO last = executionMapper.selectOne(wrapper);
        return last != null ? last.getWatermarkValue() : null;
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

    /**
     * Create a connection using SYNC pool for long-running sync operations.
     */
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
        return msg.length() > 2000 ? msg.substring(0, 2000) : msg;
    }

    @Override
    public Page<DataSyncExecutionDO> getExecutionList(Page<DataSyncExecutionDO> page,
            Long syncConfigId) {
        QueryWrapper<DataSyncExecutionDO> wrapper = new QueryWrapper<>();
        if (syncConfigId != null) {
            wrapper.lambda().eq(DataSyncExecutionDO::getSyncConfigId, syncConfigId);
        }
        wrapper.lambda().orderByDesc(DataSyncExecutionDO::getStartTime);
        return executionMapper.selectPage(page, wrapper);
    }
}
