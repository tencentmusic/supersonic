-- ========================================
-- 数据集权限迁移脚本 (MySQL)
-- 版本: V5
-- 说明: 为数据集添加权限相关字段和权限组表
-- 注意: 列可能已在 V0 baseline 中存在，使用条件检查确保幂等性
-- ========================================

-- ========================================
-- 1: 为 s2_data_set 表添加权限相关字段 (如果不存在)
-- 注意: 使用 TEXT 类型避免行大小超限
-- ========================================

-- Add viewer column if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_data_set' AND COLUMN_NAME = 'viewer');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `s2_data_set` ADD COLUMN `viewer` text DEFAULT NULL COMMENT ''可查看用户''',
    'SELECT ''Column viewer already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add view_org column if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_data_set' AND COLUMN_NAME = 'view_org');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `s2_data_set` ADD COLUMN `view_org` text DEFAULT NULL COMMENT ''可查看组织''',
    'SELECT ''Column view_org already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add is_open column if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 's2_data_set' AND COLUMN_NAME = 'is_open');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `s2_data_set` ADD COLUMN `is_open` tinyint DEFAULT 0 COMMENT ''是否公开''',
    'SELECT ''Column is_open already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========================================
-- 2: 创建数据集权限组表
-- ========================================

CREATE TABLE IF NOT EXISTS `s2_dataset_auth_groups` (
    `group_id` bigint NOT NULL AUTO_INCREMENT,
    `dataset_id` bigint NOT NULL COMMENT '数据集ID',
    `name` varchar(255) DEFAULT NULL COMMENT '权限组名称',
    `auth_rules` text DEFAULT NULL COMMENT '列权限JSON',
    `dimension_filters` text DEFAULT NULL COMMENT '行权限JSON',
    `dimension_filter_description` varchar(500) DEFAULT NULL COMMENT '行权限描述',
    `authorized_users` text DEFAULT NULL COMMENT '授权用户列表',
    `authorized_department_ids` text DEFAULT NULL COMMENT '授权部门ID列表',
    `inherit_from_model` tinyint DEFAULT 1 COMMENT '是否继承Model权限',
    `tenant_id` bigint DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`group_id`),
    KEY `idx_dataset_auth_dataset` (`dataset_id`),
    KEY `idx_dataset_auth_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据集权限组表';
