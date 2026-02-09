-- ========================================
-- 初始化数据迁移脚本 (PostgreSQL)
-- 版本: V4
-- 说明: 插入系统初始化数据，与data-postgres.sql保持一致
-- ========================================

-- ========================================
-- 1: 插入租户默认数据
-- ========================================

-- 默认订阅计划
INSERT INTO s2_subscription_plan (name, code, description, price_monthly, price_yearly, max_users, max_datasets, max_models, max_agents, max_api_calls_per_day, max_tokens_per_month, is_default, status)
SELECT '免费版', 'FREE', '免费版本，基础功能', 0, 0, 3, 5, 10, 2, 1000, 100000, 1, 'ACTIVE'
    WHERE NOT EXISTS (SELECT 1 FROM s2_subscription_plan WHERE code = 'FREE');

INSERT INTO s2_subscription_plan (name, code, description, price_monthly, price_yearly, max_users, max_datasets, max_models, max_agents, max_api_calls_per_day, max_tokens_per_month, is_default, status)
SELECT '基础版', 'BASIC', '基础版，适合小团队', 29.00, 290.00, 10, 20, 50, 10, 10000, 1000000, 0, 'ACTIVE'
    WHERE NOT EXISTS (SELECT 1 FROM s2_subscription_plan WHERE code = 'BASIC');

INSERT INTO s2_subscription_plan (name, code, description, price_monthly, price_yearly, max_users, max_datasets, max_models, max_agents, max_api_calls_per_day, max_tokens_per_month, is_default, status)
SELECT '专业版', 'PRO', '专业版，适合成长中的团队', 99.00, 990.00, 50, 100, 200, 50, 100000, 10000000, 0, 'ACTIVE'
    WHERE NOT EXISTS (SELECT 1 FROM s2_subscription_plan WHERE code = 'PRO');

INSERT INTO s2_subscription_plan (name, code, description, price_monthly, price_yearly, max_users, max_datasets, max_models, max_agents, max_api_calls_per_day, max_tokens_per_month, is_default, status)
SELECT '企业版', 'ENTERPRISE', '企业版，无限制功能', 299.00, 2990.00, -1, -1, -1, -1, -1, -1, 0, 'ACTIVE'
    WHERE NOT EXISTS (SELECT 1 FROM s2_subscription_plan WHERE code = 'ENTERPRISE');

-- 默认租户 (plan_id removed - plan is tracked via s2_tenant_subscription)
INSERT INTO s2_tenant (id, name, code, description, status, created_by)
SELECT 1, '默认租户', 'default', '系统默认租户', 'ACTIVE', 'system'
    WHERE NOT EXISTS (SELECT 1 FROM s2_tenant WHERE code = 'default');

-- 默认租户订阅
INSERT INTO s2_tenant_subscription (id, tenant_id, plan_id, status, start_date, billing_cycle, auto_renew, created_at, updated_at)
SELECT 1, 1, 1, 'ACTIVE', CURRENT_TIMESTAMP, 'MONTHLY', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM s2_tenant_subscription WHERE tenant_id = 1 AND status = 'ACTIVE');

-- ========================================
-- 2. 默认用户初始化
-- 密码默认值: 123456
-- ========================================

INSERT INTO s2_user (id, name, password, salt, display_name, email, is_admin, status, tenant_id, created_at, created_by)
SELECT 1, 'admin', 'c3VwZXJzb25pY0BiaWNvbdktJJYWw6A3rEmBUPzbn/6DNeYnD+y3mAwDKEMS3KVT', 'jGl25bVBBBW96Qi9Te4V3w==', 'admin', 'admin@supersonic.com', 1, 1, 1, CURRENT_TIMESTAMP, 'system'
WHERE NOT EXISTS (SELECT 1 FROM s2_user WHERE name = 'admin' AND tenant_id = 1);

