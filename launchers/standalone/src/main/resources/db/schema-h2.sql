-- ========================================
-- SuperSonic Multi-tenant SaaS DDL Schema (H2)
-- Version: 2.0
-- Description: 支持多租户SaaS及权限管理功能 (H2内存数据库)
-- ========================================

-- ========================================
-- 1. 基础设施表 - 租户与订阅管理
-- ========================================

-- 订阅计划表
CREATE TABLE IF NOT EXISTS `s2_subscription_plan` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `code` VARCHAR(50) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `price_monthly` DECIMAL(10, 2) DEFAULT 0,
    `price_yearly` DECIMAL(10, 2) DEFAULT 0,
    `max_users` INT DEFAULT -1,
    `max_datasets` INT DEFAULT -1,
    `max_models` INT DEFAULT -1,
    `max_agents` INT DEFAULT -1,
    `max_api_calls_per_day` INT DEFAULT -1,
    `max_tokens_per_month` BIGINT DEFAULT -1,
    `features` TEXT DEFAULT NULL,
    `is_default` TINYINT DEFAULT 0,
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_subscription_plan_code` UNIQUE (`code`)
);
COMMENT ON TABLE s2_subscription_plan IS '订阅计划定义表';

-- 租户表
CREATE TABLE IF NOT EXISTS `s2_tenant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `code` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `contact_email` VARCHAR(255) DEFAULT NULL,
    `contact_name` VARCHAR(100) DEFAULT NULL,
    `contact_phone` VARCHAR(50) DEFAULT NULL,
    `logo_url` VARCHAR(500) DEFAULT NULL,
    `settings` TEXT DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100) DEFAULT NULL,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_by` VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_tenant_code` UNIQUE (`code`)
);
COMMENT ON TABLE s2_tenant IS '租户主表';

-- 租户订阅记录表
CREATE TABLE IF NOT EXISTS `s2_tenant_subscription` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `plan_id` BIGINT NOT NULL,
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `start_date` TIMESTAMP NOT NULL,
    `end_date` TIMESTAMP DEFAULT NULL,
    `billing_cycle` VARCHAR(20) DEFAULT 'MONTHLY',
    `auto_renew` TINYINT DEFAULT 1,
    `payment_method` VARCHAR(50) DEFAULT NULL,
    `payment_reference` VARCHAR(255) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_tenant_subscription IS '租户订阅记录表';

-- 租户使用量统计表
CREATE TABLE IF NOT EXISTS `s2_tenant_usage` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `usage_date` DATE NOT NULL,
    `api_calls` INT DEFAULT 0,
    `tokens_used` BIGINT DEFAULT 0,
    `query_count` INT DEFAULT 0,
    `storage_bytes` BIGINT DEFAULT 0,
    `active_users` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_tenant_usage_date` UNIQUE (`tenant_id`, `usage_date`)
);
COMMENT ON TABLE s2_tenant_usage IS '租户使用量统计表';

-- 租户邀请表
CREATE TABLE IF NOT EXISTS `s2_tenant_invitation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `role_id` BIGINT DEFAULT NULL,
    `token` VARCHAR(255) NOT NULL,
    `status` VARCHAR(20) DEFAULT 'PENDING',
    `expires_at` TIMESTAMP NOT NULL,
    `invited_by` VARCHAR(100) DEFAULT NULL,
    `accepted_at` TIMESTAMP DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_tenant_invitation_token` UNIQUE (`token`)
);
COMMENT ON TABLE s2_tenant_invitation IS '租户用户邀请表';

-- ========================================
-- 2. 用户与权限管理表 (RBAC)
-- ========================================

-- 用户表
CREATE TABLE IF NOT EXISTS `s2_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `display_name` VARCHAR(100) DEFAULT NULL,
    `password` VARCHAR(256) DEFAULT NULL,
    `salt` VARCHAR(256) DEFAULT NULL,
    `email` VARCHAR(100) DEFAULT NULL,
    `phone` VARCHAR(50) DEFAULT NULL,
    `avatar_url` VARCHAR(500) DEFAULT NULL,
    `is_admin` TINYINT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `last_login` TIMESTAMP DEFAULT NULL,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100) DEFAULT NULL,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_by` VARCHAR(100) DEFAULT NULL,
    `employee_id` VARCHAR(64) DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_user_name_tenant` UNIQUE (`name`, `tenant_id`)
);
COMMENT ON TABLE s2_user IS '用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `s2_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `code` VARCHAR(50) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `scope` VARCHAR(20) DEFAULT 'TENANT',
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `is_system` TINYINT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100) DEFAULT NULL,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_by` VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_role_code_tenant` UNIQUE (`code`, `tenant_id`)
);
COMMENT ON TABLE s2_role IS '角色定义表';
COMMENT ON COLUMN s2_role.scope IS '作用域: PLATFORM=平台级, TENANT=租户级';

