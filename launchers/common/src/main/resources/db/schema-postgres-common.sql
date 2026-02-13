-- Common (auth + billing + infrastructure) schema (PostgreSQL)

-- ========================================
-- SuperSonic Multi-tenant SaaS DDL Schema (PostgreSQL)
-- Version: 2.0
-- Description: 支持多租户SaaS及权限管理功能
-- ========================================

-- ========================================
-- 1. 基础设施表 - 租户与订阅管理
-- ========================================

-- 订阅计划表
CREATE TABLE IF NOT EXISTS s2_subscription_plan (
    id BIGSERIAL NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    price_monthly DECIMAL(10, 2) DEFAULT 0,
    price_yearly DECIMAL(10, 2) DEFAULT 0,
    max_users INTEGER DEFAULT -1,
    max_datasets INTEGER DEFAULT -1,
    max_models INTEGER DEFAULT -1,
    max_agents INTEGER DEFAULT -1,
    max_api_calls_per_day INTEGER DEFAULT -1,
    max_tokens_per_month BIGINT DEFAULT -1,
    features TEXT DEFAULT NULL,
    is_default SMALLINT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_subscription_plan_code UNIQUE (code)
);

COMMENT ON TABLE s2_subscription_plan IS '订阅计划定义表';

COMMENT ON COLUMN s2_subscription_plan.name IS '计划名称';

COMMENT ON COLUMN s2_subscription_plan.code IS '计划编码';

COMMENT ON COLUMN s2_subscription_plan.max_users IS '最大用户数 (-1=无限制)';


-- 租户表
CREATE TABLE IF NOT EXISTS s2_tenant (
    id BIGSERIAL NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    contact_email VARCHAR(255) DEFAULT NULL,
    contact_name VARCHAR(100) DEFAULT NULL,
    contact_phone VARCHAR(50) DEFAULT NULL,
    logo_url VARCHAR(500) DEFAULT NULL,
    settings TEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tenant_code UNIQUE (code)
);

COMMENT ON TABLE s2_tenant IS '租户主表';


-- 租户订阅记录表
CREATE TABLE IF NOT EXISTS s2_tenant_subscription (
    id BIGSERIAL NOT NULL,
    tenant_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP DEFAULT NULL,
    billing_cycle VARCHAR(20) DEFAULT 'MONTHLY',
    auto_renew SMALLINT DEFAULT 1,
    payment_method VARCHAR(50) DEFAULT NULL,
    payment_reference VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_tenant_subscription IS '租户订阅记录表';


-- 租户使用量统计表
CREATE TABLE IF NOT EXISTS s2_tenant_usage (
    id BIGSERIAL NOT NULL,
    tenant_id BIGINT NOT NULL,
    usage_date DATE NOT NULL,
    api_calls INTEGER DEFAULT 0,
    tokens_used BIGINT DEFAULT 0,
    query_count INTEGER DEFAULT 0,
    storage_bytes BIGINT DEFAULT 0,
    active_users INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_tenant_usage_date UNIQUE (tenant_id, usage_date)
);

COMMENT ON TABLE s2_tenant_usage IS '租户使用量统计表';


-- 租户邀请表
CREATE TABLE IF NOT EXISTS s2_tenant_invitation (
    id BIGSERIAL NOT NULL,
    tenant_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    role_id BIGINT DEFAULT NULL,
    token VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    invited_by VARCHAR(100) DEFAULT NULL,
    accepted_at TIMESTAMP DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_tenant_invitation_token UNIQUE (token)
);

COMMENT ON TABLE s2_tenant_invitation IS '租户用户邀请表';


-- ========================================
-- 2. 用户与权限管理表 (RBAC)
-- ========================================

-- 用户表
CREATE TABLE IF NOT EXISTS s2_user (
    id BIGSERIAL NOT NULL,
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) DEFAULT NULL,
    password VARCHAR(256) DEFAULT NULL,
    salt VARCHAR(256) DEFAULT NULL,
    email VARCHAR(100) DEFAULT NULL,
    phone VARCHAR(50) DEFAULT NULL,
    avatar_url VARCHAR(500) DEFAULT NULL,
    is_admin SMALLINT DEFAULT 0,
    status SMALLINT DEFAULT 1,
    last_login TIMESTAMP DEFAULT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) DEFAULT NULL,
    employee_id VARCHAR(64) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_name_tenant UNIQUE (name, tenant_id)
);

