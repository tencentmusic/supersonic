-- Add pool_config column to s2_database table for connection pool isolation settings
-- This migration is idempotent - it checks if the column exists before adding

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 's2_database'
    AND COLUMN_NAME = 'pool_config'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE s2_database ADD COLUMN pool_config TEXT COMMENT ''JSON configuration for connection pool settings per pool type''',
    'SELECT ''Column pool_config already exists'''
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
