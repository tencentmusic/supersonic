-- ========================================
-- 组织架构迁移脚本 (MySQL)
-- 版本: V8
-- 说明: 添加组织架构表和用户-组织关联表
-- ========================================

-- ========================================
-- 1: 组织架构表
-- ========================================

CREATE TABLE IF NOT EXISTS `s2_organization` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `parent_id` bigint DEFAULT 0 COMMENT '父组织ID，根组织为0',
    `name` varchar(100) NOT NULL COMMENT '组织名称',
    `full_name` varchar(500) DEFAULT NULL COMMENT '组织全名（包含父级路径）',
    `is_root` tinyint DEFAULT 0 COMMENT '是否为根组织',
    `sort_order` int DEFAULT 0 COMMENT '排序序号',
    `status` tinyint DEFAULT 1 COMMENT '状态: 1=启用, 0=禁用',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_org_parent_id` (`parent_id`),
    KEY `idx_org_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织架构表';

-- ========================================
-- 2: 用户-组织关联表
-- ========================================

CREATE TABLE IF NOT EXISTS `s2_user_organization` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `organization_id` bigint NOT NULL COMMENT '组织ID',
    `is_primary` tinyint DEFAULT 0 COMMENT '是否为主组织',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_org` (`user_id`, `organization_id`),
    KEY `idx_user_org_user` (`user_id`),
    KEY `idx_user_org_org` (`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-组织关联表';

-- ========================================
-- 3: 初始化默认组织数据
-- ========================================

INSERT INTO `s2_organization` (`id`, `parent_id`, `name`, `full_name`, `is_root`, `sort_order`, `status`, `tenant_id`, `created_by`)
VALUES
(1, 0, 'SuperSonic', 'SuperSonic', 1, 1, 1, 1, 'system'),
(2, 1, 'HR', 'SuperSonic/HR', 0, 1, 1, 1, 'system'),
(3, 1, 'Sales', 'SuperSonic/Sales', 0, 2, 1, 1, 'system'),
(4, 1, 'Marketing', 'SuperSonic/Marketing', 0, 3, 1, 1, 'system')
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `full_name`=VALUES(`full_name`);

-- ========================================
-- 4: 初始化用户-组织关联数据
-- ========================================

INSERT INTO `s2_user_organization` (`user_id`, `organization_id`, `is_primary`, `created_by`)
VALUES
(1, 1, 1, 'system'),  -- admin -> SuperSonic (根组织)
(2, 2, 1, 'system'),  -- jack -> HR
(3, 3, 1, 'system'),  -- tom -> Sales
(4, 4, 1, 'system'),  -- lucy -> Marketing
(5, 3, 1, 'system')   -- alice -> Sales
ON DUPLICATE KEY UPDATE `is_primary`=VALUES(`is_primary`);
