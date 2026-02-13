-- ========================================
-- 组织架构迁移脚本 (PostgreSQL)
-- 版本: V8
-- 说明: 添加组织架构表和用户-组织关联表
-- ========================================

-- ========================================
-- 1: 组织架构表
-- ========================================

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
COMMENT ON COLUMN s2_organization.parent_id IS '父组织ID，根组织为0';
COMMENT ON COLUMN s2_organization.name IS '组织名称';
COMMENT ON COLUMN s2_organization.full_name IS '组织全名（包含父级路径）';
COMMENT ON COLUMN s2_organization.is_root IS '是否为根组织';
COMMENT ON COLUMN s2_organization.sort_order IS '排序序号';
COMMENT ON COLUMN s2_organization.status IS '状态: 1=启用, 0=禁用';
COMMENT ON COLUMN s2_organization.tenant_id IS '租户ID';

CREATE INDEX IF NOT EXISTS idx_org_parent_id ON s2_organization(parent_id);
CREATE INDEX IF NOT EXISTS idx_org_tenant_id ON s2_organization(tenant_id);

-- ========================================
-- 2: 用户-组织关联表
-- ========================================

CREATE TABLE IF NOT EXISTS s2_user_organization (
    id BIGSERIAL NOT NULL,
    user_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    is_primary SMALLINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_org UNIQUE (user_id, organization_id)
);

COMMENT ON TABLE s2_user_organization IS '用户-组织关联表';
COMMENT ON COLUMN s2_user_organization.user_id IS '用户ID';
COMMENT ON COLUMN s2_user_organization.organization_id IS '组织ID';
COMMENT ON COLUMN s2_user_organization.is_primary IS '是否为主组织';

CREATE INDEX IF NOT EXISTS idx_user_org_user ON s2_user_organization(user_id);
CREATE INDEX IF NOT EXISTS idx_user_org_org ON s2_user_organization(organization_id);

-- ========================================
-- 3: 初始化默认组织数据
-- ========================================

INSERT INTO s2_organization (id, parent_id, name, full_name, is_root, sort_order, status, tenant_id, created_by)
VALUES
(1, 0, 'SuperSonic', 'SuperSonic', 1, 1, 1, 1, 'system'),
(2, 1, 'HR', 'SuperSonic/HR', 0, 1, 1, 1, 'system'),
(3, 1, 'Sales', 'SuperSonic/Sales', 0, 2, 1, 1, 'system'),
(4, 1, 'Marketing', 'SuperSonic/Marketing', 0, 3, 1, 1, 'system')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, full_name = EXCLUDED.full_name;

SELECT setval('s2_organization_id_seq', (SELECT MAX(id) FROM s2_organization));

-- ========================================
-- 4: 初始化用户-组织关联数据
-- ========================================

INSERT INTO s2_user_organization (user_id, organization_id, is_primary, created_by)
VALUES
(1, 1, 1, 'system'),  -- admin -> SuperSonic (根组织)
(2, 2, 1, 'system'),  -- jack -> HR
(3, 3, 1, 'system'),  -- tom -> Sales
(4, 4, 1, 'system'),  -- lucy -> Marketing
(5, 3, 1, 'system')   -- alice -> Sales
ON CONFLICT (user_id, organization_id) DO UPDATE SET is_primary = EXCLUDED.is_primary;

SELECT setval('s2_user_organization_id_seq', (SELECT COALESCE(MAX(id), 1) FROM s2_user_organization));
