-- ========================================
-- RBAC权限管理迁移脚本 (PostgreSQL)
-- 版本: V2
-- 说明: 添加角色权限管理支持
-- ========================================

-- ========================================
-- 1: 创建RBAC相关表
-- ========================================

-- 角色表
CREATE TABLE IF NOT EXISTS s2_role (
    id BIGSERIAL NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
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
COMMENT ON COLUMN s2_role.name IS '角色名称';
COMMENT ON COLUMN s2_role.code IS '角色编码';
COMMENT ON COLUMN s2_role.is_system IS '是否系统内置角色';
COMMENT ON COLUMN s2_role.status IS '状态：0=禁用，1=启用';
CREATE INDEX IF NOT EXISTS idx_role_tenant ON s2_role(tenant_id);

-- 权限表
CREATE TABLE IF NOT EXISTS s2_permission (
    id BIGSERIAL NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    path VARCHAR(255) DEFAULT NULL,
    icon VARCHAR(100) DEFAULT NULL,
    sort_order INTEGER DEFAULT 0,
    description VARCHAR(500) DEFAULT NULL,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_permission_code UNIQUE (code)
);
COMMENT ON TABLE s2_permission IS '权限定义表';
COMMENT ON COLUMN s2_permission.name IS '权限名称';
COMMENT ON COLUMN s2_permission.code IS '权限编码';
COMMENT ON COLUMN s2_permission.type IS '权限类型: MENU/API/BUTTON';
COMMENT ON COLUMN s2_permission.parent_id IS '父级权限ID';
COMMENT ON COLUMN s2_permission.path IS '前端路径/API路径';
COMMENT ON COLUMN s2_permission.icon IS '图标';
CREATE INDEX IF NOT EXISTS idx_permission_parent ON s2_permission(parent_id);
CREATE INDEX IF NOT EXISTS idx_permission_type ON s2_permission(type);

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
CREATE INDEX IF NOT EXISTS idx_role_permission_role ON s2_role_permission(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permission_perm ON s2_role_permission(permission_id);

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
CREATE INDEX IF NOT EXISTS idx_user_role_user ON s2_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_user_role_role ON s2_user_role(role_id);

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
COMMENT ON COLUMN s2_resource_permission.resource_type IS '资源类型：MODEL/DATASET/DATABASE/AGENT';
COMMENT ON COLUMN s2_resource_permission.principal_type IS '主体类型：USER/ROLE';
COMMENT ON COLUMN s2_resource_permission.permission_type IS '权限类型：READ/WRITE/DELETE/ADMIN';
CREATE INDEX IF NOT EXISTS idx_resource_permission_tenant ON s2_resource_permission(tenant_id);
CREATE INDEX IF NOT EXISTS idx_resource_permission_resource ON s2_resource_permission(resource_type, resource_id);