INSERT INTO s2_user (id, name, password, salt, display_name, email, is_admin, status, tenant_id, created_at, created_by)
SELECT 2, 'jack', 'c3VwZXJzb25pY0BiaWNvbWxGalmwa0h/trkh/3CWOYMDiku0Op1VmOfESIKmN0HG', 'MWERWefm/3hD6kYndF6JIg==', 'jack', 'jack@supersonic.com', 0, 1, 1, CURRENT_TIMESTAMP, 'system'
WHERE NOT EXISTS (SELECT 1 FROM s2_user WHERE name = 'jack' AND tenant_id = 1);

INSERT INTO s2_user (id, name, password, salt, display_name, email, is_admin, status, tenant_id, created_at, created_by)
SELECT 3, 'tom', 'c3VwZXJzb25pY0BiaWNvbVWv0CZ6HzeX8GRUpw0C8NSaQ+0hE/dAcmzRpCFwAqxK', '4WCPdcXXgT89QDHLML+3hg==', 'tom', 'tom@supersonic.com', 0, 1, 1, CURRENT_TIMESTAMP, 'system'
WHERE NOT EXISTS (SELECT 1 FROM s2_user WHERE name = 'tom' AND tenant_id = 1);

INSERT INTO s2_user (id, name, password, salt, display_name, email, is_admin, status, tenant_id, created_at, created_by)
SELECT 4, 'lucy', 'c3VwZXJzb25pY0BiaWNvbc7Ychfu99lPL7rLmCkf/vgF4RASa4Z++Mxo1qlDCpci', '3Jnpqob6uDoGLP9eCAg5Fw==', 'lucy', 'lucy@supersonic.com', 1, 1, 1, CURRENT_TIMESTAMP, 'system'
WHERE NOT EXISTS (SELECT 1 FROM s2_user WHERE name = 'lucy' AND tenant_id = 1);

INSERT INTO s2_user (id, name, password, salt, display_name, email, is_admin, status, tenant_id, created_at, created_by)
SELECT 5, 'alice', 'c3VwZXJzb25pY0BiaWNvbe9Z4F2/DVIfAJoN1HwUTuH1KgVuiusvfh7KkWYQSNHk', 'K9gGyX8OAK8aH8Myj6djqQ==', 'alice', 'alice@supersonic.com', 0, 1, 1, CURRENT_TIMESTAMP, 'system'
WHERE NOT EXISTS (SELECT 1 FROM s2_user WHERE name = 'alice' AND tenant_id = 1);

-- 重置用户序列
SELECT setval('s2_user_id_seq', GREATEST((SELECT MAX(id) FROM s2_user), 5));

-- ========================================
-- 3: 插入默认角色
-- ========================================

INSERT INTO s2_role (id, name, code, description, tenant_id, is_system, status, created_by)
SELECT 1, '系统管理员', 'ADMIN', '拥有所有权限的超级管理员', 1, 1, 1, 'system'
    WHERE NOT EXISTS (SELECT 1 FROM s2_role WHERE code = 'ADMIN' AND tenant_id = 1);

INSERT INTO s2_role (id, name, code, description, tenant_id, is_system, status, created_by)
SELECT 2, '分析师', 'ANALYST', '可以查询和配置模型', 1, 1, 1, 'system'
    WHERE NOT EXISTS (SELECT 1 FROM s2_role WHERE code = 'ANALYST' AND tenant_id = 1);

INSERT INTO s2_role (id, name, code, description, tenant_id, is_system, status, created_by)
SELECT 3, '查看者', 'VIEWER', '只读权限', 1, 1, 1, 'system'
    WHERE NOT EXISTS (SELECT 1 FROM s2_role WHERE code = 'VIEWER' AND tenant_id = 1);

INSERT INTO s2_role (id, name, code, description, tenant_id, is_system, status, created_by)
SELECT 4, '租户管理员', 'TENANT_ADMIN', '管理本租户内的用户、设置和使用量', 1, 1, 1, 'system'
    WHERE NOT EXISTS (SELECT 1 FROM s2_role WHERE code = 'TENANT_ADMIN' AND tenant_id = 1);

