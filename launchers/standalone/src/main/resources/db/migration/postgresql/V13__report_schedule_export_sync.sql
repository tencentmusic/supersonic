-- ============================================================
-- Phase 2: Report Schedule, Execution, Export Task, Data Sync
-- ============================================================

-- Report schedule configuration
CREATE TABLE IF NOT EXISTS s2_report_schedule (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    dataset_id BIGINT NOT NULL,
    query_config TEXT,
    output_format VARCHAR(20) DEFAULT 'EXCEL',
    cron_expression VARCHAR(100) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    owner_id BIGINT,
    retry_count INT DEFAULT 3,
    retry_interval INT DEFAULT 30,
    template_version BIGINT,
    delivery_config_ids VARCHAR(500),
    quartz_job_key VARCHAR(200),
    last_execution_time TIMESTAMP,
    next_execution_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    tenant_id BIGINT NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_report_schedule_tenant ON s2_report_schedule(tenant_id);
CREATE INDEX IF NOT EXISTS idx_report_schedule_dataset ON s2_report_schedule(dataset_id);

-- Report execution records
CREATE TABLE IF NOT EXISTS s2_report_execution (
    id BIGSERIAL PRIMARY KEY,
    schedule_id BIGINT,
    attempt INT DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    result_location VARCHAR(500),
    error_message VARCHAR(2000),
    row_count BIGINT,
    sql_hash VARCHAR(64),
    tenant_id BIGINT NOT NULL DEFAULT 1,
    execution_snapshot TEXT,
    template_version BIGINT,
    engine_version VARCHAR(50),
    scan_rows BIGINT,
    execution_time_ms BIGINT,
    io_bytes BIGINT
);
CREATE INDEX IF NOT EXISTS idx_report_execution_schedule ON s2_report_execution(schedule_id);
CREATE INDEX IF NOT EXISTS idx_report_execution_tenant ON s2_report_execution(tenant_id);
CREATE INDEX IF NOT EXISTS idx_report_execution_status ON s2_report_execution(status);

-- Export task
CREATE TABLE IF NOT EXISTS s2_export_task (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(200),
    user_id BIGINT NOT NULL,
    dataset_id BIGINT,
    query_config TEXT,
    output_format VARCHAR(20) DEFAULT 'EXCEL',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    file_location VARCHAR(500),
    file_size BIGINT,
    row_count BIGINT,
    error_message VARCHAR(2000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expire_time TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_export_task_user ON s2_export_task(user_id);
CREATE INDEX IF NOT EXISTS idx_export_task_tenant ON s2_export_task(tenant_id);
CREATE INDEX IF NOT EXISTS idx_export_task_status ON s2_export_task(status);

-- Data sync configuration
CREATE TABLE IF NOT EXISTS s2_data_sync_config (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    source_database_id BIGINT NOT NULL,
    target_database_id BIGINT NOT NULL,
    sync_config TEXT,
    cron_expression VARCHAR(100) NOT NULL,
    retry_count INT DEFAULT 3,
    enabled BOOLEAN DEFAULT TRUE,
    quartz_job_key VARCHAR(200),
    created_by VARCHAR(100),
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_data_sync_config_tenant ON s2_data_sync_config(tenant_id);

-- Data sync execution records
CREATE TABLE IF NOT EXISTS s2_data_sync_execution (
    id BIGSERIAL PRIMARY KEY,
    sync_config_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    rows_read BIGINT,
    rows_written BIGINT,
    watermark_value VARCHAR(200),
    error_message VARCHAR(2000),
    tenant_id BIGINT NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_data_sync_execution_config ON s2_data_sync_execution(sync_config_id);
CREATE INDEX IF NOT EXISTS idx_data_sync_execution_tenant ON s2_data_sync_execution(tenant_id);