COMMENT ON TABLE s2_user IS '用户表';


-- 角色表
CREATE TABLE IF NOT EXISTS s2_role (
    id BIGSERIAL NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    scope VARCHAR(20) DEFAULT 'TENANT',
    tenant_id BIGINT NOT NULL DEFAULT 1,
    is_system SMALLINT DEFAULT 0,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_code_tenant UNIQUE (code, tenant_id)
);

COMMENT ON TABLE s2_role IS '角色定义表';

COMMENT ON COLUMN s2_role.scope IS '作用域: PLATFORM=平台级, TENANT=租户级';


-- 权限表
CREATE TABLE IF NOT EXISTS s2_permission (
    id BIGSERIAL NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'MENU',
    scope VARCHAR(20) DEFAULT 'TENANT',
    parent_id BIGINT DEFAULT NULL,
    path VARCHAR(255) DEFAULT NULL,
    icon VARCHAR(100) DEFAULT NULL,
    sort_order INTEGER DEFAULT 0,
    description VARCHAR(500) DEFAULT NULL,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_permission_code UNIQUE (code)
);

COMMENT ON TABLE s2_permission IS '权限定义表';

COMMENT ON COLUMN s2_permission.scope IS '作用域: PLATFORM=平台级, TENANT=租户级';

COMMENT ON COLUMN s2_permission.type IS '权限类型: MENU=菜单, BUTTON=按钮, DATA=数据, API=接口';