INSERT INTO s2_role (id, name, code, description, tenant_id, is_system, status, created_by)
SELECT 5, 'SaaS管理员', 'SAAS_ADMIN', '管理所有租户、订阅和平台配置', 1, 1, 1, 'system'
    WHERE NOT EXISTS (SELECT 1 FROM s2_role WHERE code = 'SAAS_ADMIN' AND tenant_id = 1);

-- ========================================
-- 4: 插入默认权限（菜单权限）
-- ========================================

-- 顶级菜单权限
INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 1, '对话', 'MENU_CHAT', 'MENU', NULL, '/chat', 'MessageOutlined', 1, '对话功能', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'MENU_CHAT');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 2, '模型管理', 'MENU_MODEL', 'MENU', NULL, '/model', 'DatabaseOutlined', 2, '语义模型管理', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'MENU_MODEL');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 3, '指标市场', 'MENU_METRIC', 'MENU', NULL, '/metric', 'BarChartOutlined', 3, '指标市场', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'MENU_METRIC');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 4, 'Agent管理', 'MENU_AGENT', 'MENU', NULL, '/agent', 'RobotOutlined', 4, 'Agent配置', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'MENU_AGENT');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 5, '插件管理', 'MENU_PLUGIN', 'MENU', NULL, '/plugin', 'AppstoreOutlined', 5, '插件配置', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'MENU_PLUGIN');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 6, '数据库管理', 'MENU_DATABASE', 'MENU', NULL, '/database', 'HddOutlined', 6, '数据库连接管理', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'MENU_DATABASE');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 7, 'LLM配置', 'MENU_LLM', 'MENU', NULL, '/llm', 'ThunderboltOutlined', 7, 'LLM模型配置', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'MENU_LLM');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 8, '系统管理', 'MENU_SYSTEM', 'MENU', NULL, '/system', 'SettingOutlined', 8, '系统设置', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'MENU_SYSTEM');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 9, '租户设置', 'MENU_TENANT', 'MENU', NULL, '/tenant', 'SettingOutlined', 9, '本租户设置（租户管理员）', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'MENU_TENANT');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 10, '使用量查看', 'MENU_USAGE', 'MENU', NULL, '/usage', 'DashboardOutlined', 10, '本租户使用量仪表板', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'MENU_USAGE');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 11, '平台租户管理', 'MENU_ADMIN_TENANT', 'MENU', NULL, '/admin/tenant', 'TeamOutlined', 11, '所有租户管理(SaaS管理员)', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'MENU_ADMIN_TENANT');

-- ========================================
-- 5: 插入默认权限（API权限）
-- ========================================

-- 模型管理API权限
INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 20, '模型创建', 'API_MODEL_CREATE', 'API', 2, '/api/semantic/model', NULL, 1, '创建模型', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_MODEL_CREATE');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 21, '模型编辑', 'API_MODEL_UPDATE', 'API', 2, '/api/semantic/model', NULL, 2, '编辑模型', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_MODEL_UPDATE');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 22, '模型删除', 'API_MODEL_DELETE', 'API', 2, '/api/semantic/model', NULL, 3, '删除模型', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_MODEL_DELETE');

-- Agent管理API权限
INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 23, 'Agent创建', 'API_AGENT_CREATE', 'API', 4, '/api/chat/agent', NULL, 1, '创建Agent', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_AGENT_CREATE');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 24, 'Agent编辑', 'API_AGENT_UPDATE', 'API', 4, '/api/chat/agent', NULL, 2, '编辑Agent', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_AGENT_UPDATE');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 25, 'Agent删除', 'API_AGENT_DELETE', 'API', 4, '/api/chat/agent', NULL, 3, '删除Agent', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_AGENT_DELETE');

