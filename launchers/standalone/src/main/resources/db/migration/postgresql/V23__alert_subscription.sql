-- V23: Alert subscription tables (alert rules, executions, events)

CREATE TABLE IF NOT EXISTS s2_alert_rule (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    dataset_id BIGINT NOT NULL,
    query_config TEXT NOT NULL,
    conditions TEXT NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    enabled SMALLINT DEFAULT 1,
    owner_id BIGINT,
    delivery_config_ids VARCHAR(500),
    silence_minutes INT DEFAULT 60,
    max_consecutive_failures INT DEFAULT 5,
    consecutive_failures INT DEFAULT 0,
    disabled_reason VARCHAR(1000),
    retry_count INT DEFAULT 2,
    retry_interval INT DEFAULT 30,
    quartz_job_key VARCHAR(200),
    last_check_time TIMESTAMP,
    next_check_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    tenant_id BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_alert_rule_tenant ON s2_alert_rule(tenant_id);
CREATE INDEX IF NOT EXISTS idx_alert_rule_dataset ON s2_alert_rule(dataset_id);
CREATE INDEX IF NOT EXISTS idx_alert_rule_enabled ON s2_alert_rule(enabled);

CREATE TABLE IF NOT EXISTS s2_alert_execution (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    total_rows BIGINT DEFAULT 0,
    alerted_rows BIGINT DEFAULT 0,
    silenced_rows BIGINT DEFAULT 0,
    error_message VARCHAR(2000),
    execution_time_ms BIGINT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alert_execution_rule ON s2_alert_execution(rule_id);
CREATE INDEX IF NOT EXISTS idx_alert_execution_status ON s2_alert_execution(status);
CREATE INDEX IF NOT EXISTS idx_alert_execution_tenant ON s2_alert_execution(tenant_id);

CREATE TABLE IF NOT EXISTS s2_alert_event (
    id BIGSERIAL PRIMARY KEY,
    execution_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    condition_index INT NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    alert_key VARCHAR(300) NOT NULL,
    dimension_value VARCHAR(500),
    metric_value DOUBLE PRECISION,
    baseline_value DOUBLE PRECISION,
    deviation_pct DOUBLE PRECISION,
    message TEXT,
    delivery_status VARCHAR(20) DEFAULT 'PENDING',
    silence_until TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alert_event_execution ON s2_alert_event(execution_id);
CREATE INDEX IF NOT EXISTS idx_alert_event_alert_key ON s2_alert_event(alert_key);
CREATE INDEX IF NOT EXISTS idx_alert_event_severity ON s2_alert_event(severity);
CREATE INDEX IF NOT EXISTS idx_alert_event_rule ON s2_alert_event(rule_id);
CREATE INDEX IF NOT EXISTS idx_alert_event_silence ON s2_alert_event(silence_until);
