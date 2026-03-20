CREATE TABLE IF NOT EXISTS s2_report_schedule_confirmation (
    id BIGSERIAL PRIMARY KEY,
    confirm_token VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    chat_id INT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    source_query_id BIGINT DEFAULT NULL,
    source_parse_id INT DEFAULT NULL,
    source_data_set_id BIGINT DEFAULT NULL,
    payload_json TEXT DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expire_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_report_schedule_confirm_token
    ON s2_report_schedule_confirmation(confirm_token);
CREATE INDEX IF NOT EXISTS idx_report_schedule_confirm_user_chat_status
    ON s2_report_schedule_confirmation(user_id, chat_id, status);
CREATE INDEX IF NOT EXISTS idx_report_schedule_confirm_expire_at
    ON s2_report_schedule_confirmation(expire_at);