-- 租户管理API权限
INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 30, '租户用户管理', 'API_TENANT_USER', 'API', 9, '/api/auth/tenant/user', NULL, 1, '管理本租户用户', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_TENANT_USER');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 31, '租户设置修改', 'API_TENANT_SETTING', 'API', 9, '/api/auth/tenant/setting', NULL, 2, '修改本租户设置', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_TENANT_SETTING');

-- SaaS管理API权限
INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 40, '租户创建', 'API_ADMIN_TENANT_CREATE', 'API', 11, '/api/auth/admin/tenant', NULL, 1, '创建租户', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_ADMIN_TENANT_CREATE');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 41, '租户编辑', 'API_ADMIN_TENANT_UPDATE', 'API', 11, '/api/auth/admin/tenant', NULL, 2, '编辑租户', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_ADMIN_TENANT_UPDATE');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 42, '租户删除', 'API_ADMIN_TENANT_DELETE', 'API', 11, '/api/auth/admin/tenant', NULL, 3, '删除租户', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_ADMIN_TENANT_DELETE');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 43, '订阅管理', 'API_ADMIN_SUBSCRIPTION', 'API', 11, '/api/auth/admin/subscription', NULL, 4, '管理订阅计划', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_ADMIN_SUBSCRIPTION');

-- 数据集权限
INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 50, '数据集查看', 'API_DATASET_VIEW', 'API', 2, '/api/semantic/dataset', NULL, 4, '查看数据集', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_DATASET_VIEW');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 51, '数据集创建', 'API_DATASET_CREATE', 'API', 2, '/api/semantic/dataset', NULL, 5, '创建数据集', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_DATASET_CREATE');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 52, '数据集编辑', 'API_DATASET_UPDATE', 'API', 2, '/api/semantic/dataset', NULL, 6, '编辑数据集', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_DATASET_UPDATE');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 53, '数据集删除', 'API_DATASET_DELETE', 'API', 2, '/api/semantic/dataset', NULL, 7, '删除数据集', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_DATASET_DELETE');

-- 数据库权限
INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 60, '数据库查看', 'API_DATABASE_VIEW', 'API', 6, '/api/semantic/database', NULL, 1, '查看数据库连接', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_DATABASE_VIEW');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 61, '数据库创建', 'API_DATABASE_CREATE', 'API', 6, '/api/semantic/database', NULL, 2, '创建数据库连接', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_DATABASE_CREATE');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 62, '数据库编辑', 'API_DATABASE_UPDATE', 'API', 6, '/api/semantic/database', NULL, 3, '编辑数据库连接', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_DATABASE_UPDATE');

INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status)
SELECT 63, '数据库删除', 'API_DATABASE_DELETE', 'API', 6, '/api/semantic/database', NULL, 4, '删除数据库连接', 1
    WHERE NOT EXISTS (SELECT 1 FROM s2_permission WHERE code = 'API_DATABASE_DELETE');

-- ========================================
-- 6: 插入默认角色-权限关联
-- ========================================

-- ADMIN角色 (id=1): 系统管理员拥有所有权限
INSERT INTO s2_role_permission (role_id, permission_id)
SELECT 1, id FROM s2_permission WHERE id IN (1,2,3,4,5,6,7,8,9,10,11,20,21,22,23,24,25,30,31,40,41,42,43,50,51,52,53,60,61,62,63)
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ANALYST角色 (id=2): 分析师
INSERT INTO s2_role_permission (role_id, permission_id)
SELECT 2, id FROM s2_permission WHERE id IN (1,2,3,4,5,20,21,23,24,50,51,52)
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- VIEWER角色 (id=3): 查看者（只读）
INSERT INTO s2_role_permission (role_id, permission_id)
SELECT 3, id FROM s2_permission WHERE id IN (1,2,3,50,60)
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- TENANT_ADMIN角色 (id=4): 租户管理员
INSERT INTO s2_role_permission (role_id, permission_id)
SELECT 4, id FROM s2_permission WHERE id IN (1,2,3,4,5,6,7,9,10,20,21,22,23,24,25,30,31,50,51,52,53,60,61,62,63)
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- SAAS_ADMIN角色 (id=5): SaaS管理员（全平台权限）
INSERT INTO s2_role_permission (role_id, permission_id)
SELECT 5, id FROM s2_permission WHERE id IN (1,2,3,4,5,6,7,8,9,10,11,20,21,22,23,24,25,30,31,40,41,42,43,50,51,52,53,60,61,62,63)
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ========================================
-- 7: 给现有管理员用户分配ADMIN角色
-- ========================================

