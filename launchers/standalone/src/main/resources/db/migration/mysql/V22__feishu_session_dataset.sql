-- MySQL does not support ALTER TABLE ... ADD COLUMN IF NOT EXISTS
-- Use PREPARE/EXECUTE pattern (Flyway-compatible, no DELIMITER needed)

-- Add dataset_id column if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_feishu_query_session' AND COLUMN_NAME = 'dataset_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE s2_feishu_query_session ADD COLUMN dataset_id BIGINT DEFAULT NULL',
    'SELECT ''Column dataset_id already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add agent_id column if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_feishu_query_session' AND COLUMN_NAME = 'agent_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE s2_feishu_query_session ADD COLUMN agent_id INT DEFAULT NULL',
    'SELECT ''Column agent_id already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
