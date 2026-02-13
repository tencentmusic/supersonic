-- ========================================
-- OAuth与会话管理迁移脚本 (PostgreSQL)
-- 版本: V3
-- 说明: 添加OAuth认证和会话管理支持
-- ========================================

-- ========================================
-- 1: OAuth提供者配置表
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
COMMENT ON COLUMN s2_oauth_provider.name IS '提供者名称';
COMMENT ON COLUMN s2_oauth_provider.type IS '提供者类型: GOOGLE, AZURE_AD, KEYCLOAK, GENERIC_OIDC';
COMMENT ON COLUMN s2_oauth_provider.client_id IS 'OAuth客户端ID';
COMMENT ON COLUMN s2_oauth_provider.client_secret IS 'OAuth客户端密钥';
COMMENT ON COLUMN s2_oauth_provider.authorization_uri IS '授权端点';
COMMENT ON COLUMN s2_oauth_provider.token_uri IS 'Token端点';
COMMENT ON COLUMN s2_oauth_provider.user_info_uri IS '用户信息端点';
COMMENT ON COLUMN s2_oauth_provider.jwks_uri IS 'JWKS端点';
COMMENT ON COLUMN s2_oauth_provider.issuer IS 'Token签发者';
COMMENT ON COLUMN s2_oauth_provider.scopes IS 'OAuth范围';
COMMENT ON COLUMN s2_oauth_provider.pkce_enabled IS '是否启用PKCE';
CREATE INDEX IF NOT EXISTS idx_oauth_provider_tenant ON s2_oauth_provider(tenant_id);

-- ========================================
-- PHASE 2: OAuth状态与Token表
-- ========================================

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
COMMENT ON COLUMN s2_oauth_state.state IS '状态参数';
COMMENT ON COLUMN s2_oauth_state.provider_name IS 'OAuth提供者名称';
COMMENT ON COLUMN s2_oauth_state.code_verifier IS 'PKCE验证码';
COMMENT ON COLUMN s2_oauth_state.redirect_uri IS '重定向URI';
COMMENT ON COLUMN s2_oauth_state.nonce IS 'OIDC随机数';
COMMENT ON COLUMN s2_oauth_state.expires_at IS '过期时间';
COMMENT ON COLUMN s2_oauth_state.used IS '是否已使用';
CREATE INDEX IF NOT EXISTS idx_oauth_state_expires ON s2_oauth_state(expires_at);
CREATE INDEX IF NOT EXISTS idx_oauth_state_tenant ON s2_oauth_state(tenant_id);

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
COMMENT ON COLUMN s2_oauth_token.user_id IS '用户ID';
COMMENT ON COLUMN s2_oauth_token.provider_name IS 'OAuth提供者名称';
COMMENT ON COLUMN s2_oauth_token.access_token IS '访问令牌';
COMMENT ON COLUMN s2_oauth_token.refresh_token IS '刷新令牌';
COMMENT ON COLUMN s2_oauth_token.id_token IS 'ID令牌';
COMMENT ON COLUMN s2_oauth_token.token_type IS '令牌类型';
COMMENT ON COLUMN s2_oauth_token.expires_at IS '访问令牌过期时间';
COMMENT ON COLUMN s2_oauth_token.refresh_expires_at IS '刷新令牌过期时间';
COMMENT ON COLUMN s2_oauth_token.scopes IS '授权范围';
CREATE INDEX IF NOT EXISTS idx_oauth_token_user ON s2_oauth_token(user_id, provider_name);

-- ========================================
-- PHASE 3: 会话与刷新令牌表
-- ========================================

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
COMMENT ON COLUMN s2_refresh_token.token_hash IS '令牌SHA-256哈希';
COMMENT ON COLUMN s2_refresh_token.user_id IS '用户ID';
COMMENT ON COLUMN s2_refresh_token.session_id IS '关联会话ID';
COMMENT ON COLUMN s2_refresh_token.issued_at IS '签发时间';
COMMENT ON COLUMN s2_refresh_token.expires_at IS '过期时间';
COMMENT ON COLUMN s2_refresh_token.revoked IS '是否已撤销';
COMMENT ON COLUMN s2_refresh_token.revoked_at IS '撤销时间';
COMMENT ON COLUMN s2_refresh_token.device_info IS '设备信息';
COMMENT ON COLUMN s2_refresh_token.ip_address IS 'IP地址';
CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON s2_refresh_token(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires ON s2_refresh_token(expires_at);

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
COMMENT ON COLUMN s2_user_session.session_id IS '唯一会话标识';
COMMENT ON COLUMN s2_user_session.user_id IS '用户ID';
COMMENT ON COLUMN s2_user_session.auth_method IS '认证方式: LOCAL, OAUTH';
COMMENT ON COLUMN s2_user_session.provider_name IS 'OAuth提供者(如适用)';
COMMENT ON COLUMN s2_user_session.created_at IS '会话创建时间';
COMMENT ON COLUMN s2_user_session.last_activity_at IS '最后活动时间';
COMMENT ON COLUMN s2_user_session.expires_at IS '会话过期时间';
COMMENT ON COLUMN s2_user_session.ip_address IS 'IP地址';
COMMENT ON COLUMN s2_user_session.user_agent IS '用户代理';
COMMENT ON COLUMN s2_user_session.revoked IS '是否已撤销';
COMMENT ON COLUMN s2_user_session.revoked_at IS '撤销时间';
COMMENT ON COLUMN s2_user_session.revoked_reason IS '撤销原因';
CREATE INDEX IF NOT EXISTS idx_session_user ON s2_user_session(user_id);
CREATE INDEX IF NOT EXISTS idx_session_expires ON s2_user_session(expires_at);