-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS s2_role_permission (
    id BIGSERIAL NOT NULL,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

COMMENT ON TABLE s2_role_permission IS '角色-权限关联表';


-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS s2_user_role (
    id BIGSERIAL NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

COMMENT ON TABLE s2_user_role IS '用户-角色关联表';


-- 资源权限表
CREATE TABLE IF NOT EXISTS s2_resource_permission (
    id BIGSERIAL NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id BIGINT NOT NULL,
    principal_type VARCHAR(20) NOT NULL,
    principal_id BIGINT NOT NULL,
    permission_type VARCHAR(20) NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_resource_principal_perm UNIQUE (resource_type, resource_id, principal_type, principal_id, permission_type)
);

COMMENT ON TABLE s2_resource_permission IS '资源级权限控制表';


-- ========================================
-- 3. OAuth与会话管理表
-- ========================================

-- OAuth提供者配置表
CREATE TABLE IF NOT EXISTS s2_oauth_provider (
    id BIGSERIAL NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    client_secret VARCHAR(512) DEFAULT NULL,
    authorization_uri VARCHAR(500) DEFAULT NULL,
    token_uri VARCHAR(500) DEFAULT NULL,
    user_info_uri VARCHAR(500) DEFAULT NULL,
    jwks_uri VARCHAR(500) DEFAULT NULL,
    issuer VARCHAR(500) DEFAULT NULL,
    scopes VARCHAR(500) DEFAULT NULL,
    pkce_enabled SMALLINT DEFAULT 1,
    additional_params TEXT DEFAULT NULL,
    enabled SMALLINT DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_oauth_provider_name_tenant UNIQUE (name, tenant_id)
);

COMMENT ON TABLE s2_oauth_provider IS 'OAuth提供者配置表';


-- OAuth状态表
CREATE TABLE IF NOT EXISTS s2_oauth_state (
    id BIGSERIAL NOT NULL,
    state VARCHAR(128) NOT NULL,
    provider_name VARCHAR(100) NOT NULL,
    code_verifier VARCHAR(128) DEFAULT NULL,
    redirect_uri VARCHAR(500) DEFAULT NULL,
    nonce VARCHAR(128) DEFAULT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used SMALLINT DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_oauth_state UNIQUE (state)
);

COMMENT ON TABLE s2_oauth_state IS 'OAuth状态(CSRF和PKCE)表';


-- OAuth Token表
CREATE TABLE IF NOT EXISTS s2_oauth_token (
    id BIGSERIAL NOT NULL,
    user_id BIGINT NOT NULL,
    provider_name VARCHAR(100) NOT NULL,
    access_token TEXT NOT NULL,
    refresh_token TEXT DEFAULT NULL,
    id_token TEXT DEFAULT NULL,
    token_type VARCHAR(50) DEFAULT 'Bearer',
    expires_at TIMESTAMP DEFAULT NULL,
    refresh_expires_at TIMESTAMP DEFAULT NULL,
    scopes VARCHAR(500) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_oauth_token IS '用户OAuth令牌表';


-- 刷新令牌表
CREATE TABLE IF NOT EXISTS s2_refresh_token (
    id BIGSERIAL NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    session_id BIGINT DEFAULT NULL,
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked SMALLINT DEFAULT 0,
    revoked_at TIMESTAMP DEFAULT NULL,
    device_info VARCHAR(500) DEFAULT NULL,
    ip_address VARCHAR(45) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash)
);

COMMENT ON TABLE s2_refresh_token IS 'JWT刷新令牌表';


-- 用户会话表
CREATE TABLE IF NOT EXISTS s2_user_session (
    id BIGSERIAL NOT NULL,
    session_id VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    auth_method VARCHAR(50) NOT NULL,
    provider_name VARCHAR(100) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    ip_address VARCHAR(45) DEFAULT NULL,
    user_agent VARCHAR(500) DEFAULT NULL,
    revoked SMALLINT DEFAULT 0,
    revoked_at TIMESTAMP DEFAULT NULL,
    revoked_reason VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_session_id UNIQUE (session_id)
);

COMMENT ON TABLE s2_user_session IS '用户会话跟踪表';


-- 用户令牌表
CREATE TABLE IF NOT EXISTS s2_user_token (
    id BIGSERIAL NOT NULL,
    name VARCHAR(255) NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    expire_time BIGINT NOT NULL,
    token TEXT NOT NULL,
    salt VARCHAR(255) DEFAULT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL,
    create_by VARCHAR(255) NOT NULL,
    update_time TIMESTAMP DEFAULT NULL,
    update_by VARCHAR(255) NOT NULL,
    expire_date_time TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_name_username_tenant UNIQUE (name, user_name, tenant_id)
);

COMMENT ON TABLE s2_user_token IS '用户令牌信息表';


-- 组织架构表
CREATE TABLE IF NOT EXISTS s2_organization (
    id BIGSERIAL NOT NULL,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    full_name VARCHAR(500) DEFAULT NULL,
    is_root SMALLINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    status SMALLINT DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_organization IS '组织架构表';


-- 用户-组织关联表
CREATE TABLE IF NOT EXISTS s2_user_organization (
    id BIGSERIAL NOT NULL,
    user_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    is_primary SMALLINT DEFAULT 0,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_org UNIQUE (user_id, organization_id)
);

COMMENT ON TABLE s2_user_organization IS '用户-组织关联表';

COMMENT ON COLUMN s2_user_organization.tenant_id IS '租户ID';


-- 对话模型实例表
CREATE TABLE IF NOT EXISTS s2_chat_model (
    id BIGSERIAL NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    config TEXT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    admin VARCHAR(500) DEFAULT NULL,
    viewer VARCHAR(500) DEFAULT NULL,
    is_open SMALLINT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_chat_model IS '对话大模型实例表';


-- 应用表
CREATE TABLE IF NOT EXISTS s2_app (
    id BIGSERIAL NOT NULL,
    name VARCHAR(255),
    description VARCHAR(255),
    status INTEGER,
    config TEXT,
    end_date TIMESTAMP,
    qps INTEGER,
    app_secret VARCHAR(255),
    owner VARCHAR(255),
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_app IS '应用表';


-- 认证组表
CREATE TABLE IF NOT EXISTS s2_auth_groups (
    group_id BIGINT NOT NULL,
    config VARCHAR(2048) DEFAULT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (group_id)
);

COMMENT ON TABLE s2_auth_groups IS '认证组表';


-- 系统配置表
CREATE TABLE IF NOT EXISTS s2_system_config (
    id BIGSERIAL NOT NULL,
    admin VARCHAR(500),
    parameters TEXT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_system_config IS '系统配置表';