-- 权限表
CREATE TABLE IF NOT EXISTS `s2_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `code` VARCHAR(100) NOT NULL,
    `type` VARCHAR(50) NOT NULL DEFAULT 'MENU',
    `scope` VARCHAR(20) DEFAULT 'TENANT',
    `parent_id` BIGINT DEFAULT NULL,
    `path` VARCHAR(255) DEFAULT NULL,
    `icon` VARCHAR(100) DEFAULT NULL,
    `sort_order` INT DEFAULT 0,
    `description` VARCHAR(500) DEFAULT NULL,
    `status` TINYINT DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100) DEFAULT NULL,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_permission_code` UNIQUE (`code`)
);
COMMENT ON TABLE s2_permission IS '权限定义表';
COMMENT ON COLUMN s2_permission.scope IS '作用域: PLATFORM=平台级, TENANT=租户级';
COMMENT ON COLUMN s2_permission.type IS '权限类型: MENU=菜单, BUTTON=按钮, DATA=数据, API=接口';

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS `s2_role_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `role_id` BIGINT NOT NULL,
    `permission_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_role_permission` UNIQUE (`role_id`, `permission_id`)
);
COMMENT ON TABLE s2_role_permission IS '角色-权限关联表';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS `s2_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_user_role` UNIQUE (`user_id`, `role_id`)
);
COMMENT ON TABLE s2_user_role IS '用户-角色关联表';

-- 资源权限表
CREATE TABLE IF NOT EXISTS `s2_resource_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `resource_type` VARCHAR(50) NOT NULL,
    `resource_id` BIGINT NOT NULL,
    `principal_type` VARCHAR(20) NOT NULL,
    `principal_id` BIGINT NOT NULL,
    `permission_type` VARCHAR(20) NOT NULL,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_resource_principal_perm` UNIQUE (`resource_type`, `resource_id`, `principal_type`, `principal_id`, `permission_type`)
);
COMMENT ON TABLE s2_resource_permission IS '资源级权限控制表';

-- ========================================
-- 3. OAuth与会话管理表
-- ========================================

-- OAuth提供者配置表
CREATE TABLE IF NOT EXISTS `s2_oauth_provider` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `client_id` VARCHAR(255) NOT NULL,
    `client_secret` VARCHAR(512) DEFAULT NULL,
    `authorization_uri` VARCHAR(500) DEFAULT NULL,
    `token_uri` VARCHAR(500) DEFAULT NULL,
    `user_info_uri` VARCHAR(500) DEFAULT NULL,
    `jwks_uri` VARCHAR(500) DEFAULT NULL,
    `issuer` VARCHAR(500) DEFAULT NULL,
    `scopes` VARCHAR(500) DEFAULT NULL,
    `pkce_enabled` TINYINT DEFAULT 1,
    `additional_params` TEXT DEFAULT NULL,
    `enabled` TINYINT DEFAULT 1,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_oauth_provider_name_tenant` UNIQUE (`name`, `tenant_id`)
);
COMMENT ON TABLE s2_oauth_provider IS 'OAuth提供者配置表';

-- OAuth状态表
CREATE TABLE IF NOT EXISTS `s2_oauth_state` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `state` VARCHAR(128) NOT NULL,
    `provider_name` VARCHAR(100) NOT NULL,
    `code_verifier` VARCHAR(128) DEFAULT NULL,
    `redirect_uri` VARCHAR(500) DEFAULT NULL,
    `nonce` VARCHAR(128) DEFAULT NULL,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `expires_at` TIMESTAMP NOT NULL,
    `used` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_oauth_state` UNIQUE (`state`)
);
COMMENT ON TABLE s2_oauth_state IS 'OAuth状态(CSRF和PKCE)表';

-- OAuth Token表
CREATE TABLE IF NOT EXISTS `s2_oauth_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `provider_name` VARCHAR(100) NOT NULL,
    `access_token` TEXT NOT NULL,
    `refresh_token` TEXT DEFAULT NULL,
    `id_token` TEXT DEFAULT NULL,
    `token_type` VARCHAR(50) DEFAULT 'Bearer',
    `expires_at` TIMESTAMP DEFAULT NULL,
    `refresh_expires_at` TIMESTAMP DEFAULT NULL,
    `scopes` VARCHAR(500) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_oauth_token IS '用户OAuth令牌表';

