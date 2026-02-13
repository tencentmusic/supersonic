-- ========================================
-- RBAC权限管理迁移脚本 (MySQL)
-- 版本: V2
-- 说明: 添加角色权限管理(RBAC)支持
-- ========================================

-- ========================================
-- 1: 创建RBAC表
-- ========================================

-- 角色表
CREATE TABLE IF NOT EXISTS `s2_role` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(100) NOT NULL COMMENT '角色名称',
    `code` varchar(50) NOT NULL COMMENT '角色编码',
    `description` varchar(500) DEFAULT NULL COMMENT '角色描述',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `is_system` tinyint DEFAULT 0 COMMENT '是否系统内置角色',
    `status` tinyint DEFAULT 1 COMMENT '状态: 1=启用, 0=禁用',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code_tenant` (`code`, `tenant_id`),
    KEY `idx_role_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色定义表';

-- 权限表
CREATE TABLE IF NOT EXISTS `s2_permission` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(100) NOT NULL COMMENT '权限名称',
    `code` varchar(100) NOT NULL COMMENT '权限编码',
    `type` varchar(50) NOT NULL COMMENT '权限类型: MENU, BUTTON, API, DATA',
    `parent_id` bigint DEFAULT NULL COMMENT '父权限ID',
    `path` varchar(255) DEFAULT NULL COMMENT '菜单路径或API路径',
    `icon` varchar(100) DEFAULT NULL COMMENT '菜单图标',
    `sort_order` int DEFAULT 0 COMMENT '排序号',
    `description` varchar(500) DEFAULT NULL COMMENT '权限描述',
    `status` tinyint DEFAULT 1 COMMENT '状态: 1=启用, 0=禁用',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`code`),
    KEY `idx_permission_parent` (`parent_id`),
    KEY `idx_permission_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限定义表';

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS `s2_role_permission` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `permission_id` bigint NOT NULL COMMENT '权限ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_permission_role` (`role_id`),
    KEY `idx_role_permission_perm` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联表';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS `s2_user_role` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_role_user` (`user_id`),
    KEY `idx_user_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';

-- 资源权限表 (用于数据集、模型等资源级别的权限控制)
CREATE TABLE IF NOT EXISTS `s2_resource_permission` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `resource_type` varchar(50) NOT NULL COMMENT '资源类型: DOMAIN, MODEL, DATASET, AGENT, DATABASE',
    `resource_id` bigint NOT NULL COMMENT '资源ID',
    `principal_type` varchar(20) NOT NULL COMMENT '主体类型: USER, ROLE',
    `principal_id` bigint NOT NULL COMMENT '主体ID (用户ID或角色ID)',
    `permission_type` varchar(20) NOT NULL COMMENT '权限类型: ADMIN, EDIT, VIEW',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_resource_principal_perm` (`resource_type`, `resource_id`, `principal_type`, `principal_id`, `permission_type`),
    KEY `idx_resource_permission_resource` (`resource_type`, `resource_id`),
    KEY `idx_resource_permission_principal` (`principal_type`, `principal_id`),
    KEY `idx_resource_permission_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资源级权限控制表';
