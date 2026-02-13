-- Common (auth + billing + infrastructure) schema (MySQL)

-- ========================================
-- SuperSonic Multi-tenant SaaS DDL Schema
-- Version: 2.0
-- Description: 支持多租户SaaS及权限管理功能
-- ========================================

-- ========================================
-- 1. 基础设施表 - 租户与订阅管理
-- ========================================

-- 订阅计划表
CREATE TABLE IF NOT EXISTS `s2_subscription_plan` (
                                                      `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(100) NOT NULL COMMENT '计划名称',
    `code` varchar(50) NOT NULL COMMENT '计划编码',
    `description` varchar(500) DEFAULT NULL COMMENT '计划描述',
    `price_monthly` decimal(10, 2) DEFAULT 0 COMMENT '月度价格',
    `price_yearly` decimal(10, 2) DEFAULT 0 COMMENT '年度价格',
    `max_users` int(11) DEFAULT -1 COMMENT '最大用户数 (-1=无限制)',
    `max_datasets` int(11) DEFAULT -1 COMMENT '最大数据集数',
    `max_models` int(11) DEFAULT -1 COMMENT '最大模型数',
    `max_agents` int(11) DEFAULT -1 COMMENT '最大智能体数',
    `max_api_calls_per_day` int(11) DEFAULT -1 COMMENT '每日最大API调用数',
    `max_tokens_per_month` bigint(20) DEFAULT -1 COMMENT '每月最大Token数',
    `features` text DEFAULT NULL COMMENT '功能特性列表(JSON)',
    `is_default` tinyint DEFAULT 0 COMMENT '是否默认计划',
    `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '计划状态',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subscription_plan_code` (`code`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅计划定义表';


-- 租户表
CREATE TABLE IF NOT EXISTS `s2_tenant` (
                                           `id` bigint(20) NOT NULL AUTO_INCREMENT,
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
                                                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `tenant_id` bigint(20) NOT NULL COMMENT '租户ID',
    `plan_id` bigint(20) NOT NULL COMMENT '订阅计划ID',
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
                                                 `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `tenant_id` bigint(20) NOT NULL COMMENT '租户ID',
    `usage_date` date NOT NULL COMMENT '使用日期',
    `api_calls` int(11) DEFAULT 0 COMMENT 'API调用次数',
    `tokens_used` bigint(20) DEFAULT 0 COMMENT '使用Token数',
    `query_count` int(11) DEFAULT 0 COMMENT '查询次数',
    `storage_bytes` bigint(20) DEFAULT 0 COMMENT '存储字节数',
    `active_users` int(11) DEFAULT 0 COMMENT '活跃用户数',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_usage_date` (`tenant_id`, `usage_date`),
    KEY `idx_tenant_usage_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户使用量统计表';


-- 租户邀请表
CREATE TABLE IF NOT EXISTS `s2_tenant_invitation` (
                                                      `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `tenant_id` bigint(20) NOT NULL COMMENT '租户ID',
    `email` varchar(255) NOT NULL COMMENT '被邀请人邮箱',
    `role_id` bigint(20) DEFAULT NULL COMMENT '分配角色ID',
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
-- 2. 用户与权限管理表 (RBAC)
-- ========================================

-- 用户表
CREATE TABLE IF NOT EXISTS `s2_user` (
                                         `id` bigint(20) NOT NULL AUTO_INCREMENT,
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
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` varchar(100) DEFAULT NULL,
    `employee_id` varchar(64) DEFAULT NULL COMMENT '工号(飞书自动匹配)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_name_tenant` (`name`, `tenant_id`),
    KEY `idx_user_tenant` (`tenant_id`),
    KEY `idx_user_email` (`email`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';


-- 角色表
CREATE TABLE IF NOT EXISTS `s2_role` (
                                         `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(100) NOT NULL COMMENT '角色名称',
    `code` varchar(50) NOT NULL COMMENT '角色编码',
    `description` varchar(500) DEFAULT NULL COMMENT '角色描述',
    `scope` varchar(20) DEFAULT 'TENANT' COMMENT '作用域: PLATFORM=平台级, TENANT=租户级',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `is_system` tinyint DEFAULT 0 COMMENT '是否系统内置角色',
    `status` tinyint DEFAULT 1 COMMENT '状态: 1=启用, 0=禁用',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code_tenant` (`code`, `tenant_id`),
    KEY `idx_role_tenant` (`tenant_id`),
    KEY `idx_role_scope` (`scope`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色定义表';


-- 权限表
CREATE TABLE IF NOT EXISTS `s2_permission` (
                                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(100) NOT NULL COMMENT '权限名称',
    `code` varchar(100) NOT NULL COMMENT '权限编码',
    `type` varchar(50) NOT NULL DEFAULT 'MENU' COMMENT '权限类型: MENU, BUTTON, API, DATA',
    `scope` varchar(20) DEFAULT 'TENANT' COMMENT '作用域: PLATFORM=平台级, TENANT=租户级',
    `parent_id` bigint(20) DEFAULT NULL COMMENT '父权限ID',
    `path` varchar(255) DEFAULT NULL COMMENT '菜单路径或API路径',
    `icon` varchar(100) DEFAULT NULL COMMENT '菜单图标',
    `sort_order` int(11) DEFAULT 0 COMMENT '排序号',
    `description` varchar(500) DEFAULT NULL COMMENT '权限描述',
    `status` tinyint DEFAULT 1 COMMENT '状态: 1=启用, 0=禁用',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`code`),
    KEY `idx_permission_parent` (`parent_id`),
    KEY `idx_permission_type` (`type`),
    KEY `idx_permission_scope` (`scope`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限定义表';


-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS `s2_role_permission` (
                                                    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `role_id` bigint(20) NOT NULL COMMENT '角色ID',
    `permission_id` bigint(20) NOT NULL COMMENT '权限ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_permission_role` (`role_id`),
    KEY `idx_role_permission_perm` (`permission_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联表';


-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS `s2_user_role` (
                                              `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    `role_id` bigint(20) NOT NULL COMMENT '角色ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_role_user` (`user_id`),
    KEY `idx_user_role_role` (`role_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';


-- 资源权限表 (用于数据集、模型等资源级别的权限控制)
CREATE TABLE IF NOT EXISTS `s2_resource_permission` (
                                                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `resource_type` varchar(50) NOT NULL COMMENT '资源类型: DOMAIN, MODEL, DATASET, AGENT, DATABASE',
    `resource_id` bigint(20) NOT NULL COMMENT '资源ID',
    `principal_type` varchar(20) NOT NULL COMMENT '主体类型: USER, ROLE',
    `principal_id` bigint(20) NOT NULL COMMENT '主体ID (用户ID或角色ID)',
    `permission_type` varchar(20) NOT NULL COMMENT '权限类型: ADMIN, EDIT, VIEW',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_resource_principal_perm` (`resource_type`, `resource_id`, `principal_type`, `principal_id`, `permission_type`),
    KEY `idx_resource_permission_resource` (`resource_type`, `resource_id`),
    KEY `idx_resource_permission_principal` (`principal_type`, `principal_id`),
    KEY `idx_resource_permission_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资源级权限控制表';


-- ========================================
-- 3. OAuth与会话管理表
-- ========================================

-- OAuth提供者配置表
CREATE TABLE IF NOT EXISTS `s2_oauth_provider` (
                                                   `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(100) NOT NULL COMMENT '提供者名称',
    `type` varchar(50) NOT NULL COMMENT '提供者类型: GOOGLE, AZURE_AD, KEYCLOAK, GENERIC_OIDC',
    `client_id` varchar(255) NOT NULL COMMENT 'OAuth客户端ID',
    `client_secret` varchar(512) DEFAULT NULL COMMENT 'OAuth客户端密钥',
    `authorization_uri` varchar(500) DEFAULT NULL COMMENT '授权端点',
    `token_uri` varchar(500) DEFAULT NULL COMMENT 'Token端点',
    `user_info_uri` varchar(500) DEFAULT NULL COMMENT '用户信息端点',
    `jwks_uri` varchar(500) DEFAULT NULL COMMENT 'JWKS端点',
    `issuer` varchar(500) DEFAULT NULL COMMENT 'Token签发者',
    `scopes` varchar(500) DEFAULT NULL COMMENT 'OAuth范围',
    `pkce_enabled` tinyint DEFAULT 1 COMMENT '是否启用PKCE',
    `additional_params` text DEFAULT NULL COMMENT '附加参数(JSON)',
    `enabled` tinyint DEFAULT 1 COMMENT '是否启用',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_oauth_provider_name_tenant` (`name`, `tenant_id`),
    KEY `idx_oauth_provider_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OAuth提供者配置表';


-- OAuth状态表
CREATE TABLE IF NOT EXISTS `s2_oauth_state` (
                                                `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `state` varchar(128) NOT NULL COMMENT '状态参数',
    `provider_name` varchar(100) NOT NULL COMMENT 'OAuth提供者名称',
    `code_verifier` varchar(128) DEFAULT NULL COMMENT 'PKCE验证码',
    `redirect_uri` varchar(500) DEFAULT NULL COMMENT '重定向URI',
    `nonce` varchar(128) DEFAULT NULL COMMENT 'OIDC随机数',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `expires_at` datetime NOT NULL COMMENT '过期时间',
    `used` tinyint DEFAULT 0 COMMENT '是否已使用',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_oauth_state` (`state`),
    KEY `idx_oauth_state_expires` (`expires_at`),
    KEY `idx_oauth_state_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OAuth状态(CSRF和PKCE)表';


-- OAuth Token表
CREATE TABLE IF NOT EXISTS `s2_oauth_token` (
                                                `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    `provider_name` varchar(100) NOT NULL COMMENT 'OAuth提供者名称',
    `access_token` text NOT NULL COMMENT '访问令牌',
    `refresh_token` text DEFAULT NULL COMMENT '刷新令牌',
    `id_token` text DEFAULT NULL COMMENT 'ID令牌',
    `token_type` varchar(50) DEFAULT 'Bearer' COMMENT '令牌类型',
    `expires_at` datetime DEFAULT NULL COMMENT '访问令牌过期时间',
    `refresh_expires_at` datetime DEFAULT NULL COMMENT '刷新令牌过期时间',
    `scopes` varchar(500) DEFAULT NULL COMMENT '授权范围',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_oauth_token_user_provider` (`user_id`, `provider_name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户OAuth令牌表';


-- 刷新令牌表
CREATE TABLE IF NOT EXISTS `s2_refresh_token` (
                                                  `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `token_hash` varchar(128) NOT NULL COMMENT '令牌SHA-256哈希',
    `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    `session_id` bigint(20) DEFAULT NULL COMMENT '关联会话ID',
    `issued_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '签发时间',
    `expires_at` datetime NOT NULL COMMENT '过期时间',
    `revoked` tinyint DEFAULT 0 COMMENT '是否已撤销',
    `revoked_at` datetime DEFAULT NULL COMMENT '撤销时间',
    `device_info` varchar(500) DEFAULT NULL COMMENT '设备信息',
    `ip_address` varchar(45) DEFAULT NULL COMMENT 'IP地址',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
    KEY `idx_refresh_token_user` (`user_id`),
    KEY `idx_refresh_token_expires` (`expires_at`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='JWT刷新令牌表';


-- 用户会话表
CREATE TABLE IF NOT EXISTS `s2_user_session` (
                                                 `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `session_id` varchar(128) NOT NULL COMMENT '唯一会话标识',
    `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    `auth_method` varchar(50) NOT NULL COMMENT '认证方式: LOCAL, OAUTH',
    `provider_name` varchar(100) DEFAULT NULL COMMENT 'OAuth提供者(如适用)',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    `last_activity_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最后活动时间',
    `expires_at` datetime NOT NULL COMMENT '会话过期时间',
    `ip_address` varchar(45) DEFAULT NULL COMMENT 'IP地址',
    `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
    `revoked` tinyint DEFAULT 0 COMMENT '是否已撤销',
    `revoked_at` datetime DEFAULT NULL COMMENT '撤销时间',
    `revoked_reason` varchar(255) DEFAULT NULL COMMENT '撤销原因',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`),
    KEY `idx_session_user` (`user_id`),
    KEY `idx_session_expires` (`expires_at`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户会话跟踪表';


-- 用户令牌表
CREATE TABLE IF NOT EXISTS `s2_user_token` (
                                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL COMMENT '令牌名称',
    `user_name` varchar(255) NOT NULL COMMENT '用户名',
    `expire_time` bigint(20) NOT NULL COMMENT '过期时间(时间戳)',
    `token` text NOT NULL COMMENT '令牌值',
    `salt` varchar(255) DEFAULT NULL COMMENT '盐值',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time` datetime NOT NULL,
    `create_by` varchar(255) NOT NULL,
    `update_time` datetime DEFAULT NULL,
    `update_by` varchar(255) NOT NULL,
    `expire_date_time` datetime NOT NULL COMMENT '过期日期时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name_username_tenant` (`name`, `user_name`, `tenant_id`),
    KEY `idx_user_token_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户令牌信息表';


-- 组织架构表
CREATE TABLE IF NOT EXISTS `s2_organization` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `parent_id` bigint(20) DEFAULT 0 COMMENT '父组织ID，根组织为0',
    `name` varchar(100) NOT NULL COMMENT '组织名称',
    `full_name` varchar(500) DEFAULT NULL COMMENT '组织全名（包含父级路径）',
    `is_root` tinyint(1) DEFAULT 0 COMMENT '是否为根组织',
    `sort_order` int(11) DEFAULT 0 COMMENT '排序序号',
    `status` tinyint(1) DEFAULT 1 COMMENT '状态: 1=启用, 0=禁用',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_org_parent_id` (`parent_id`),
    KEY `idx_org_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织架构表';


-- 用户-组织关联表
CREATE TABLE IF NOT EXISTS `s2_user_organization` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    `organization_id` bigint(20) NOT NULL COMMENT '组织ID',
    `is_primary` tinyint(1) DEFAULT 0 COMMENT '是否为主组织',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_org` (`user_id`, `organization_id`),
    KEY `idx_user_org_user` (`user_id`),
    KEY `idx_user_org_org` (`organization_id`),
    KEY `idx_user_org_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-组织关联表';


-- 对话模型实例表
CREATE TABLE IF NOT EXISTS `s2_chat_model` (
                                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL COMMENT '名称',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `config` text NOT NULL COMMENT '配置信息',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `admin` varchar(500) DEFAULT NULL COMMENT '管理员',
    `viewer` varchar(500) DEFAULT NULL COMMENT '查看者',
    `is_open` tinyint DEFAULT NULL COMMENT '是否公开',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL COMMENT '创建人',
    `updated_at` datetime NOT NULL COMMENT '更新时间',
    `updated_by` varchar(100) NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_chat_model_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话大模型实例表';


-- 应用表
CREATE TABLE IF NOT EXISTS `s2_app` (
                                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(255) COMMENT '名称',
    `description` varchar(255) COMMENT '描述',
    `status` int COMMENT '状态',
    `config` text COMMENT '配置',
    `end_date` datetime COMMENT '结束日期',
    `qps` int COMMENT 'QPS限制',
    `app_secret` varchar(255) COMMENT '应用密钥',
    `owner` varchar(255) COMMENT '所有者',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime NULL,
    `updated_at` datetime NULL,
    `created_by` varchar(255) NULL,
    `updated_by` varchar(255) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_app_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用表';


-- 认证组表
CREATE TABLE IF NOT EXISTS `s2_auth_groups` (
                                                `group_id` bigint(20) NOT NULL,
    `config` varchar(2048) DEFAULT NULL COMMENT '配置',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`group_id`),
    KEY `idx_auth_groups_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认证组表';


-- 系统配置表
CREATE TABLE IF NOT EXISTS `s2_system_config` (
                                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `admin` varchar(500) COMMENT '系统管理员',
    `parameters` text NULL COMMENT '配置项',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_system_config_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