-- 刷新令牌表
CREATE TABLE IF NOT EXISTS `s2_refresh_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `token_hash` VARCHAR(128) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `session_id` BIGINT DEFAULT NULL,
    `issued_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `expires_at` TIMESTAMP NOT NULL,
    `revoked` TINYINT DEFAULT 0,
    `revoked_at` TIMESTAMP DEFAULT NULL,
    `device_info` VARCHAR(500) DEFAULT NULL,
    `ip_address` VARCHAR(45) DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_refresh_token_hash` UNIQUE (`token_hash`)
);
COMMENT ON TABLE s2_refresh_token IS 'JWT刷新令牌表';

-- 用户会话表
CREATE TABLE IF NOT EXISTS `s2_user_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` VARCHAR(128) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `auth_method` VARCHAR(50) NOT NULL,
    `provider_name` VARCHAR(100) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `last_activity_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `expires_at` TIMESTAMP NOT NULL,
    `ip_address` VARCHAR(45) DEFAULT NULL,
    `user_agent` VARCHAR(500) DEFAULT NULL,
    `revoked` TINYINT DEFAULT 0,
    `revoked_at` TIMESTAMP DEFAULT NULL,
    `revoked_reason` VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_session_id` UNIQUE (`session_id`)
);
COMMENT ON TABLE s2_user_session IS '用户会话跟踪表';

-- 用户令牌表
CREATE TABLE IF NOT EXISTS `s2_user_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `user_name` VARCHAR(255) NOT NULL,
    `expire_time` BIGINT NOT NULL,
    `token` TEXT NOT NULL,
    `salt` VARCHAR(255) DEFAULT NULL,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `create_time` TIMESTAMP NOT NULL,
    `create_by` VARCHAR(255) NOT NULL,
    `update_time` TIMESTAMP DEFAULT NULL,
    `update_by` VARCHAR(255) NOT NULL,
    `expire_date_time` TIMESTAMP NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_name_username_tenant` UNIQUE (`name`, `user_name`, `tenant_id`)
);
COMMENT ON TABLE s2_user_token IS '用户令牌信息表';

-- 组织架构表
CREATE TABLE IF NOT EXISTS `s2_organization` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `parent_id` BIGINT DEFAULT 0,
    `name` VARCHAR(100) NOT NULL,
    `full_name` VARCHAR(500) DEFAULT NULL,
    `is_root` TINYINT DEFAULT 0,
    `sort_order` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100) DEFAULT NULL,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_by` VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_org_parent_id` (`parent_id`),
    INDEX `idx_org_tenant_id` (`tenant_id`)
);
COMMENT ON TABLE s2_organization IS '组织架构表';

-- 用户-组织关联表
CREATE TABLE IF NOT EXISTS `s2_user_organization` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `organization_id` BIGINT NOT NULL,
    `is_primary` TINYINT DEFAULT 0,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_user_org` UNIQUE (`user_id`, `organization_id`),
    INDEX `idx_user_org_user` (`user_id`),
    INDEX `idx_user_org_org` (`organization_id`),
    INDEX `idx_user_org_tenant` (`tenant_id`)
);
COMMENT ON TABLE s2_user_organization IS '用户-组织关联表';
COMMENT ON COLUMN s2_user_organization.tenant_id IS '租户ID';

-- ========================================
-- 4. 核心业务表 - 数据源与数据库
-- ========================================

-- 数据库实例表
CREATE TABLE IF NOT EXISTS `s2_database` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `version` VARCHAR(64) DEFAULT NULL,
    `type` VARCHAR(20) NOT NULL,
    `config` TEXT NOT NULL,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `admin` VARCHAR(500) DEFAULT NULL,
    `viewer` VARCHAR(500) DEFAULT NULL,
    `is_open` TINYINT DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL,
    `created_by` VARCHAR(100) NOT NULL,
    `updated_at` TIMESTAMP NOT NULL,
    `updated_by` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_database IS '数据库实例表';

-- ========================================
-- 5. 核心业务表 - 主题域与模型
-- ========================================

-- 主题域表
CREATE TABLE IF NOT EXISTS `s2_domain` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) DEFAULT NULL,
    `biz_name` VARCHAR(255) DEFAULT NULL,
    `parent_id` BIGINT DEFAULT 0,
    `status` TINYINT NOT NULL,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `admin` VARCHAR(3000) DEFAULT NULL,
    `admin_org` VARCHAR(3000) DEFAULT NULL,
    `is_open` TINYINT DEFAULT NULL,
    `viewer` VARCHAR(3000) DEFAULT NULL,
    `view_org` VARCHAR(3000) DEFAULT NULL,
    `entity` VARCHAR(500) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT NULL,
    `created_by` VARCHAR(100) DEFAULT NULL,
    `updated_at` TIMESTAMP DEFAULT NULL,
    `updated_by` VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_domain IS '主题域基础信息表';

