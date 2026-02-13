-- Add retry scheduling fields and WeChatWork support
-- Note: Made idempotent - columns/indexes may already exist

-- Add max_retries column if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_report_delivery_record' AND COLUMN_NAME = 'max_retries');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE s2_report_delivery_record ADD COLUMN `max_retries` INT DEFAULT 5 COMMENT ''最大重试次数'' AFTER `retry_count`',
    'SELECT ''Column max_retries already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add next_retry_at column if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_report_delivery_record' AND COLUMN_NAME = 'next_retry_at');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE s2_report_delivery_record ADD COLUMN `next_retry_at` DATETIME COMMENT ''下次重试时间'' AFTER `max_retries`',
    'SELECT ''Column next_retry_at already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create index for retry task queries if not exists
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_report_delivery_record' AND INDEX_NAME = 'idx_delivery_record_retry');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_delivery_record_retry ON s2_report_delivery_record(`status`, `next_retry_at`)',
    'SELECT ''Index idx_delivery_record_retry already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
