-- Report delivery configuration table
CREATE TABLE IF NOT EXISTS s2_report_delivery_config (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    delivery_type VARCHAR(50) NOT NULL,
    delivery_config TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    description VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_delivery_config_tenant_id ON s2_report_delivery_config(tenant_id);
CREATE INDEX IF NOT EXISTS idx_delivery_config_type ON s2_report_delivery_config(delivery_type);

COMMENT ON TABLE s2_report_delivery_config IS '报表推送渠道配置';
COMMENT ON COLUMN s2_report_delivery_config.name IS '配置名称';
COMMENT ON COLUMN s2_report_delivery_config.delivery_type IS '推送类型: EMAIL/WEBHOOK/FEISHU/DINGTALK';
COMMENT ON COLUMN s2_report_delivery_config.delivery_config IS 'JSON配置(收件人、Webhook URL等)';
COMMENT ON COLUMN s2_report_delivery_config.enabled IS '是否启用';

-- Report delivery record table (for tracking and idempotency)
CREATE TABLE IF NOT EXISTS s2_report_delivery_record (
    id BIGSERIAL PRIMARY KEY,
    delivery_key VARCHAR(200) NOT NULL,
    schedule_id BIGINT NOT NULL,
    execution_id BIGINT,
    config_id BIGINT NOT NULL,
    delivery_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    file_location VARCHAR(500),
    error_message TEXT,
    retry_count INT DEFAULT 0,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_delivery_record_key ON s2_report_delivery_record(delivery_key);
CREATE INDEX IF NOT EXISTS idx_delivery_record_schedule_id ON s2_report_delivery_record(schedule_id);
CREATE INDEX IF NOT EXISTS idx_delivery_record_execution_id ON s2_report_delivery_record(execution_id);
CREATE INDEX IF NOT EXISTS idx_delivery_record_config_id ON s2_report_delivery_record(config_id);
CREATE INDEX IF NOT EXISTS idx_delivery_record_status ON s2_report_delivery_record(status);
CREATE INDEX IF NOT EXISTS idx_delivery_record_tenant_id ON s2_report_delivery_record(tenant_id);

COMMENT ON TABLE s2_report_delivery_record IS '报表推送记录';
COMMENT ON COLUMN s2_report_delivery_record.delivery_key IS '幂等键: schedule_id + execution_time + channel_id';
COMMENT ON COLUMN s2_report_delivery_record.status IS 'PENDING/SENDING/SUCCESS/FAILED';