-- 模型表
CREATE TABLE IF NOT EXISTS `s2_model` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) DEFAULT NULL,
    `biz_name` VARCHAR(100) DEFAULT NULL,
    `domain_id` BIGINT DEFAULT NULL,
    `alias` VARCHAR(200) DEFAULT NULL,
    `status` TINYINT DEFAULT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `database_id` BIGINT NOT NULL,
    `model_detail` TEXT NOT NULL,
    `source_type` VARCHAR(128) DEFAULT NULL,
    `depends` VARCHAR(500) DEFAULT NULL,
    `filter_sql` VARCHAR(1000) DEFAULT NULL,
    `drill_down_dimensions` TEXT DEFAULT NULL,
    `tag_object_id` BIGINT DEFAULT 0,
    `ext` VARCHAR(1000) DEFAULT NULL,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `viewer` VARCHAR(500) DEFAULT NULL,
    `view_org` VARCHAR(500) DEFAULT NULL,
    `admin` VARCHAR(500) DEFAULT NULL,
    `admin_org` VARCHAR(500) DEFAULT NULL,
    `is_open` TINYINT DEFAULT NULL,
    `created_by` VARCHAR(100) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT NULL,
    `updated_by` VARCHAR(100) DEFAULT NULL,
    `updated_at` TIMESTAMP DEFAULT NULL,
    `entity` TEXT,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_model IS '模型表';

-- 模型关系表
CREATE TABLE IF NOT EXISTS `s2_model_rela` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `domain_id` BIGINT,
    `from_model_id` BIGINT NOT NULL,
    `to_model_id` BIGINT NOT NULL,
    `join_type` VARCHAR(255),
    `join_condition` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_model_rela IS '模型关系表';

-- ========================================
-- 6. 核心业务表 - 维度与指标
-- ========================================

-- 维度表
CREATE TABLE IF NOT EXISTS `s2_dimension` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `model_id` BIGINT NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `biz_name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(500) NOT NULL,
    `status` TINYINT NOT NULL,
    `sensitive_level` INT DEFAULT NULL,
    `type` VARCHAR(50) NOT NULL,
    `type_params` TEXT,
    `data_type` VARCHAR(50) DEFAULT NULL,
    `expr` TEXT NOT NULL,
    `semantic_type` VARCHAR(20) NOT NULL,
    `alias` VARCHAR(500) DEFAULT NULL,
    `default_values` VARCHAR(500) DEFAULT NULL,
    `dim_value_maps` VARCHAR(5000) DEFAULT NULL,
    `is_tag` TINYINT DEFAULT NULL,
    `ext` VARCHAR(1000) DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL,
    `created_by` VARCHAR(100) NOT NULL,
    `updated_at` TIMESTAMP NOT NULL,
    `updated_by` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_dimension IS '维度表';

-- 指标表
CREATE TABLE IF NOT EXISTS `s2_metric` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `model_id` BIGINT NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `biz_name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `status` TINYINT NOT NULL,
    `sensitive_level` TINYINT NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `type_params` TEXT NOT NULL,
    `data_format_type` VARCHAR(50) DEFAULT NULL,
    `data_format` VARCHAR(500) DEFAULT NULL,
    `alias` VARCHAR(500) DEFAULT NULL,
    `classifications` VARCHAR(500) DEFAULT NULL,
    `relate_dimensions` VARCHAR(500) DEFAULT NULL,
    `ext` TEXT DEFAULT NULL,
    `define_type` VARCHAR(50) DEFAULT NULL,
    `is_publish` TINYINT DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL,
    `created_by` VARCHAR(100) NOT NULL,
    `updated_at` TIMESTAMP NOT NULL,
    `updated_by` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_metric IS '指标表';

-- ========================================
-- 7. 核心业务表 - 数据集
-- ========================================

-- 数据集表
CREATE TABLE IF NOT EXISTS `s2_data_set` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `domain_id` BIGINT,
    `name` VARCHAR(255),
    `biz_name` VARCHAR(255),
    `description` VARCHAR(255),
    `status` INT,
    `alias` VARCHAR(255),
    `data_set_detail` TEXT,
    `query_config` VARCHAR(3000),
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `admin` VARCHAR(3000) DEFAULT NULL,
    `admin_org` VARCHAR(3000) DEFAULT NULL,
    `viewer` VARCHAR(3000) DEFAULT NULL,
    `view_org` VARCHAR(3000) DEFAULT NULL,
    `is_open` TINYINT DEFAULT 0,
    `created_at` TIMESTAMP,
    `created_by` VARCHAR(255),
    `updated_at` TIMESTAMP,
    `updated_by` VARCHAR(255),
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_data_set IS '数据集表';

