-- ========================================
-- Billing API 路径更新迁移脚本 (PostgreSQL)
-- 版本: V10
-- 说明: 更新订阅管理 API 路径以符合 RESTful 规范
--       从 /api/auth/admin/subscription 改为 /api/v1/subscription-plans
-- ========================================

-- ========================================
-- 1: 更新现有订阅管理权限的路径
-- ========================================

UPDATE s2_permission
SET path = '/api/v1/subscription-plans',
    description = '管理订阅计划 (CRUD)'
WHERE code = 'API_ADMIN_SUBSCRIPTION';

-- ========================================
-- 2: 添加新的订阅相关权限
-- ========================================

-- 租户订阅管理权限 (平台管理员)
INSERT INTO s2_permission (code, name, description, scope, type, path, status, created_by)
SELECT 'API_ADMIN_TENANT_SUBSCRIPTION', '租户订阅管理', '管理租户的订阅分配', 'PLATFORM', 'API', '/api/v1/tenants/*/subscription', 1, 'system'
WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_ADMIN_TENANT_SUBSCRIPTION');

-- 自助订阅权限 (租户用户)
INSERT INTO s2_permission (code, name, description, scope, type, path, status, created_by)
SELECT 'API_MY_SUBSCRIPTION', '我的订阅', '查看和管理自己的订阅', 'TENANT', 'API', '/api/v1/my-subscription', 1, 'system'
WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_MY_SUBSCRIPTION');

-- ========================================
-- 3: 为平台超级管理员分配新权限
-- ========================================

INSERT INTO s2_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM s2_role r, s2_permission p
WHERE r.code = 'PLATFORM_SUPER_ADMIN' AND p.code = 'API_ADMIN_TENANT_SUBSCRIPTION'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ========================================
-- 4: 为租户管理员分配自助订阅权限
-- ========================================

INSERT INTO s2_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM s2_role r, s2_permission p
WHERE r.code = 'TENANT_ADMIN' AND p.code = 'API_MY_SUBSCRIPTION'
ON CONFLICT (role_id, permission_id) DO NOTHING;
