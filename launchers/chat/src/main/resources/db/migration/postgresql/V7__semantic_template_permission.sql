-- ========================================
-- 语义模板权限迁移脚本 (PostgreSQL)
-- 版本: V8
-- 说明: 添加语义模板菜单和API权限
-- ========================================

-- 添加语义模板菜单权限
INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 12, '语义模板', 'MENU_SEMANTIC_TEMPLATE', 'MENU', NULL, '/semantic-template', 'BlockOutlined', 12, '语义模板管理', 1
WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'MENU_SEMANTIC_TEMPLATE');

-- 语义模板API权限
INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 70, '模板查看', 'API_TEMPLATE_VIEW', 'API', 12, '/api/semantic/template', NULL, 1, '查看语义模板', 1
WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_TEMPLATE_VIEW');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 71, '模板创建', 'API_TEMPLATE_CREATE', 'API', 12, '/api/semantic/template', NULL, 2, '创建语义模板', 1
WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_TEMPLATE_CREATE');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 72, '模板编辑', 'API_TEMPLATE_UPDATE', 'API', 12, '/api/semantic/template', NULL, 3, '编辑语义模板', 1
WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_TEMPLATE_UPDATE');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 73, '模板删除', 'API_TEMPLATE_DELETE', 'API', 12, '/api/semantic/template', NULL, 4, '删除语义模板', 1
WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_TEMPLATE_DELETE');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 74, '模板部署', 'API_TEMPLATE_DEPLOY', 'API', 12, '/api/semantic/template/deploy', NULL, 5, '部署语义模板', 1
WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_TEMPLATE_DEPLOY');

-- 为ADMIN角色(id=1)添加语义模板权限
INSERT INTO s2_role_permission (role_id, permission_id)
SELECT 1, id FROM s2_permission WHERE id IN (12, 70, 71, 72, 73, 74)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 为ANALYST角色(id=2)添加语义模板权限（查看和部署）
INSERT INTO s2_role_permission (role_id, permission_id)
SELECT 2, id FROM s2_permission WHERE id IN (12, 70, 74)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 为TENANT_ADMIN角色(id=4)添加语义模板权限
INSERT INTO s2_role_permission (role_id, permission_id)
SELECT 4, id FROM s2_permission WHERE id IN (12, 70, 71, 72, 73, 74)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 为SUPER_ADMIN角色(id=5)添加语义模板权限
INSERT INTO s2_role_permission (role_id, permission_id)
SELECT 5, id FROM s2_permission WHERE id IN (12, 70, 71, 72, 73, 74)
ON CONFLICT (role_id, permission_id) DO NOTHING;
