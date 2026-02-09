-- ========================================
-- 多租户支持迁移脚本 (PostgreSQL)
-- 版本: V1
-- 说明: 添加租户与订阅管理支持
-- ========================================

-- ========================================
-- 1: 创建租户相关表
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

-- 租户表 (quota columns removed - plan quotas managed via s2_tenant_subscription + s2_subscription_plan)
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
COMMENT ON COLUMN s2_tenant.name IS '租户名称';
COMMENT ON COLUMN s2_tenant.code IS '租户编码';

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
COMMENT ON COLUMN s2_tenant_subscription.billing_cycle IS '计费周期: MONTHLY/YEARLY';
CREATE INDEX IF NOT EXISTS idx_tenant_subscription_tenant ON s2_tenant_subscription(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_subscription_plan ON s2_tenant_subscription(plan_id);

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
CREATE INDEX IF NOT EXISTS idx_tenant_usage_tenant ON s2_tenant_usage(tenant_id);

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
COMMENT ON COLUMN s2_tenant_invitation.role_id IS '邀请的角色ID';
CREATE INDEX IF NOT EXISTS idx_tenant_invitation_email ON s2_tenant_invitation(email);
CREATE INDEX IF NOT EXISTS idx_tenant_invitation_tenant ON s2_tenant_invitation(tenant_id);

-- ========================================
-- 2: 用户表完整定义
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
    PRIMARY KEY (id),
    CONSTRAINT uk_user_name_tenant UNIQUE (name, tenant_id)
);
COMMENT ON TABLE s2_user IS '用户表';
COMMENT ON COLUMN s2_user.name IS '用户名';
COMMENT ON COLUMN s2_user.display_name IS '显示名称';
COMMENT ON COLUMN s2_user.password IS '密码哈希';
COMMENT ON COLUMN s2_user.salt IS '密码盐';
COMMENT ON COLUMN s2_user.is_admin IS '是否管理员';
COMMENT ON COLUMN s2_user.status IS '用户状态：0=禁用，1=启用';
COMMENT ON COLUMN s2_user.tenant_id IS '租户ID';
CREATE INDEX IF NOT EXISTS idx_user_tenant ON s2_user(tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_email ON s2_user(email);

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
COMMENT ON COLUMN s2_user_token.name IS '令牌名称';
COMMENT ON COLUMN s2_user_token.user_name IS '用户名';
COMMENT ON COLUMN s2_user_token.expire_time IS '过期时间戳';
COMMENT ON COLUMN s2_user_token.token IS '令牌值';
COMMENT ON COLUMN s2_user_token.expire_date_time IS '过期日期时间';
CREATE INDEX IF NOT EXISTS idx_user_token_tenant ON s2_user_token(tenant_id);
