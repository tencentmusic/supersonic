-- ========================================
-- 多租户支持迁移脚本 (MySQL)
-- 版本: V1
-- 说明: 添加多租户SaaS支持
-- ========================================

-- ========================================
-- PHASE 1: 创建租户相关表
-- ========================================

-- 订阅计划表
CREATE TABLE IF NOT EXISTS `s2_subscription_plan` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(100) NOT NULL COMMENT '计划名称',
    `code` varchar(50) NOT NULL COMMENT '计划编码',
    `description` varchar(500) DEFAULT NULL COMMENT '计划描述',
    `price_monthly` decimal(10, 2) DEFAULT 0 COMMENT '月度价格',
    `price_yearly` decimal(10, 2) DEFAULT 0 COMMENT '年度价格',
    `max_users` int DEFAULT -1 COMMENT '最大用户数 (-1=无限制)',
    `max_datasets` int DEFAULT -1 COMMENT '最大数据集数',
    `max_models` int DEFAULT -1 COMMENT '最大模型数',
    `max_agents` int DEFAULT -1 COMMENT '最大智能体数',
    `max_api_calls_per_day` int DEFAULT -1 COMMENT '每日最大API调用数',
    `max_tokens_per_month` bigint DEFAULT -1 COMMENT '每月最大Token数',
    `features` text DEFAULT NULL COMMENT '功能特性列表(JSON)',
    `is_default` tinyint DEFAULT 0 COMMENT '是否默认计划',
    `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '计划状态',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subscription_plan_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅计划定义表';

-- 租户表 (quota columns removed - plan quotas managed via s2_tenant_subscription + s2_subscription_plan)
CREATE TABLE IF NOT EXISTS `s2_tenant` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL COMMENT '租户名称',
    `code` varchar(100) NOT NULL COMMENT '租户编码',
    `description` varchar(500) DEFAULT NULL COMMENT '租户描述',
    `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '租户状态: ACTIVE, SUSPENDED, DELETED',
    `contact_email` varchar(255) DEFAULT NULL COMMENT '联系邮箱',
    `contact_name` varchar(100) DEFAULT NULL COMMENT '联系人姓名',
    `contact_phone` varchar(50) DEFAULT NULL COMMENT '联系电话',
    `logo_url` varchar(500) DEFAULT NULL COMMENT 'Logo URL',
    `settings` text DEFAULT NULL COMMENT '租户设置(JSON)',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户主表';

-- 租户订阅记录表
CREATE TABLE IF NOT EXISTS `s2_tenant_subscription` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `tenant_id` bigint NOT NULL COMMENT '租户ID',
    `plan_id` bigint NOT NULL COMMENT '订阅计划ID',
    `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '订阅状态',
    `start_date` datetime NOT NULL COMMENT '订阅开始日期',
    `end_date` datetime DEFAULT NULL COMMENT '订阅结束日期',
    `billing_cycle` varchar(20) DEFAULT 'MONTHLY' COMMENT '计费周期',
    `auto_renew` tinyint DEFAULT 1 COMMENT '是否自动续费',
    `payment_method` varchar(50) DEFAULT NULL COMMENT '支付方式',
    `payment_reference` varchar(255) DEFAULT NULL COMMENT '支付参考号',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_subscription_tenant` (`tenant_id`),
    KEY `idx_tenant_subscription_plan` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户订阅记录表';

-- 租户使用量统计表
CREATE TABLE IF NOT EXISTS `s2_tenant_usage` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `tenant_id` bigint NOT NULL COMMENT '租户ID',
    `usage_date` date NOT NULL COMMENT '使用日期',
    `api_calls` int DEFAULT 0 COMMENT 'API调用次数',
    `tokens_used` bigint DEFAULT 0 COMMENT '使用Token数',
    `query_count` int DEFAULT 0 COMMENT '查询次数',
    `storage_bytes` bigint DEFAULT 0 COMMENT '存储字节数',
    `active_users` int DEFAULT 0 COMMENT '活跃用户数',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_usage_date` (`tenant_id`, `usage_date`),
    KEY `idx_tenant_usage_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户使用量统计表';

-- 租户邀请表
CREATE TABLE IF NOT EXISTS `s2_tenant_invitation` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `tenant_id` bigint NOT NULL COMMENT '租户ID',
    `email` varchar(255) NOT NULL COMMENT '被邀请人邮箱',
    `role_id` bigint DEFAULT NULL COMMENT '分配角色ID',
    `token` varchar(255) NOT NULL COMMENT '邀请令牌',
    `status` varchar(20) DEFAULT 'PENDING' COMMENT '邀请状态',
    `expires_at` datetime NOT NULL COMMENT '过期时间',
    `invited_by` varchar(100) DEFAULT NULL COMMENT '邀请人',
    `accepted_at` datetime DEFAULT NULL COMMENT '接受时间',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_invitation_token` (`token`),
    KEY `idx_tenant_invitation_email` (`email`),
    KEY `idx_tenant_invitation_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户用户邀请表';

-- ========================================
-- PHASE 2: 创建用户表（如果V0没有创建）
-- ========================================

-- 用户表
CREATE TABLE IF NOT EXISTS `s2_user` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(100) NOT NULL COMMENT '用户名',
    `display_name` varchar(100) DEFAULT NULL COMMENT '显示名称',
    `password` varchar(256) DEFAULT NULL COMMENT '密码',
    `salt` varchar(256) DEFAULT NULL COMMENT '密码盐',
    `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
    `phone` varchar(50) DEFAULT NULL COMMENT '手机号',
    `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像URL',
    `is_admin` tinyint DEFAULT 0 COMMENT '是否系统管理员',
    `status` tinyint DEFAULT 1 COMMENT '状态: 1=启用, 0=禁用',
    `last_login` datetime DEFAULT NULL COMMENT '最后登录时间',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_name_tenant` (`name`, `tenant_id`),
    KEY `idx_user_tenant` (`tenant_id`),
    KEY `idx_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 用户令牌表
CREATE TABLE IF NOT EXISTS `s2_user_token` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL COMMENT '令牌名称',
    `user_name` varchar(255) NOT NULL COMMENT '用户名',
    `expire_time` bigint NOT NULL COMMENT '过期时间(时间戳)',
    `token` text NOT NULL COMMENT '令牌值',
    `salt` varchar(255) DEFAULT NULL COMMENT '盐值',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time` datetime NOT NULL,
    `create_by` varchar(255) NOT NULL,
    `update_time` datetime DEFAULT NULL,
    `update_by` varchar(255) NOT NULL,
    `expire_date_time` datetime NOT NULL COMMENT '过期日期时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name_username_tenant` (`name`, `user_name`, `tenant_id`),
    KEY `idx_user_token_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户令牌信息表';
