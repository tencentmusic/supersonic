-- Add delivery timing and failure tracking columns for observability
-- Note: Made idempotent - columns/indexes may already exist

-- Add delivery_time_ms column if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_report_delivery_record' AND COLUMN_NAME = 'delivery_time_ms');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE s2_report_delivery_record ADD COLUMN `delivery_time_ms` BIGINT COMMENT ''推送耗时(毫秒)'' AFTER `completed_at`',
    'SELECT ''Column delivery_time_ms already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add consecutive_failures column if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_report_delivery_config' AND COLUMN_NAME = 'consecutive_failures');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE s2_report_delivery_config ADD COLUMN `consecutive_failures` INT DEFAULT 0 COMMENT ''连续失败次数'' AFTER `description`',
    'SELECT ''Column consecutive_failures already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add max_consecutive_failures column if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_report_delivery_config' AND COLUMN_NAME = 'max_consecutive_failures');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE s2_report_delivery_config ADD COLUMN `max_consecutive_failures` INT DEFAULT 5 COMMENT ''最大连续失败次数(超过后自动禁用)'' AFTER `consecutive_failures`',
    'SELECT ''Column max_consecutive_failures already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create index for statistics queries if not exists
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_report_delivery_record' AND INDEX_NAME = 'idx_delivery_record_created_at');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_delivery_record_created_at ON s2_report_delivery_record(`created_at`)',
    'SELECT ''Index idx_delivery_record_created_at already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_report_delivery_record' AND INDEX_NAME = 'idx_delivery_record_status_type');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_delivery_record_status_type ON s2_report_delivery_record(`status`, `delivery_type`)',
    'SELECT ''Index idx_delivery_record_status_type already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
