-- Feishu bot user mapping table
CREATE TABLE IF NOT EXISTS s2_feishu_user_mapping (
    id BIGSERIAL PRIMARY KEY,
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
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_feishu_open_id UNIQUE (feishu_open_id)
);
CREATE INDEX IF NOT EXISTS idx_s2_feishu_user_mapping_s2_user_id ON s2_feishu_user_mapping(s2_user_id);

-- Feishu bot query session table (no tenant_id — excluded from tenant filtering)
CREATE TABLE IF NOT EXISTS s2_feishu_query_session (
    id BIGSERIAL PRIMARY KEY,
    feishu_open_id VARCHAR(64) NOT NULL,
    feishu_message_id VARCHAR(64) NOT NULL,
    query_text TEXT NOT NULL,
    query_result_id BIGINT,
    sql_text TEXT,
    row_count INT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_s2_feishu_query_session_open_id_created ON s2_feishu_query_session(feishu_open_id, created_at DESC);

-- Add employee_id column to s2_user for auto-matching
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 's2_user' AND column_name = 'employee_id') THEN
        ALTER TABLE s2_user ADD COLUMN employee_id VARCHAR(64) DEFAULT NULL;
    END IF;
END $$;