UPDATE s2_user SET is_admin = 1 WHERE name = 'admin' AND is_admin IS NULL;

-- ========================================
-- 8. 用户-角色关联初始化
-- ========================================

INSERT INTO s2_user_role (user_id, role_id, created_at, created_by)
SELECT 1, 1, CURRENT_TIMESTAMP, 'system'
WHERE NOT EXISTS (SELECT 1 FROM s2_user_role WHERE user_id = 1 AND role_id = 1);  -- admin -> 系统管理员

INSERT INTO s2_user_role (user_id, role_id, created_at, created_by)
SELECT 2, 2, CURRENT_TIMESTAMP, 'system'
WHERE NOT EXISTS (SELECT 1 FROM s2_user_role WHERE user_id = 2 AND role_id = 2);  -- jack -> 分析师

INSERT INTO s2_user_role (user_id, role_id, created_at, created_by)
SELECT 3, 3, CURRENT_TIMESTAMP, 'system'
WHERE NOT EXISTS (SELECT 1 FROM s2_user_role WHERE user_id = 3 AND role_id = 3);  -- tom -> 查看者

INSERT INTO s2_user_role (user_id, role_id, created_at, created_by)
SELECT 4, 4, CURRENT_TIMESTAMP, 'system'
WHERE NOT EXISTS (SELECT 1 FROM s2_user_role WHERE user_id = 4 AND role_id = 4);  -- lucy -> 租户管理员

INSERT INTO s2_user_role (user_id, role_id, created_at, created_by)
SELECT 5, 2, CURRENT_TIMESTAMP, 'system'
WHERE NOT EXISTS (SELECT 1 FROM s2_user_role WHERE user_id = 5 AND role_id = 2);  -- alice -> 分析师

-- ========================================
-- 9. 可用日期信息初始化
-- ========================================

INSERT INTO s2_available_date_info (id, item_id, type, date_format, start_date, end_date, unavailable_date, status, created_at, created_by, updated_at, updated_by)
SELECT 1, 1, 'dimension', 'yyyy-MM-dd', TO_CHAR(CURRENT_DATE - INTERVAL '28 days', 'YYYY-MM-DD'), TO_CHAR(CURRENT_DATE - INTERVAL '1 day', 'YYYY-MM-DD'), '[]', 1, CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin'
WHERE NOT EXISTS (SELECT 1 FROM s2_available_date_info WHERE item_id = 1 AND type = 'dimension');

INSERT INTO s2_available_date_info (id, item_id, type, date_format, start_date, end_date, unavailable_date, status, created_at, created_by, updated_at, updated_by)
SELECT 2, 2, 'dimension', 'yyyy-MM-dd', TO_CHAR(CURRENT_DATE - INTERVAL '28 days', 'YYYY-MM-DD'), TO_CHAR(CURRENT_DATE - INTERVAL '1 day', 'YYYY-MM-DD'), '[]', 1, CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin'
WHERE NOT EXISTS (SELECT 1 FROM s2_available_date_info WHERE item_id = 2 AND type = 'dimension');

