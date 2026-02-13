-- ========================================
-- Platform/Tenant RBAC 迁移脚本 (PostgreSQL)
-- 版本: V9
-- 说明: 区分平台级和租户级的角色权限管理
-- ========================================

-- ========================================
-- 1: 修改角色表，添加 scope 字段
-- ========================================

ALTER TABLE s2_role ADD COLUMN IF NOT EXISTS scope VARCHAR(20) DEFAULT 'TENANT';
COMMENT ON COLUMN s2_role.scope IS '作用域: PLATFORM=平台级, TENANT=租户级';

-- ========================================
-- 2: 修改权限表，添加 scope、type、created_by 字段
-- ========================================

ALTER TABLE s2_permission ADD COLUMN IF NOT EXISTS scope VARCHAR(20) DEFAULT 'TENANT';
ALTER TABLE s2_permission ADD COLUMN IF NOT EXISTS type VARCHAR(20) DEFAULT 'MENU';
ALTER TABLE s2_permission ADD COLUMN IF NOT EXISTS created_by VARCHAR(100) DEFAULT NULL;
COMMENT ON COLUMN s2_permission.scope IS '作用域: PLATFORM=平台级, TENANT=租户级';
COMMENT ON COLUMN s2_permission.type IS '权限类型: MENU=菜单, BUTTON=按钮, DATA=数据, API=接口';
COMMENT ON COLUMN s2_permission.created_by IS '创建人';

-- 修改 s2_role_permission 表，添加 created_by 字段
ALTER TABLE s2_role_permission ADD COLUMN IF NOT EXISTS created_by VARCHAR(100) DEFAULT NULL;
COMMENT ON COLUMN s2_role_permission.created_by IS '创建人';

-- 修改 s2_user_organization 表，添加 tenant_id 字段
ALTER TABLE s2_user_organization ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
COMMENT ON COLUMN s2_user_organization.tenant_id IS '租户ID';
CREATE INDEX IF NOT EXISTS idx_user_org_tenant ON s2_user_organization(tenant_id);

-- ========================================
-- 3: 添加平台级权限数据
-- ========================================

INSERT INTO s2_permission (code, name, description, scope, type, status, created_by)
VALUES
('PLATFORM_ADMIN', '平台管理员', '平台管理入口权限', 'PLATFORM', 'MENU', 1, 'system'),
('PLATFORM_TENANT_MANAGE', '租户管理', '管理所有租户', 'PLATFORM', 'MENU', 1, 'system'),
('PLATFORM_SUBSCRIPTION', '订阅计划管理', '管理订阅计划', 'PLATFORM', 'MENU', 1, 'system'),
('PLATFORM_ROLE_MANAGE', '平台角色管理', '管理平台级角色', 'PLATFORM', 'MENU', 1, 'system'),
('PLATFORM_PERMISSION', '平台权限管理', '管理平台级权限', 'PLATFORM', 'MENU', 1, 'system'),
('PLATFORM_SETTINGS', '系统设置', '系统全局配置', 'PLATFORM', 'MENU', 1, 'system')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, scope = EXCLUDED.scope;

-- ========================================
-- 4: 添加租户级权限数据
-- ========================================

INSERT INTO s2_permission (code, name, description, scope, type, status, created_by)
VALUES
('TENANT_ADMIN', '租户管理员', '租户管理入口权限', 'TENANT', 'MENU', 1, 'system'),
('TENANT_ORG_MANAGE', '组织架构管理', '管理本租户组织架构', 'TENANT', 'MENU', 1, 'system'),
('TENANT_MEMBER_MANAGE', '成员管理', '管理本租户成员', 'TENANT', 'MENU', 1, 'system'),
('TENANT_ROLE_MANAGE', '角色管理', '管理本租户角色', 'TENANT', 'MENU', 1, 'system'),
('TENANT_PERMISSION', '权限管理', '管理本租户权限', 'TENANT', 'MENU', 1, 'system'),
('TENANT_SETTINGS', '租户设置', '本租户配置', 'TENANT', 'MENU', 1, 'system'),
('TENANT_USAGE_VIEW', '用量统计', '查看本租户用量', 'TENANT', 'MENU', 1, 'system')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, scope = EXCLUDED.scope;

-- ========================================
-- 5: 添加平台级角色
-- ========================================

INSERT INTO s2_role (name, code, description, scope, status, created_by)
VALUES
('平台超级管理员', 'PLATFORM_SUPER_ADMIN', '拥有平台所有权限', 'PLATFORM', 1, 'system'),
('平台运营', 'PLATFORM_OPERATOR', '平台日常运营管理', 'PLATFORM', 1, 'system')
ON CONFLICT (code, tenant_id) DO UPDATE SET name = EXCLUDED.name, scope = EXCLUDED.scope;

-- ========================================
-- 6: 更新现有角色的 scope 字段
-- ========================================

-- 将现有租户级角色设置为 TENANT scope
UPDATE s2_role SET scope = 'TENANT' WHERE code IN ('ADMIN', 'ANALYST', 'VIEWER', 'TENANT_ADMIN') AND scope IS NULL;

-- 将 SAAS_ADMIN 设置为 PLATFORM scope
UPDATE s2_role SET scope = 'PLATFORM' WHERE code = 'SAAS_ADMIN';

-- ========================================
-- 7: 为平台超级管理员分配所有平台权限
-- ========================================

INSERT INTO s2_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM s2_role r, s2_permission p
WHERE r.code = 'PLATFORM_SUPER_ADMIN' AND p.scope = 'PLATFORM'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ========================================
-- 8: 为租户管理员分配新的租户级权限
-- ========================================

INSERT INTO s2_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM s2_role r, s2_permission p
WHERE r.code = 'TENANT_ADMIN' AND r.tenant_id = 1 AND p.code IN ('TENANT_ADMIN', 'TENANT_ORG_MANAGE', 'TENANT_MEMBER_MANAGE', 'TENANT_ROLE_MANAGE', 'TENANT_PERMISSION', 'TENANT_SETTINGS', 'TENANT_USAGE_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ========================================
-- 9: 为现有 admin 用户分配平台超级管理员角色
-- ========================================

INSERT INTO s2_user_role (user_id, role_id, created_at, created_by)
SELECT u.id, r.id, CURRENT_TIMESTAMP, 'system'
FROM s2_user u, s2_role r
WHERE u.name = 'admin' AND u.is_admin = 1 AND r.code = 'PLATFORM_SUPER_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;