-- 数据集权限组表
CREATE TABLE IF NOT EXISTS `s2_dataset_auth_groups` (
    `group_id` BIGINT NOT NULL AUTO_INCREMENT,
    `dataset_id` BIGINT NOT NULL,
    `name` VARCHAR(255),
    `auth_rules` TEXT,
    `dimension_filters` TEXT,
    `dimension_filter_description` VARCHAR(500),
    `authorized_users` TEXT,
    `authorized_department_ids` TEXT,
    `inherit_from_model` TINYINT DEFAULT 1,
    `tenant_id` BIGINT DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100),
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_by` VARCHAR(100),
    PRIMARY KEY (`group_id`)
);
COMMENT ON TABLE s2_dataset_auth_groups IS '数据集权限组表';
CREATE INDEX idx_dataset_auth_dataset ON s2_dataset_auth_groups (`dataset_id`);
CREATE INDEX idx_dataset_auth_tenant ON s2_dataset_auth_groups (`tenant_id`);

-- ========================================
-- 8. 智能体与对话表
-- ========================================

-- 智能体表
CREATE TABLE IF NOT EXISTS `s2_agent` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) DEFAULT NULL,
    `description` TEXT DEFAULT NULL,
    `examples` TEXT DEFAULT NULL,
    `status` TINYINT DEFAULT NULL,
    `model` VARCHAR(100) DEFAULT NULL,
    `tool_config` TEXT DEFAULT NULL,
    `llm_config` TEXT DEFAULT NULL,
    `chat_model_config` TEXT DEFAULT NULL,
    `visual_config` TEXT DEFAULT NULL,
    `enable_search` TINYINT DEFAULT 1,
    `enable_feedback` TINYINT DEFAULT 1,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `admin` VARCHAR(1000) DEFAULT NULL,
    `admin_org` VARCHAR(1000) DEFAULT NULL,
    `is_open` TINYINT DEFAULT NULL,
    `viewer` VARCHAR(1000) DEFAULT NULL,
    `view_org` VARCHAR(1000) DEFAULT NULL,
    `created_by` VARCHAR(100) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT NULL,
    `updated_by` VARCHAR(100) DEFAULT NULL,
    `updated_at` TIMESTAMP DEFAULT NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_agent IS '智能体表';

-- 对话表
CREATE TABLE IF NOT EXISTS `s2_chat` (
    `chat_id` BIGINT NOT NULL AUTO_INCREMENT,
    `agent_id` BIGINT DEFAULT NULL,
    `chat_name` VARCHAR(300) DEFAULT NULL,
    `create_time` TIMESTAMP DEFAULT NULL,
    `last_time` TIMESTAMP DEFAULT NULL,
    `creator` VARCHAR(30) DEFAULT NULL,
    `last_question` VARCHAR(200) DEFAULT NULL,
    `is_delete` TINYINT DEFAULT 0,
    `is_top` TINYINT DEFAULT 0,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (`chat_id`)
);
COMMENT ON TABLE s2_chat IS '对话表';

-- 对话配置表
CREATE TABLE IF NOT EXISTS `s2_chat_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `model_id` BIGINT DEFAULT NULL,
    `chat_detail_config` TEXT,
    `chat_agg_config` TEXT,
    `recommended_questions` TEXT,
    `llm_examples` TEXT,
    `status` TINYINT NOT NULL,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP NOT NULL,
    `updated_at` TIMESTAMP NOT NULL,
    `created_by` VARCHAR(100) NOT NULL,
    `updated_by` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_chat_config IS '对话配置表';

-- 对话记忆表
CREATE TABLE IF NOT EXISTS `s2_chat_memory` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `question` VARCHAR(655),
    `side_info` TEXT,
    `query_id` BIGINT,
    `agent_id` BIGINT,
    `db_schema` TEXT,
    `s2_sql` TEXT,
    `status` VARCHAR(10),
    `llm_review` VARCHAR(10),
    `llm_comment` TEXT,
    `human_review` VARCHAR(10),
    `human_comment` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100) DEFAULT NULL,
    `updated_by` VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_chat_memory IS '对话记忆表';

