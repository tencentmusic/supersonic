-- ========================================
-- V5 迁移失败修复脚本 - 简化版 (MySQL)
-- 使用方法: 在 MySQL 客户端手动执行
-- ========================================

-- 步骤1: 删除可能已添加的字段（如果字段不存在会报错，忽略即可）
-- 根据实际情况选择执行，如果字段不存在则跳过
-- ALTER TABLE s2_data_set DROP COLUMN viewer;
-- ALTER TABLE s2_data_set DROP COLUMN view_org;
-- ALTER TABLE s2_data_set DROP COLUMN is_open;

-- 步骤2: 删除可能已创建的表
DROP TABLE IF EXISTS s2_dataset_auth_groups;

-- 步骤3: 删除 Flyway 中 V5 的失败记录（关键步骤）
DELETE FROM flyway_schema_history WHERE version = '5';

-- 步骤4: 验证 Flyway 历史记录
SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;

-- 完成后重新启动应用
