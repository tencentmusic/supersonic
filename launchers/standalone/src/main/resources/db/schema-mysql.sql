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

-- ========================================
-- 4. 核心业务表 - 数据源与数据库
-- ========================================

-- 数据库实例表
CREATE TABLE IF NOT EXISTS `s2_database` (
                                             `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL COMMENT '名称',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `version` varchar(64) DEFAULT NULL COMMENT '版本',
    `type` varchar(20) NOT NULL COMMENT '类型: mysql, clickhouse, tdw等',
    `config` text NOT NULL COMMENT '配置信息(JSON)',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `admin` varchar(500) DEFAULT NULL COMMENT '管理员',
    `viewer` varchar(500) DEFAULT NULL COMMENT '查看者',
    `is_open` tinyint DEFAULT NULL COMMENT '是否公开',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL COMMENT '创建人',
    `updated_at` datetime NOT NULL COMMENT '更新时间',
    `updated_by` varchar(100) NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_database_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据库实例表';

-- ========================================
-- 5. 核心业务表 - 主题域与模型
-- ========================================

-- 主题域表
CREATE TABLE IF NOT EXISTS `s2_domain` (
                                           `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `name` varchar(255) DEFAULT NULL COMMENT '主题域名称',
    `biz_name` varchar(255) DEFAULT NULL COMMENT '业务名称',
    `parent_id` bigint(20) DEFAULT 0 COMMENT '父主题域ID',
    `status` tinyint NOT NULL COMMENT '主题域状态',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `admin` varchar(3000) DEFAULT NULL COMMENT '主题域管理员',
    `admin_org` varchar(3000) DEFAULT NULL COMMENT '主题域管理员组织',
    `is_open` tinyint DEFAULT NULL COMMENT '主题域是否公开',
    `viewer` varchar(3000) DEFAULT NULL COMMENT '主题域可用用户',
    `view_org` varchar(3000) DEFAULT NULL COMMENT '主题域可用组织',
    `entity` varchar(500) DEFAULT NULL COMMENT '主题域实体信息',
    `created_at` datetime DEFAULT NULL COMMENT '创建时间',
    `created_by` varchar(100) DEFAULT NULL COMMENT '创建人',
    `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
    `updated_by` varchar(100) DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_domain_tenant` (`tenant_id`),
    KEY `idx_domain_parent` (`parent_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主题域基础信息表';

-- 模型表
CREATE TABLE IF NOT EXISTS `s2_model` (
                                          `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模型名称',
    `biz_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务名称',
    `domain_id` bigint(20) DEFAULT NULL COMMENT '主题域ID',
    `alias` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '别名',
    `status` tinyint DEFAULT NULL COMMENT '状态',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `database_id` bigint(20) NOT NULL COMMENT '数据库ID',
    `model_detail` text NOT NULL COMMENT '模型详情(JSON)',
    `source_type` varchar(128) DEFAULT NULL COMMENT '来源类型',
    `depends` varchar(500) DEFAULT NULL COMMENT '依赖',
    `filter_sql` varchar(1000) DEFAULT NULL COMMENT '过滤SQL',
    `drill_down_dimensions` text DEFAULT NULL COMMENT '下钻维度',
    `tag_object_id` bigint(20) DEFAULT 0 COMMENT '标签对象ID',
    `ext` varchar(1000) DEFAULT NULL COMMENT '扩展信息',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `viewer` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '查看者',
    `view_org` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '查看组织',
    `admin` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '管理员',
    `admin_org` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '管理员组织',
    `is_open` tinyint DEFAULT NULL COMMENT '是否公开',
    `created_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `updated_at` datetime DEFAULT NULL,
    `entity` text COLLATE utf8mb4_unicode_ci COMMENT '实体信息',
    PRIMARY KEY (`id`),
    KEY `idx_model_tenant` (`tenant_id`),
    KEY `idx_model_domain` (`domain_id`),
    KEY `idx_model_database` (`database_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型表';

-- 模型关系表
CREATE TABLE IF NOT EXISTS `s2_model_rela` (
                                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `domain_id` bigint(20) COMMENT '主题域ID',
    `from_model_id` bigint(20) NOT NULL COMMENT '源模型ID',
    `to_model_id` bigint(20) NOT NULL COMMENT '目标模型ID',
    `join_type` varchar(255) COMMENT '关联类型',
    `join_condition` text COMMENT '关联条件',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_model_rela_domain` (`domain_id`),
    KEY `idx_model_rela_from` (`from_model_id`),
    KEY `idx_model_rela_to` (`to_model_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型关系表';

-- ========================================
-- 6. 核心业务表 - 维度与指标
-- ========================================

-- 维度表
CREATE TABLE IF NOT EXISTS `s2_dimension` (
                                              `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '维度ID',
    `model_id` bigint(20) NOT NULL COMMENT '模型ID',
    `name` varchar(255) NOT NULL COMMENT '维度名称',
    `biz_name` varchar(255) NOT NULL COMMENT '字段名称',
    `description` varchar(500) NOT NULL COMMENT '描述',
    `status` tinyint NOT NULL COMMENT '维度状态: 0=正常, 1=下架',
    `sensitive_level` int(10) DEFAULT NULL COMMENT '敏感级别',
    `type` varchar(50) NOT NULL COMMENT '维度类型: categorical, time',
    `type_params` text COMMENT '类型参数',
    `data_type` varchar(50) DEFAULT NULL COMMENT '维度数据类型: varchar, array',
    `expr` text NOT NULL COMMENT '表达式',
    `semantic_type` varchar(20) NOT NULL COMMENT '语义类型: DATE, ID, CATEGORY',
    `alias` varchar(500) DEFAULT NULL COMMENT '别名',
    `default_values` varchar(500) DEFAULT NULL COMMENT '默认值',
    `dim_value_maps` varchar(5000) DEFAULT NULL COMMENT '维度值映射',
    `is_tag` tinyint DEFAULT NULL COMMENT '是否标签',
    `ext` varchar(1000) DEFAULT NULL COMMENT '扩展信息',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL COMMENT '创建人',
    `updated_at` datetime NOT NULL COMMENT '更新时间',
    `updated_by` varchar(100) NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_dimension_model` (`model_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='维度表';

-- 指标表
CREATE TABLE IF NOT EXISTS `s2_metric` (
                                           `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `model_id` bigint(20) NOT NULL COMMENT '模型ID',
    `name` varchar(255) NOT NULL COMMENT '指标名称',
    `biz_name` varchar(255) NOT NULL COMMENT '字段名称',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `status` tinyint NOT NULL COMMENT '指标状态',
    `sensitive_level` tinyint NOT NULL COMMENT '敏感级别',
    `type` varchar(50) NOT NULL COMMENT '指标类型',
    `type_params` text NOT NULL COMMENT '类型参数',
    `data_format_type` varchar(50) DEFAULT NULL COMMENT '数值类型',
    `data_format` varchar(500) DEFAULT NULL COMMENT '数值类型参数',
    `alias` varchar(500) DEFAULT NULL COMMENT '别名',
    `classifications` varchar(500) DEFAULT NULL COMMENT '分类',
    `relate_dimensions` varchar(500) DEFAULT NULL COMMENT '指标相关维度',
    `ext` text DEFAULT NULL COMMENT '扩展信息',
    `define_type` varchar(50) DEFAULT NULL COMMENT '定义类型: MEASURE, FIELD, METRIC',
    `is_publish` tinyint DEFAULT NULL COMMENT '是否发布',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL COMMENT '创建人',
    `updated_at` datetime NOT NULL COMMENT '更新时间',
    `updated_by` varchar(100) NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_metric_model` (`model_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标表';

-- ========================================
-- 7. 核心业务表 - 数据集
-- ========================================

-- 数据集表
CREATE TABLE IF NOT EXISTS `s2_data_set` (
                                             `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `domain_id` bigint(20) COMMENT '主题域ID',
    `name` varchar(255) COMMENT '名称',
    `biz_name` varchar(255) COMMENT '业务名称',
    `description` varchar(255) COMMENT '描述',
    `status` int COMMENT '状态',
    `alias` varchar(255) COMMENT '别名',
    `data_set_detail` text COMMENT '数据集详情',
    `query_config` varchar(3000) COMMENT '查询配置',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `admin` varchar(3000) DEFAULT NULL COMMENT '管理员',
    `admin_org` varchar(3000) DEFAULT NULL COMMENT '管理员组织',
    `viewer` text DEFAULT NULL COMMENT '可查看用户',
    `view_org` text DEFAULT NULL COMMENT '可查看组织',
    `is_open` tinyint DEFAULT 0 COMMENT '是否公开',
    `created_at` datetime,
    `created_by` varchar(255),
    `updated_at` datetime,
    `updated_by` varchar(255),
    PRIMARY KEY (`id`),
    KEY `idx_data_set_tenant` (`tenant_id`),
    KEY `idx_data_set_domain` (`domain_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据集表';

-- 数据集权限组表
CREATE TABLE IF NOT EXISTS `s2_dataset_auth_groups` (
    `group_id` bigint(20) NOT NULL AUTO_INCREMENT,
    `dataset_id` bigint(20) NOT NULL COMMENT '数据集ID',
    `name` varchar(255) COMMENT '权限组名称',
    `auth_rules` text COMMENT '列权限JSON',
    `dimension_filters` text COMMENT '行权限JSON',
    `dimension_filter_description` varchar(500) COMMENT '行权限描述',
    `authorized_users` text COMMENT '授权用户列表',
    `authorized_department_ids` text COMMENT '授权部门ID列表',
    `inherit_from_model` tinyint DEFAULT 1 COMMENT '是否继承Model权限',
    `tenant_id` bigint(20) DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100),
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` varchar(100),
    PRIMARY KEY (`group_id`),
    KEY `idx_dataset_auth_dataset` (`dataset_id`),
    KEY `idx_dataset_auth_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据集权限组表';

-- ========================================
-- 8. 智能体与对话表
-- ========================================

-- 智能体表
CREATE TABLE IF NOT EXISTS `s2_agent` (
                                          `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '智能体名称',
    `description` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
    `examples` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '示例',
    `status` tinyint DEFAULT NULL COMMENT '状态',
    `model` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用模型',
    `tool_config` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工具配置',
    `llm_config` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'LLM配置',
    `chat_model_config` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '对话模型配置',
    `visual_config` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '可视化配置',
    `enable_search` tinyint DEFAULT 1 COMMENT '是否启用搜索',
    `enable_feedback` tinyint DEFAULT 1 COMMENT '是否启用反馈',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `admin` varchar(1000) DEFAULT NULL COMMENT '管理员',
    `admin_org` varchar(1000) DEFAULT NULL COMMENT '管理员组织',
    `is_open` tinyint DEFAULT NULL COMMENT '是否公开',
    `viewer` varchar(1000) DEFAULT NULL COMMENT '可用用户',
    `view_org` varchar(1000) DEFAULT NULL COMMENT '可用组织',
    `created_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `updated_at` datetime DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_agent_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体表';

-- 对话表
CREATE TABLE IF NOT EXISTS `s2_chat` (
                                         `chat_id` bigint(20) NOT NULL AUTO_INCREMENT,
    `agent_id` bigint(20) DEFAULT NULL COMMENT '智能体ID',
    `chat_name` varchar(300) DEFAULT NULL COMMENT '对话名称',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `last_time` datetime DEFAULT NULL COMMENT '最后更新时间',
    `creator` varchar(30) DEFAULT NULL COMMENT '创建者',
    `last_question` varchar(200) DEFAULT NULL COMMENT '最后问题',
    `is_delete` tinyint DEFAULT 0 COMMENT '是否删除',
    `is_top` tinyint DEFAULT 0 COMMENT '是否置顶',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`chat_id`),
    KEY `idx_chat_tenant` (`tenant_id`),
    KEY `idx_chat_agent` (`agent_id`),
    KEY `idx_chat_creator` (`creator`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话表';

-- 对话配置表
CREATE TABLE IF NOT EXISTS `s2_chat_config` (
                                                `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `model_id` bigint(20) DEFAULT NULL COMMENT '模型ID',
    `chat_detail_config` mediumtext COMMENT '明细模式配置信息',
    `chat_agg_config` mediumtext COMMENT '指标模式配置信息',
    `recommended_questions` mediumtext COMMENT '推荐问题配置',
    `llm_examples` text COMMENT 'LLM示例',
    `status` tinyint NOT NULL COMMENT '状态: 0=删除, 1=生效',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `updated_at` datetime NOT NULL COMMENT '更新时间',
    `created_by` varchar(100) NOT NULL COMMENT '创建人',
    `updated_by` varchar(100) NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_chat_config_model` (`model_id`),
    KEY `idx_chat_config_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话配置表';

-- 对话记忆表
CREATE TABLE IF NOT EXISTS `s2_chat_memory` (
                                                `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `question` varchar(655) COMMENT '用户问题',
    `side_info` text COMMENT '辅助信息',
    `query_id` bigint(20) COMMENT '问答ID',
    `agent_id` bigint(20) COMMENT '智能体ID',
    `db_schema` text COMMENT 'Schema映射',
    `s2_sql` text COMMENT '大模型解析SQL',
    `status` varchar(10) COMMENT '状态',
    `llm_review` varchar(10) COMMENT '大模型评估结果',
    `llm_comment` text COMMENT '大模型评估意见',
    `human_review` varchar(10) COMMENT '管理员评估结果',
    `human_comment` text COMMENT '管理员评估意见',
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_chat_memory_agent` (`agent_id`),
    KEY `idx_chat_memory_query` (`query_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话记忆表';

-- 对话上下文表
CREATE TABLE IF NOT EXISTS `s2_chat_context` (
                                                 `chat_id` bigint(20) NOT NULL COMMENT '对话ID',
    `modified_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `query_user` varchar(64) DEFAULT NULL COMMENT '查询用户',
    `query_text` text COMMENT '查询文本',
    `semantic_parse` text COMMENT '语义解析数据',
    `ext_data` text COMMENT '扩展数据',
    PRIMARY KEY (`chat_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话上下文表';

-- 对话解析表
CREATE TABLE IF NOT EXISTS `s2_chat_parse` (
                                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `question_id` bigint(20) NOT NULL COMMENT '问题ID',
    `chat_id` bigint(20) NOT NULL COMMENT '对话ID',
    `parse_id` bigint(20) NOT NULL COMMENT '解析ID',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `query_text` varchar(500) DEFAULT NULL COMMENT '查询文本',
    `user_name` varchar(150) DEFAULT NULL COMMENT '用户名',
    `parse_info` mediumtext NOT NULL COMMENT '解析信息',
    `is_candidate` int(11) DEFAULT 1 COMMENT '1=候选, 0=已选',
    PRIMARY KEY (`id`),
    KEY `idx_chat_parse_question` (`question_id`),
    KEY `idx_chat_parse_chat` (`chat_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话解析表';

-- 对话查询表
CREATE TABLE IF NOT EXISTS `s2_chat_query` (
                                               `question_id` bigint(20) NOT NULL AUTO_INCREMENT,
    `agent_id` bigint(20) DEFAULT NULL COMMENT '智能体ID',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `query_text` mediumtext COMMENT '查询文本',
    `user_name` varchar(150) DEFAULT NULL COMMENT '用户名',
    `query_state` int(1) DEFAULT NULL COMMENT '查询状态',
    `chat_id` bigint(20) NOT NULL COMMENT '对话ID',
    `query_result` mediumtext COMMENT '查询结果',
    `score` int(11) DEFAULT 0 COMMENT '评分',
    `feedback` varchar(1024) DEFAULT '' COMMENT '反馈',
    `similar_queries` varchar(1024) DEFAULT '' COMMENT '相似查询',
    `parse_time_cost` varchar(1024) DEFAULT '' COMMENT '解析耗时',
    PRIMARY KEY (`question_id`),
    KEY `idx_chat_query_agent` (`agent_id`),
    KEY `idx_chat_query_chat` (`chat_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话查询表';

-- 对话统计表
CREATE TABLE IF NOT EXISTS `s2_chat_statistics` (
                                                    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `question_id` bigint(20) NOT NULL COMMENT '问题ID',
    `chat_id` bigint(20) NOT NULL COMMENT '对话ID',
    `user_name` varchar(150) DEFAULT NULL COMMENT '用户名',
    `query_text` varchar(200) DEFAULT NULL COMMENT '查询文本',
    `interface_name` varchar(100) DEFAULT NULL COMMENT '接口名称',
    `cost` int(6) DEFAULT 0 COMMENT '耗时(ms)',
    `type` int(11) DEFAULT NULL COMMENT '类型',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_chat_statistics_question` (`question_id`),
    KEY `idx_chat_statistics_chat` (`chat_id`),
    KEY `idx_chat_statistics_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话统计表';

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

-- ========================================
-- 9. 标签与分类表
-- ========================================

-- 标签对象表
CREATE TABLE IF NOT EXISTS `s2_tag_object` (
                                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `domain_id` bigint(20) DEFAULT NULL COMMENT '主题域ID',
    `name` varchar(255) NOT NULL COMMENT '名称',
    `biz_name` varchar(255) NOT NULL COMMENT '英文名称',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
    `sensitive_level` tinyint NOT NULL DEFAULT 0 COMMENT '敏感级别',
    `ext` text DEFAULT NULL COMMENT '扩展信息',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL COMMENT '创建人',
    `updated_at` datetime NULL COMMENT '更新时间',
    `updated_by` varchar(100) NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_tag_object_domain` (`domain_id`),
    KEY `idx_tag_object_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签对象表';

-- 标签表
CREATE TABLE IF NOT EXISTS `s2_tag` (
                                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `item_id` bigint(20) NOT NULL COMMENT '关联项ID',
    `type` varchar(255) NOT NULL COMMENT '类型',
    `ext` text DEFAULT NULL COMMENT '扩展信息',
    `created_at` datetime NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_tag_item` (`item_id`, `type`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- ========================================
-- 10. 术语与规则表
-- ========================================

-- 术语表
CREATE TABLE IF NOT EXISTS `s2_term` (
                                         `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `domain_id` bigint(20) COMMENT '主题域ID',
    `name` varchar(255) NOT NULL COMMENT '术语名称',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `alias` varchar(1000) NOT NULL COMMENT '别名',
    `related_metrics` varchar(1000) DEFAULT NULL COMMENT '关联指标',
    `related_dimensions` varchar(1000) DEFAULT NULL COMMENT '关联维度',
    `created_at` datetime NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_term_domain` (`domain_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='术语表';

-- 查询规则表
CREATE TABLE IF NOT EXISTS `s2_query_rule` (
                                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `data_set_id` bigint(20) COMMENT '数据集ID',
    `priority` int(10) NOT NULL DEFAULT 1 COMMENT '优先级',
    `rule_type` varchar(255) NOT NULL COMMENT '规则类型',
    `name` varchar(255) NOT NULL COMMENT '名称',
    `biz_name` varchar(255) NOT NULL COMMENT '业务名称',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `rule` text DEFAULT NULL COMMENT '规则内容',
    `action` text DEFAULT NULL COMMENT '动作',
    `status` int NOT NULL DEFAULT 1 COMMENT '状态',
    `ext` text DEFAULT NULL COMMENT '扩展信息',
    `created_at` datetime NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_query_rule_data_set` (`data_set_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='查询规则表';

-- ========================================
-- 11. 字典与配置表
-- ========================================

-- 字典配置表
CREATE TABLE IF NOT EXISTS `s2_dictionary_conf` (
                                                    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `description` varchar(255) COMMENT '描述',
    `type` varchar(255) NOT NULL COMMENT '类型',
    `item_id` bigint(20) NOT NULL COMMENT '关联项ID',
    `config` mediumtext COMMENT '配置',
    `status` varchar(255) NOT NULL COMMENT '状态',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_dictionary_conf_item` (`item_id`, `type`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典配置信息表';

-- 字典任务表
CREATE TABLE IF NOT EXISTS `s2_dictionary_task` (
                                                    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL COMMENT '名称',
    `description` varchar(255) COMMENT '描述',
    `type` varchar(255) NOT NULL COMMENT '类型',
    `item_id` bigint(20) NOT NULL COMMENT '关联项ID',
    `config` mediumtext COMMENT '配置',
    `status` varchar(255) NOT NULL COMMENT '状态',
    `elapsed_ms` int(10) DEFAULT NULL COMMENT '耗时(ms)',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_dictionary_task_item` (`item_id`, `type`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典运行任务表';

-- 可用日期信息表
CREATE TABLE IF NOT EXISTS `s2_available_date_info` (
                                                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `item_id` bigint(20) NOT NULL COMMENT '关联项ID',
    `type` varchar(255) NOT NULL COMMENT '类型',
    `date_format` varchar(64) NOT NULL COMMENT '日期格式',
    `date_period` varchar(64) DEFAULT NULL COMMENT '日期周期',
    `start_date` varchar(64) DEFAULT NULL COMMENT '开始日期',
    `end_date` varchar(64) DEFAULT NULL COMMENT '结束日期',
    `unavailable_date` text COMMENT '不可用日期',
    `status` tinyint DEFAULT 0 COMMENT '状态',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` varchar(100) NOT NULL,
    `updated_at` timestamp NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_item_type` (`item_id`, `type`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='可用日期信息表';

-- ========================================
-- 12. 插件与应用表
-- ========================================

-- 插件表
CREATE TABLE IF NOT EXISTS `s2_plugin` (
                                           `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `type` varchar(50) DEFAULT NULL COMMENT '类型: DASHBOARD, WIDGET, URL',
    `data_set` varchar(100) DEFAULT NULL COMMENT '数据集',
    `pattern` varchar(500) DEFAULT NULL COMMENT '模式',
    `parse_mode` varchar(100) DEFAULT NULL COMMENT '解析模式',
    `parse_mode_config` text COMMENT '解析模式配置',
    `name` varchar(100) DEFAULT NULL COMMENT '名称',
    `config` text COMMENT '配置',
    `comment` text COMMENT '备注',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT NULL,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_plugin_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='插件表';

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

-- ========================================
-- 13. 统计与分析表
-- ========================================

-- 查询统计信息表
CREATE TABLE IF NOT EXISTS `s2_query_stat_info` (
                                                    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `trace_id` varchar(200) DEFAULT NULL COMMENT '查询标识',
    `model_id` bigint(20) DEFAULT NULL COMMENT '模型ID',
    `data_set_id` bigint(20) DEFAULT NULL COMMENT '数据集ID',
    `query_user` varchar(200) DEFAULT NULL COMMENT '执行SQL的用户',
    `query_type` varchar(200) DEFAULT NULL COMMENT '查询对应的场景',
    `query_type_back` int(10) DEFAULT 0 COMMENT '查询类型: 0=正常查询, 1=预刷类型',
    `query_sql_cmd` mediumtext COMMENT '对应查询的SQL命令',
    `sql_cmd_md5` varchar(200) DEFAULT NULL COMMENT 'SQL命令MD5值',
    `query_struct_cmd` mediumtext COMMENT '对应查询的struct',
    `struct_cmd_md5` varchar(200) DEFAULT NULL COMMENT 'struct MD5值',
    `query_sql` mediumtext COMMENT '对应查询的SQL',
    `sql_md5` varchar(200) DEFAULT NULL COMMENT 'SQL MD5值',
    `query_engine` varchar(20) DEFAULT NULL COMMENT '查询引擎',
    `elapsed_ms` bigint(10) DEFAULT NULL COMMENT '查询耗时(ms)',
    `query_state` varchar(20) DEFAULT NULL COMMENT '查询最终状态',
    `native_query` int(10) DEFAULT NULL COMMENT '1=明细查询, 0=聚合查询',
    `start_date` varchar(50) DEFAULT NULL COMMENT 'SQL开始日期',
    `end_date` varchar(50) DEFAULT NULL COMMENT 'SQL结束日期',
    `dimensions` mediumtext COMMENT 'SQL涉及的维度',
    `metrics` mediumtext COMMENT 'SQL涉及的指标',
    `select_cols` mediumtext COMMENT 'SQL select部分涉及的标签',
    `agg_cols` mediumtext COMMENT 'SQL agg部分涉及的标签',
    `filter_cols` mediumtext COMMENT 'SQL where部分涉及的标签',
    `group_by_cols` mediumtext COMMENT 'SQL group by部分涉及的标签',
    `order_by_cols` mediumtext COMMENT 'SQL order by部分涉及的标签',
    `use_result_cache` tinyint(1) DEFAULT -1 COMMENT '是否命中结果缓存',
    `use_sql_cache` tinyint(1) DEFAULT -1 COMMENT '是否命中SQL缓存',
    `sql_cache_key` mediumtext COMMENT 'SQL缓存key',
    `result_cache_key` mediumtext COMMENT '结果缓存key',
    `query_opt_mode` varchar(20) DEFAULT NULL COMMENT '优化模式',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_query_stat_model` (`model_id`),
    KEY `idx_query_stat_data_set` (`data_set_id`),
    KEY `idx_query_stat_tenant` (`tenant_id`),
    KEY `idx_query_stat_user` (`query_user`),
    KEY `idx_query_stat_created` (`created_at`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='查询统计信息表';

-- ========================================
-- 14. 其他辅助表
-- ========================================

-- 画布表
CREATE TABLE IF NOT EXISTS `s2_canvas` (
                                           `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `domain_id` bigint(20) DEFAULT NULL COMMENT '主题域ID',
    `type` varchar(20) DEFAULT NULL COMMENT '类型: datasource, dimension, metric',
    `config` text COMMENT '配置详情',
    `created_at` datetime DEFAULT NULL,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_canvas_domain` (`domain_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='画布表';

-- 收藏表
CREATE TABLE IF NOT EXISTS `s2_collect` (
                                            `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `type` varchar(20) NOT NULL COMMENT '类型',
    `username` varchar(20) NOT NULL COMMENT '用户名',
    `collect_id` bigint(20) NOT NULL COMMENT '收藏对象ID',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time` datetime,
    `update_time` datetime,
    PRIMARY KEY (`id`),
    KEY `idx_collect_user` (`username`),
    KEY `idx_collect_tenant` (`tenant_id`),
    KEY `idx_collect_type_id` (`type`, `collect_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- 指标查询默认配置表
CREATE TABLE IF NOT EXISTS `s2_metric_query_default_config` (
                                                                `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `metric_id` bigint(20) COMMENT '指标ID',
    `user_name` varchar(255) NOT NULL COMMENT '用户名',
    `default_config` varchar(1000) NOT NULL COMMENT '默认配置',
    `created_at` datetime NULL,
    `updated_at` datetime NULL,
    `created_by` varchar(100) NULL,
    `updated_by` varchar(100) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_metric_query_config_metric` (`metric_id`),
    KEY `idx_metric_query_config_user` (`user_name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标查询默认配置表';

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
-- ========================================
-- 15. 语义模板管理表
-- ========================================

-- 语义模板定义表
CREATE TABLE IF NOT EXISTS `s2_semantic_template` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(100) NOT NULL COMMENT '模板名称',
    `biz_name` varchar(100) NOT NULL COMMENT '模板代码',
    `description` varchar(500) DEFAULT NULL COMMENT '模板描述',
    `category` varchar(50) NOT NULL COMMENT '模板类别: VISITS/SINGER/COMPANY/ECOMMERCE',
    `template_config` longtext NOT NULL COMMENT 'JSON: 模板配置',
    `preview_image` varchar(500) DEFAULT NULL COMMENT '预览图URL',
    `status` tinyint DEFAULT 0 COMMENT '状态: 0-草稿 1-已部署',
    `is_builtin` tinyint DEFAULT 0 COMMENT '是否内置模板: 0-租户自定义 1-系统内置',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID: 1表示系统级(内置模板)',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_semantic_template_tenant_biz` (`tenant_id`, `biz_name`),
    KEY `idx_semantic_template_tenant` (`tenant_id`),
    KEY `idx_semantic_template_builtin` (`is_builtin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语义模板定义表';

-- 语义模板部署记录表
CREATE TABLE IF NOT EXISTS `s2_semantic_deployment` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `template_id` bigint(20) NOT NULL COMMENT '模板ID',
    `template_name` varchar(100) DEFAULT NULL COMMENT '模板名称快照',
    `database_id` bigint(20) DEFAULT NULL COMMENT '目标数据库ID',
    `param_config` text DEFAULT NULL COMMENT 'JSON: 用户自定义参数',
    `status` varchar(20) NOT NULL COMMENT '状态: PENDING/RUNNING/SUCCESS/FAILED',
    `result_detail` longtext DEFAULT NULL COMMENT 'JSON: 创建的对象详情',
    `error_message` text DEFAULT NULL COMMENT '错误信息',
    `current_step` varchar(50) DEFAULT NULL COMMENT '当前执行步骤',
    `start_time` datetime DEFAULT NULL COMMENT '开始时间',
    `end_time` datetime DEFAULT NULL COMMENT '结束时间',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `active_lock` varchar(100) DEFAULT NULL COMMENT '部署并发锁: PENDING/RUNNING时为templateId_tenantId, 否则为NULL',
    PRIMARY KEY (`id`),
    KEY `idx_semantic_deployment_template` (`template_id`),
    KEY `idx_semantic_deployment_tenant` (`tenant_id`),
    KEY `idx_semantic_deployment_status` (`status`),
    UNIQUE KEY `uk_deployment_active_lock` (`active_lock`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语义模板部署记录表';
