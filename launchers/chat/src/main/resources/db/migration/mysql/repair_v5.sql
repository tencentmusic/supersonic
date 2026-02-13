-- ========================================
-- V5 迁移失败修复脚本 (MySQL)
-- 说明: 清理 V5 迁移失败后的残留数据，修复 Flyway schema history
-- 使用方法: 手动在 MySQL 客户端执行此脚本
-- ========================================

-- ========================================
-- 1: 清理 s2_data_set 表中可能已添加的字段
-- ========================================

-- 检查并删除 viewer 字段（如果存在）
SET @exist_viewer := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
    AND table_name = 's2_data_set'
    AND column_name = 'viewer'
);
SET @sql_viewer := IF(@exist_viewer > 0,
    'ALTER TABLE s2_data_set DROP COLUMN viewer',
    'SELECT "viewer column does not exist, skipping"'
);
PREPARE stmt FROM @sql_viewer;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并删除 view_org 字段（如果存在）
SET @exist_view_org := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
    AND table_name = 's2_data_set'
    AND column_name = 'view_org'
);
SET @sql_view_org := IF(@exist_view_org > 0,
    'ALTER TABLE s2_data_set DROP COLUMN view_org',
    'SELECT "view_org column does not exist, skipping"'
);
PREPARE stmt FROM @sql_view_org;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并删除 is_open 字段（如果存在）
SET @exist_is_open := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
    AND table_name = 's2_data_set'
    AND column_name = 'is_open'
);
SET @sql_is_open := IF(@exist_is_open > 0,
    'ALTER TABLE s2_data_set DROP COLUMN is_open',
    'SELECT "is_open column does not exist, skipping"'
);
PREPARE stmt FROM @sql_is_open;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========================================
-- 2: 删除 s2_dataset_auth_groups 表（如果存在）
-- ========================================

DROP TABLE IF EXISTS s2_dataset_auth_groups;

-- ========================================
-- 3: 修复 Flyway schema history
-- ========================================

-- 删除 V5 的失败记录
DELETE FROM flyway_schema_history WHERE version = '5';

-- ========================================
-- 4: 验证修复结果
-- ========================================

-- 查看 s2_data_set 表结构
SELECT '=== s2_data_set 表当前字段 ===' AS info;
SELECT column_name, data_type, column_comment
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 's2_data_set'
ORDER BY ordinal_position;

-- 查看 Flyway 迁移历史
SELECT '=== Flyway 迁移历史 ===' AS info;
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- ========================================
-- 修复完成后，重新启动应用即可自动执行 V5 迁移
-- ========================================
