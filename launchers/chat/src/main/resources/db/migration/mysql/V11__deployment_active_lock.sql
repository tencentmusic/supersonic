-- Add active_lock column for deployment concurrency control
-- Non-null (templateId_tenantId) when PENDING/RUNNING, NULL when SUCCESS/FAILED
-- Unique constraint ensures only one active deployment per template+tenant
-- Note: Made idempotent - columns may already exist from previous runs

-- Add active_lock column if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_semantic_deployment' AND COLUMN_NAME = 'active_lock');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `s2_semantic_deployment` ADD COLUMN `active_lock` varchar(100) DEFAULT NULL COMMENT ''部署并发锁: PENDING/RUNNING时为templateId_tenantId, 否则为NULL''',
    'SELECT ''Column active_lock already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add unique key if not exists
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_semantic_deployment' AND INDEX_NAME = 'uk_deployment_active_lock');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `s2_semantic_deployment` ADD UNIQUE KEY `uk_deployment_active_lock` (`active_lock`)',
    'SELECT ''Index uk_deployment_active_lock already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add current_step column if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_semantic_deployment' AND COLUMN_NAME = 'current_step');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `s2_semantic_deployment` ADD COLUMN `current_step` varchar(50) DEFAULT NULL COMMENT ''当前执行步骤''',
    'SELECT ''Column current_step already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