-- 对话上下文表
CREATE TABLE IF NOT EXISTS `s2_chat_context` (
    `chat_id` BIGINT NOT NULL,
    `modified_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `query_user` VARCHAR(64) DEFAULT NULL,
    `query_text` TEXT,
    `semantic_parse` TEXT,
    `ext_data` TEXT,
    PRIMARY KEY (`chat_id`)
);
COMMENT ON TABLE s2_chat_context IS '对话上下文表';

-- 对话解析表
CREATE TABLE IF NOT EXISTS `s2_chat_parse` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `question_id` BIGINT NOT NULL,
    `chat_id` BIGINT NOT NULL,
    `parse_id` BIGINT NOT NULL,
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `query_text` VARCHAR(500) DEFAULT NULL,
    `user_name` VARCHAR(150) DEFAULT NULL,
    `parse_info` TEXT NOT NULL,
    `is_candidate` INT DEFAULT 1,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_chat_parse IS '对话解析表';

-- 对话查询表
CREATE TABLE IF NOT EXISTS `s2_chat_query` (
    `question_id` BIGINT NOT NULL AUTO_INCREMENT,
    `agent_id` BIGINT DEFAULT NULL,
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `query_text` TEXT,
    `user_name` VARCHAR(150) DEFAULT NULL,
    `query_state` INT DEFAULT NULL,
    `chat_id` BIGINT NOT NULL,
    `query_result` TEXT,
    `score` INT DEFAULT 0,
    `feedback` VARCHAR(1024) DEFAULT '',
    `similar_queries` VARCHAR(1024) DEFAULT '',
    `parse_time_cost` VARCHAR(1024) DEFAULT '',
    PRIMARY KEY (`question_id`)
);
COMMENT ON TABLE s2_chat_query IS '对话查询表';

-- 对话统计表
CREATE TABLE IF NOT EXISTS `s2_chat_statistics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `question_id` BIGINT NOT NULL,
    `chat_id` BIGINT NOT NULL,
    `user_name` VARCHAR(150) DEFAULT NULL,
    `query_text` VARCHAR(200) DEFAULT NULL,
    `interface_name` VARCHAR(100) DEFAULT NULL,
    `cost` INT DEFAULT 0,
    `type` INT DEFAULT NULL,
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_chat_statistics IS '对话统计表';

-- 对话模型实例表
CREATE TABLE IF NOT EXISTS `s2_chat_model` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `config` TEXT NOT NULL,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `admin` VARCHAR(500) DEFAULT NULL,
    `viewer` VARCHAR(500) DEFAULT NULL,
    `is_open` TINYINT DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL,
    `created_by` VARCHAR(100) NOT NULL,
    `updated_at` TIMESTAMP NOT NULL,
    `updated_by` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_chat_model IS '对话大模型实例表';

-- ========================================
-- 9. 标签与分类表
-- ========================================

-- 标签对象表
CREATE TABLE IF NOT EXISTS `s2_tag_object` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `domain_id` BIGINT DEFAULT NULL,
    `name` VARCHAR(255) NOT NULL,
    `biz_name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `sensitive_level` TINYINT NOT NULL DEFAULT 0,
    `ext` TEXT DEFAULT NULL,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP NOT NULL,
    `created_by` VARCHAR(100) NOT NULL,
    `updated_at` TIMESTAMP NULL,
    `updated_by` VARCHAR(100) NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_tag_object IS '标签对象表';

-- 标签表
CREATE TABLE IF NOT EXISTS `s2_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `item_id` BIGINT NOT NULL,
    `type` VARCHAR(255) NOT NULL,
    `ext` TEXT DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL,
    `created_by` VARCHAR(100) NOT NULL,
    `updated_at` TIMESTAMP DEFAULT NULL,
    `updated_by` VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_tag IS '标签表';

-- ========================================
-- 10. 术语与规则表
-- ========================================

-- 术语表
CREATE TABLE IF NOT EXISTS `s2_term` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `domain_id` BIGINT,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `alias` VARCHAR(1000) NOT NULL,
    `related_metrics` VARCHAR(1000) DEFAULT NULL,
    `related_dimensions` VARCHAR(1000) DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL,
    `created_by` VARCHAR(100) NOT NULL,
    `updated_at` TIMESTAMP DEFAULT NULL,
    `updated_by` VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_term IS '术语表';

-- 查询规则表
CREATE TABLE IF NOT EXISTS `s2_query_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `data_set_id` BIGINT,
    `priority` INT NOT NULL DEFAULT 1,
    `rule_type` VARCHAR(255) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `biz_name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `rule` TEXT DEFAULT NULL,
    `action` TEXT DEFAULT NULL,
    `status` INT NOT NULL DEFAULT 1,
    `ext` TEXT DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL,
    `created_by` VARCHAR(100) NOT NULL,
    `updated_at` TIMESTAMP DEFAULT NULL,
    `updated_by` VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_query_rule IS '查询规则表';

-- ========================================
-- 11. 字典与配置表
-- ========================================

-- 字典配置表
CREATE TABLE IF NOT EXISTS `s2_dictionary_conf` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `description` VARCHAR(255),
    `type` VARCHAR(255) NOT NULL,
    `item_id` BIGINT NOT NULL,
    `config` TEXT,
    `status` VARCHAR(255) NOT NULL,
    `created_at` TIMESTAMP NOT NULL,
    `created_by` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_dictionary_conf IS '字典配置信息表';

-- 字典任务表
CREATE TABLE IF NOT EXISTS `s2_dictionary_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(255),
    `type` VARCHAR(255) NOT NULL,
    `item_id` BIGINT NOT NULL,
    `config` TEXT,
    `status` VARCHAR(255) NOT NULL,
    `elapsed_ms` INT DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_dictionary_task IS '字典运行任务表';

-- 可用日期信息表
CREATE TABLE IF NOT EXISTS `s2_available_date_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `item_id` BIGINT NOT NULL,
    `type` VARCHAR(255) NOT NULL,
    `date_format` VARCHAR(64) NOT NULL,
    `date_period` VARCHAR(64) DEFAULT NULL,
    `start_date` VARCHAR(64) DEFAULT NULL,
    `end_date` VARCHAR(64) DEFAULT NULL,
    `unavailable_date` TEXT,
    `status` TINYINT DEFAULT 0,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100) NOT NULL,
    `updated_at` TIMESTAMP NULL,
    `updated_by` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_item_type` UNIQUE (`item_id`, `type`)
);
COMMENT ON TABLE s2_available_date_info IS '可用日期信息表';

-- ========================================
-- 12. 插件与应用表
-- ========================================

-- 插件表
CREATE TABLE IF NOT EXISTS `s2_plugin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `type` VARCHAR(50) DEFAULT NULL,
    `data_set` VARCHAR(100) DEFAULT NULL,
    `pattern` VARCHAR(500) DEFAULT NULL,
    `parse_mode` VARCHAR(100) DEFAULT NULL,
    `parse_mode_config` TEXT,
    `name` VARCHAR(100) DEFAULT NULL,
    `config` TEXT,
    `comment` TEXT,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT NULL,
    `created_by` VARCHAR(100) DEFAULT NULL,
    `updated_at` TIMESTAMP DEFAULT NULL,
    `updated_by` VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_plugin IS '插件表';

