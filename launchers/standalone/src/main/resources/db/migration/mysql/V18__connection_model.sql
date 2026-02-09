-- ============================================================
-- Phase A: Connection Model (Airbyte-style)
-- Upgrades data sync from database-attached to independent entity
-- Note: Made idempotent - columns/data may already exist
-- ============================================================

-- Connection: Independent first-class entity for data synchronization
CREATE TABLE IF NOT EXISTS `s2_connection` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(200) NOT NULL COMMENT '连接名称',
    `description` varchar(500) DEFAULT NULL COMMENT '连接描述',
    `source_database_id` bigint NOT NULL COMMENT '源数据库ID',
    `destination_database_id` bigint NOT NULL COMMENT '目标数据库ID',

    -- Lifecycle status
    `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PAUSED/BROKEN/DEPRECATED',
    `status_updated_at` datetime DEFAULT NULL COMMENT '状态更新时间',
    `status_message` varchar(500) DEFAULT NULL COMMENT '状态说明(错误信息等)',

    -- Schema configuration (Airbyte catalog format)
    `configured_catalog` text COMMENT 'JSON: 已配置的stream列表',
    `discovered_catalog` text COMMENT 'JSON: 最近发现的schema',
    `discovered_catalog_at` datetime DEFAULT NULL COMMENT 'Schema发现时间',
    `schema_change_status` varchar(20) DEFAULT 'NO_CHANGE' COMMENT 'NO_CHANGE/NON_BREAKING/BREAKING',
    `schema_change_detail` text COMMENT 'JSON: Schema变更详情',

    -- Schedule configuration
    `schedule_type` varchar(20) NOT NULL DEFAULT 'SCHEDULED' COMMENT 'MANUAL/SCHEDULED/CRON',
    `cron_expression` varchar(100) DEFAULT NULL COMMENT 'Cron表达式',
    `schedule_units` int DEFAULT NULL COMMENT '调度间隔数值',
    `schedule_time_unit` varchar(20) DEFAULT NULL COMMENT 'MINUTES/HOURS/DAYS/WEEKS',

    -- Checkpointing
    `state` text COMMENT 'JSON: per-stream watermarks',
    `state_type` varchar(20) DEFAULT 'LEGACY' COMMENT '状态类型',

    -- Retry configuration
    `retry_count` int DEFAULT 3 COMMENT '最大重试次数',
    `advanced_config` text COMMENT 'JSON: 高级配置',

    -- Quartz integration
    `quartz_job_key` varchar(200) DEFAULT NULL COMMENT 'Quartz Job标识',

    -- Audit fields
    `created_by` varchar(100) DEFAULT NULL,
    `updated_by` varchar(100) DEFAULT NULL,
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_connection_tenant` (`tenant_id`),
    KEY `idx_connection_status` (`status`),
    KEY `idx_connection_source_db` (`source_database_id`),
    KEY `idx_connection_dest_db` (`destination_database_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据连接配置(Airbyte模型)';

-- Connection Event: Timeline tracking for audit and debugging
CREATE TABLE IF NOT EXISTS `s2_connection_event` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `connection_id` bigint NOT NULL COMMENT '关联连接ID',
    `event_type` varchar(50) NOT NULL COMMENT 'SYNC_STARTED/SYNC_COMPLETED/STATUS_CHANGED/SCHEMA_DETECTED/STATE_RESET/CONFIG_UPDATED',
    `event_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
    `event_data` text COMMENT 'JSON: 事件详情',
    `user_id` bigint DEFAULT NULL COMMENT '触发用户ID',
    `user_name` varchar(100) DEFAULT NULL COMMENT '触发用户名',
    `job_id` bigint DEFAULT NULL COMMENT '关联的执行记录ID',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',

    PRIMARY KEY (`id`),
    KEY `idx_event_connection` (`connection_id`),
    KEY `idx_event_time` (`event_time`),
    KEY `idx_event_type` (`event_type`),
    KEY `idx_event_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='连接事件时间线';

-- Extend s2_data_sync_execution with connection_id for migration support (idempotent)

-- Add connection_id column if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_data_sync_execution' AND COLUMN_NAME = 'connection_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `s2_data_sync_execution` ADD COLUMN `connection_id` bigint DEFAULT NULL COMMENT ''关联连接ID'' AFTER `sync_config_id`',
    'SELECT ''Column connection_id already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add job_type column if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_data_sync_execution' AND COLUMN_NAME = 'job_type');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `s2_data_sync_execution` ADD COLUMN `job_type` varchar(20) DEFAULT ''SYNC'' COMMENT ''SYNC/RESET/CLEAR'' AFTER `connection_id`',
    'SELECT ''Column job_type already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add attempt_number column if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_data_sync_execution' AND COLUMN_NAME = 'attempt_number');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `s2_data_sync_execution` ADD COLUMN `attempt_number` int DEFAULT 1 COMMENT ''尝试次数'' AFTER `job_type`',
    'SELECT ''Column attempt_number already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add bytes_synced column if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_data_sync_execution' AND COLUMN_NAME = 'bytes_synced');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `s2_data_sync_execution` ADD COLUMN `bytes_synced` bigint DEFAULT NULL COMMENT ''同步字节数'' AFTER `rows_written`',
    'SELECT ''Column bytes_synced already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add index if not exists
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_data_sync_execution' AND INDEX_NAME = 'idx_execution_connection');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `s2_data_sync_execution` ADD KEY `idx_execution_connection` (`connection_id`)',
    'SELECT ''Index idx_execution_connection already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Data migration: Copy existing DataSyncConfig to Connection (only if not already migrated)
INSERT INTO s2_connection (
    name,
    description,
    source_database_id,
    destination_database_id,
    status,
    status_updated_at,
    configured_catalog,
    schedule_type,
    cron_expression,
    retry_count,
    quartz_job_key,
    created_by,
    tenant_id,
    created_at,
    updated_at
)
SELECT
    dsc.name,
    CONCAT('Migrated from DataSyncConfig #', dsc.id),
    dsc.source_database_id,
    dsc.target_database_id,
    CASE WHEN dsc.enabled = 1 THEN 'ACTIVE' ELSE 'PAUSED' END,
    dsc.updated_at,
    dsc.sync_config,
    'CRON',
    dsc.cron_expression,
    dsc.retry_count,
    dsc.quartz_job_key,
    dsc.created_by,
    dsc.tenant_id,
    dsc.created_at,
    dsc.updated_at
FROM s2_data_sync_config dsc
WHERE NOT EXISTS (
    SELECT 1 FROM s2_connection c
    WHERE c.source_database_id = dsc.source_database_id
    AND c.destination_database_id = dsc.target_database_id
    AND c.tenant_id = dsc.tenant_id
);

-- Update execution records to reference new Connection (idempotent - only updates NULL connection_id)
UPDATE s2_data_sync_execution e
JOIN s2_data_sync_config c ON e.sync_config_id = c.id
JOIN s2_connection conn ON conn.source_database_id = c.source_database_id
    AND conn.destination_database_id = c.target_database_id
    AND conn.tenant_id = c.tenant_id
SET e.connection_id = conn.id
WHERE e.sync_config_id IS NOT NULL AND e.connection_id IS NULL;

-- Create migration events for audit trail (only for connections without migration event)
INSERT INTO s2_connection_event (connection_id, event_type, event_time, event_data, tenant_id)
SELECT
    conn.id,
    'CONFIG_UPDATED',
    NOW(),
    JSON_OBJECT('action', 'MIGRATED_FROM_LEGACY', 'original_sync_config_id', (
        SELECT dsc.id FROM s2_data_sync_config dsc
        WHERE dsc.source_database_id = conn.source_database_id
        AND dsc.target_database_id = conn.destination_database_id
        AND dsc.tenant_id = conn.tenant_id
        LIMIT 1
    )),
    conn.tenant_id
FROM s2_connection conn
WHERE NOT EXISTS (
    SELECT 1 FROM s2_connection_event ce
    WHERE ce.connection_id = conn.id
    AND ce.event_type = 'CONFIG_UPDATED'
    AND JSON_EXTRACT(ce.event_data, '$.action') = 'MIGRATED_FROM_LEGACY'
);
