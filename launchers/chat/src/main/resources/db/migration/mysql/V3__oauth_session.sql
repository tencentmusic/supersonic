-- ========================================
-- OAuth与会话管理迁移脚本 (MySQL)
-- 版本: V3
-- 说明: 添加OAuth认证和会话管理支持
-- ========================================

-- ========================================
-- 1: OAuth提供者配置表
-- ========================================

-- OAuth提供者配置表
CREATE TABLE IF NOT EXISTS `s2_oauth_provider` (
    `id` bigint NOT NULL AUTO_INCREMENT,
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
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_oauth_provider_name_tenant` (`name`, `tenant_id`),
    KEY `idx_oauth_provider_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OAuth提供者配置表';

-- ========================================
-- 2: OAuth状态与Token表
-- ========================================

-- OAuth状态表
CREATE TABLE IF NOT EXISTS `s2_oauth_state` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `state` varchar(128) NOT NULL COMMENT '状态参数',
    `provider_name` varchar(100) NOT NULL COMMENT 'OAuth提供者名称',
    `code_verifier` varchar(128) DEFAULT NULL COMMENT 'PKCE验证码',
    `redirect_uri` varchar(500) DEFAULT NULL COMMENT '重定向URI',
    `nonce` varchar(128) DEFAULT NULL COMMENT 'OIDC随机数',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
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
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL COMMENT '用户ID',
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

-- ========================================
-- 3: 会话与刷新令牌表
-- ========================================

-- 刷新令牌表
CREATE TABLE IF NOT EXISTS `s2_refresh_token` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `token_hash` varchar(128) NOT NULL COMMENT '令牌SHA-256哈希',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `session_id` bigint DEFAULT NULL COMMENT '关联会话ID',
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
    `id` bigint NOT NULL AUTO_INCREMENT,
    `session_id` varchar(128) NOT NULL COMMENT '唯一会话标识',
    `user_id` bigint NOT NULL COMMENT '用户ID',
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