-- 应用表
CREATE TABLE IF NOT EXISTS `s2_app` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255),
    `description` VARCHAR(255),
    `status` INT,
    `config` TEXT,
    `end_date` TIMESTAMP,
    `qps` INT,
    `app_secret` VARCHAR(255),
    `owner` VARCHAR(255),
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP NULL,
    `updated_at` TIMESTAMP NULL,
    `created_by` VARCHAR(255) NULL,
    `updated_by` VARCHAR(255) NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_app IS '应用表';

-- ========================================
-- 13. 统计与分析表
-- ========================================

-- 查询统计信息表
CREATE TABLE IF NOT EXISTS `s2_query_stat_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `trace_id` VARCHAR(200) DEFAULT NULL,
    `model_id` BIGINT DEFAULT NULL,
    `data_set_id` BIGINT DEFAULT NULL,
    `query_user` VARCHAR(200) DEFAULT NULL,
    `query_type` VARCHAR(200) DEFAULT NULL,
    `query_type_back` INT DEFAULT 0,
    `query_sql_cmd` TEXT,
    `sql_cmd_md5` VARCHAR(200) DEFAULT NULL,
    `query_struct_cmd` TEXT,
    `struct_cmd_md5` VARCHAR(200) DEFAULT NULL,
    `query_sql` TEXT,
    `sql_md5` VARCHAR(200) DEFAULT NULL,
    `query_engine` VARCHAR(20) DEFAULT NULL,
    `elapsed_ms` BIGINT DEFAULT NULL,
    `query_state` VARCHAR(20) DEFAULT NULL,
    `native_query` INT DEFAULT NULL,
    `start_date` VARCHAR(50) DEFAULT NULL,
    `end_date` VARCHAR(50) DEFAULT NULL,
    `dimensions` TEXT,
    `metrics` TEXT,
    `select_cols` TEXT,
    `agg_cols` TEXT,
    `filter_cols` TEXT,
    `group_by_cols` TEXT,
    `order_by_cols` TEXT,
    `use_result_cache` TINYINT DEFAULT -1,
    `use_sql_cache` TINYINT DEFAULT -1,
    `sql_cache_key` TEXT,
    `result_cache_key` TEXT,
    `query_opt_mode` VARCHAR(20) DEFAULT NULL,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_query_stat_info IS '查询统计信息表';

-- ========================================
-- 14. 其他辅助表
-- ========================================

-- 画布表
CREATE TABLE IF NOT EXISTS `s2_canvas` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `domain_id` BIGINT DEFAULT NULL,
    `type` VARCHAR(20) DEFAULT NULL,
    `config` TEXT,
    `created_at` TIMESTAMP DEFAULT NULL,
    `created_by` VARCHAR(100) DEFAULT NULL,
    `updated_at` TIMESTAMP DEFAULT NULL,
    `updated_by` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_canvas IS '画布表';

-- 收藏表
CREATE TABLE IF NOT EXISTS `s2_collect` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `type` VARCHAR(20) NOT NULL,
    `username` VARCHAR(20) NOT NULL,
    `collect_id` BIGINT NOT NULL,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `create_time` TIMESTAMP,
    `update_time` TIMESTAMP,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_collect IS '收藏表';

-- 指标查询默认配置表
CREATE TABLE IF NOT EXISTS `s2_metric_query_default_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `metric_id` BIGINT,
    `user_name` VARCHAR(255) NOT NULL,
    `default_config` VARCHAR(1000) NOT NULL,
    `created_at` TIMESTAMP NULL,
    `updated_at` TIMESTAMP NULL,
    `created_by` VARCHAR(100) NULL,
    `updated_by` VARCHAR(100) NULL,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_metric_query_default_config IS '指标查询默认配置表';

