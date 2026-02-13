-- Feishu bot user mapping table
CREATE TABLE IF NOT EXISTS s2_feishu_user_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feishu_open_id VARCHAR(64) NOT NULL,
    feishu_union_id VARCHAR(64),
    feishu_user_name VARCHAR(128),
    feishu_email VARCHAR(128),
    feishu_mobile VARCHAR(20),
    feishu_employee_id VARCHAR(64),
    s2_user_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    default_agent_id INT,
    match_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_feishu_open_id (feishu_open_id),
    KEY idx_s2_user_id (s2_user_id)
);

-- Feishu bot query session table (no tenant_id — excluded from tenant filtering)
CREATE TABLE IF NOT EXISTS s2_feishu_query_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feishu_open_id VARCHAR(64) NOT NULL,
    feishu_message_id VARCHAR(64) NOT NULL,
    query_text TEXT NOT NULL,
    query_result_id BIGINT,
    sql_text TEXT,
    row_count INT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_open_id_created (feishu_open_id, created_at DESC)
);

-- Add employee_id column to s2_user for auto-matching
-- MySQL does not support ADD COLUMN IF NOT EXISTS; Flyway ensures single execution
ALTER TABLE s2_user ADD COLUMN employee_id VARCHAR(64) DEFAULT NULL;
