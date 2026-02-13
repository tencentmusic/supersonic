-- ============================================================
-- Phase A: Connection Model (Airbyte-style)
-- Upgrades data sync from database-attached to independent entity
-- ============================================================

-- Connection: Independent first-class entity for data synchronization
CREATE TABLE IF NOT EXISTS s2_connection (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    source_database_id BIGINT NOT NULL,
    destination_database_id BIGINT NOT NULL,

    -- Lifecycle status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    status_updated_at TIMESTAMP,
    status_message VARCHAR(500),

    -- Schema configuration (Airbyte catalog format)
    configured_catalog TEXT,
    discovered_catalog TEXT,
    discovered_catalog_at TIMESTAMP,
    schema_change_status VARCHAR(20) DEFAULT 'NO_CHANGE',
    schema_change_detail TEXT,

    -- Schedule configuration
    schedule_type VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    cron_expression VARCHAR(100),
    schedule_units INT,
    schedule_time_unit VARCHAR(20),

    -- Checkpointing
    state TEXT,
    state_type VARCHAR(20) DEFAULT 'LEGACY',

    -- Retry configuration
    retry_count INT DEFAULT 3,
    advanced_config TEXT,

    -- Quartz integration
    quartz_job_key VARCHAR(200),

    -- Audit fields
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_connection_tenant ON s2_connection(tenant_id);
CREATE INDEX IF NOT EXISTS idx_connection_status ON s2_connection(status);
CREATE INDEX IF NOT EXISTS idx_connection_source_db ON s2_connection(source_database_id);
CREATE INDEX IF NOT EXISTS idx_connection_dest_db ON s2_connection(destination_database_id);

-- Connection Event: Timeline tracking for audit and debugging
CREATE TABLE IF NOT EXISTS s2_connection_event (
    id BIGSERIAL PRIMARY KEY,
    connection_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_data TEXT,
    user_id BIGINT,
    user_name VARCHAR(100),
    job_id BIGINT,
    tenant_id BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_event_connection ON s2_connection_event(connection_id);
CREATE INDEX IF NOT EXISTS idx_event_time ON s2_connection_event(event_time);
CREATE INDEX IF NOT EXISTS idx_event_type ON s2_connection_event(event_type);
CREATE INDEX IF NOT EXISTS idx_event_tenant ON s2_connection_event(tenant_id);

-- Extend s2_data_sync_execution with connection_id for migration support
ALTER TABLE s2_data_sync_execution
ADD COLUMN IF NOT EXISTS connection_id BIGINT,
ADD COLUMN IF NOT EXISTS job_type VARCHAR(20) DEFAULT 'SYNC',
ADD COLUMN IF NOT EXISTS attempt_number INT DEFAULT 1,
ADD COLUMN IF NOT EXISTS bytes_synced BIGINT;

CREATE INDEX IF NOT EXISTS idx_execution_connection ON s2_data_sync_execution(connection_id);

-- Data migration: Copy existing DataSyncConfig to Connection
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
    name,
    'Migrated from DataSyncConfig #' || id,
    source_database_id,
    target_database_id,
    CASE WHEN enabled = true THEN 'ACTIVE' ELSE 'PAUSED' END,
    updated_at,
    sync_config,
    'CRON',
    cron_expression,
    retry_count,
    quartz_job_key,
    created_by,
    tenant_id,
    created_at,
    updated_at
FROM s2_data_sync_config;

-- Update execution records to reference new Connection
UPDATE s2_data_sync_execution e
SET connection_id = conn.id
FROM s2_data_sync_config c, s2_connection conn
WHERE e.sync_config_id = c.id
    AND conn.source_database_id = c.source_database_id
    AND conn.destination_database_id = c.target_database_id
    AND conn.tenant_id = c.tenant_id
    AND e.sync_config_id IS NOT NULL
    AND e.connection_id IS NULL;

-- Create migration events for audit trail
INSERT INTO s2_connection_event (connection_id, event_type, event_time, event_data, tenant_id)
SELECT
    conn.id,
    'CONFIG_UPDATED',
    NOW(),
    jsonb_build_object('action', 'MIGRATED_FROM_LEGACY', 'original_sync_config_id', dsc.id)::text,
    conn.tenant_id
FROM s2_connection conn
LEFT JOIN s2_data_sync_config dsc ON dsc.source_database_id = conn.source_database_id
    AND dsc.target_database_id = conn.destination_database_id
    AND dsc.tenant_id = conn.tenant_id;