-- 认证组表
CREATE TABLE IF NOT EXISTS `s2_auth_groups` (
    `group_id` BIGINT NOT NULL,
    `config` VARCHAR(2048) DEFAULT NULL,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (`group_id`)
);
COMMENT ON TABLE s2_auth_groups IS '认证组表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `s2_system_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `admin` VARCHAR(500),
    `parameters` TEXT NULL,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_system_config IS '系统配置表';

-- ========================================
-- 15. 语义模板管理表
-- ========================================

-- 语义模板定义表
CREATE TABLE IF NOT EXISTS `s2_semantic_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `biz_name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `category` VARCHAR(50) NOT NULL,
    `template_config` CLOB NOT NULL,
    `preview_image` VARCHAR(500) DEFAULT NULL,
    `status` TINYINT DEFAULT 0,
    `is_builtin` TINYINT DEFAULT 0,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100) DEFAULT NULL,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_by` VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_semantic_template_tenant_biz` UNIQUE (`tenant_id`, `biz_name`)
);
COMMENT ON TABLE s2_semantic_template IS '语义模板定义表';

-- 语义模板部署记录表
CREATE TABLE IF NOT EXISTS `s2_semantic_deployment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `template_id` BIGINT NOT NULL,
    `template_name` VARCHAR(100) DEFAULT NULL,
    `database_id` BIGINT DEFAULT NULL,
    `param_config` TEXT DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL,
    `result_detail` CLOB DEFAULT NULL,
    `error_message` TEXT DEFAULT NULL,
    `current_step` VARCHAR(50) DEFAULT NULL,
    `start_time` TIMESTAMP DEFAULT NULL,
    `end_time` TIMESTAMP DEFAULT NULL,
    `tenant_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100) DEFAULT NULL,
    `active_lock` VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT uk_deployment_active_lock UNIQUE (`active_lock`)
);
COMMENT ON TABLE s2_semantic_deployment IS '语义模板部署记录表';
CREATE INDEX idx_semantic_deployment_template ON s2_semantic_deployment(`template_id`);
CREATE INDEX idx_semantic_deployment_tenant ON s2_semantic_deployment(`tenant_id`);
CREATE INDEX idx_semantic_deployment_status ON s2_semantic_deployment(`status`);

-- ========================================
-- 飞书机器人
-- ========================================

-- 飞书用户映射表
CREATE TABLE IF NOT EXISTS `s2_feishu_user_mapping` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `feishu_open_id` VARCHAR(64) NOT NULL,
    `feishu_union_id` VARCHAR(64) DEFAULT NULL,
    `feishu_user_name` VARCHAR(128) DEFAULT NULL,
    `feishu_email` VARCHAR(128) DEFAULT NULL,
    `feishu_mobile` VARCHAR(20) DEFAULT NULL,
    `feishu_employee_id` VARCHAR(64) DEFAULT NULL,
    `s2_user_id` BIGINT DEFAULT NULL,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `default_agent_id` INT DEFAULT NULL,
    `match_type` VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    `status` TINYINT DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_feishu_open_id` UNIQUE (`feishu_open_id`)
);
CREATE INDEX idx_s2_feishu_user_mapping_s2_user_id ON s2_feishu_user_mapping(`s2_user_id`);

-- 飞书查询会话表
CREATE TABLE IF NOT EXISTS `s2_feishu_query_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `feishu_open_id` VARCHAR(64) NOT NULL,
    `feishu_message_id` VARCHAR(64) NOT NULL,
    `query_text` CLOB NOT NULL,
    `query_result_id` BIGINT DEFAULT NULL,
    `sql_text` CLOB DEFAULT NULL,
    `row_count` INT DEFAULT NULL,
    `dataset_id` BIGINT DEFAULT NULL,
    `agent_id` INT DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `error_message` CLOB DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);
CREATE INDEX idx_feishu_session_open_id_created ON s2_feishu_query_session(`feishu_open_id`, `created_at` DESC);