INSERT INTO s2_available_date_info (id, item_id, type, date_format, start_date, end_date, unavailable_date, status, created_at, created_by, updated_at, updated_by)
SELECT 3, 3, 'dimension', 'yyyy-MM-dd', TO_CHAR(CURRENT_DATE - INTERVAL '28 days', 'YYYY-MM-DD'), TO_CHAR(CURRENT_DATE - INTERVAL '1 day', 'YYYY-MM-DD'), '[]', 1, CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin'
WHERE NOT EXISTS (SELECT 1 FROM s2_available_date_info WHERE item_id = 3 AND type = 'dimension');

-- 重置序列
SELECT setval('s2_available_date_info_id_seq', GREATEST((SELECT MAX(id) FROM s2_available_date_info), 3));

-- ========================================
-- 10. 画布配置初始化
-- ========================================

INSERT INTO s2_canvas (id, domain_id, type, config, created_at, created_by, updated_at, updated_by)
SELECT 1, 1, 'modelEdgeRelation', '[{"source":"datasource-1","target":"datasource-3","type":"polyline","id":"edge-0.305251275235679741702883718912","style":{"active":{"stroke":"rgb(95, 149, 255)","lineWidth":1},"selected":{"stroke":"rgb(95, 149, 255)","lineWidth":2,"shadowColor":"rgb(95, 149, 255)","shadowBlur":10,"text-shape":{"fontWeight":500}},"highlight":{"stroke":"rgb(95, 149, 255)","lineWidth":2,"text-shape":{"fontWeight":500}},"inactive":{"stroke":"rgb(234, 234, 234)","lineWidth":1},"disable":{"stroke":"rgb(245, 245, 245)","lineWidth":1},"stroke":"#296df3","endArrow":true},"startPoint":{"x":-94,"y":-137.5,"anchorIndex":0,"id":"-94|||-137.5"},"endPoint":{"x":-234,"y":-45,"anchorIndex":1,"id":"-234|||-45"},"sourceAnchor":2,"targetAnchor":1,"label":"模型关系编辑"},{"source":"datasource-1","target":"datasource-2","type":"polyline","id":"edge-0.466237264629309141702883756359","style":{"active":{"stroke":"rgb(95, 149, 255)","lineWidth":1},"selected":{"stroke":"rgb(95, 149, 255)","lineWidth":2,"shadowColor":"rgb(95, 149, 255)","shadowBlur":10,"text-shape":{"fontWeight":500}},"highlight":{"stroke":"rgb(95, 149, 255)","lineWidth":2,"text-shape":{"fontWeight":500}},"inactive":{"stroke":"rgb(234, 234, 234)","lineWidth":1},"disable":{"stroke":"rgb(245, 245, 245)","lineWidth":1},"stroke":"#296df3","endArrow":true},"startPoint":{"x":-12,"y":-137.5,"anchorIndex":1,"id":"-12|||-137.5"},"endPoint":{"x":85,"y":31.5,"anchorIndex":0,"id":"85|||31.5"},"sourceAnchor":1,"targetAnchor":2,"label":"模型关系编辑"}]', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin'
WHERE NOT EXISTS (SELECT 1 FROM s2_canvas WHERE id = 1);

-- 重置序列
SELECT setval('s2_canvas_id_seq', GREATEST((SELECT MAX(id) FROM s2_canvas), 1));

-- ========================================
-- 11. 系统配置初始化
-- ========================================

INSERT INTO s2_system_config (id, admin, parameters, tenant_id)
SELECT 1, 'admin', '{"systemName":"SuperSonic","version":"2.0","enableMultiTenant":true}', 1
WHERE NOT EXISTS (SELECT 1 FROM s2_system_config WHERE id = 1);

-- 重置序列
SELECT setval('s2_system_config_id_seq', GREATEST((SELECT MAX(id) FROM s2_system_config), 1));

-- ========================================
-- 12. 认证组初始化
-- ========================================

INSERT INTO s2_auth_groups (group_id, config, tenant_id)
SELECT 1, '{"name":"默认组","permissions":["read","write"]}', 1
WHERE NOT EXISTS (SELECT 1 FROM s2_auth_groups WHERE group_id = 1);
