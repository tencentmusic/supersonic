-- Common (auth + billing + infrastructure) data (PostgreSQL)

-- ========================================
-- SuperSonic Multi-tenant SaaS 初始化数据 (PostgreSQL)
-- Version: 2.0
-- Description: 数据初始化脚本
-- ========================================

-- ========================================
-- 1. 订阅计划初始化
-- ========================================
INSERT INTO s2_subscription_plan (id, name, code, description, price_monthly, price_yearly, max_users, max_datasets, max_models, max_agents, max_api_calls_per_day, max_tokens_per_month, is_default, status)
VALUES
(1, '免费版', 'FREE', '免费版本，基础功能', 0, 0, 3, 5, 10, 2, 1000, 100000, 1, 'ACTIVE'),
(2, '基础版', 'BASIC', '基础版，适合小团队', 29.00, 290.00, 10, 20, 50, 10, 10000, 1000000, 0, 'ACTIVE'),
(3, '专业版', 'PRO', '专业版，适合成长中的团队', 99.00, 990.00, 50, 100, 200, 50, 100000, 10000000, 0, 'ACTIVE'),
(4, '企业版', 'ENTERPRISE', '企业版，无限制功能', 299.00, 2990.00, -1, -1, -1, -1, -1, -1, 0, 'ACTIVE')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description;


-- ========================================
-- 2. 默认租户初始化
-- ========================================
INSERT INTO s2_tenant (id, name, code, description, status, plan_id, created_by)
VALUES (1, '默认租户', 'default', '系统默认租户', 'ACTIVE', 1, 'system')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name;


-- ========================================
-- 2.1 默认租户订阅初始化
-- ========================================
INSERT INTO s2_tenant_subscription (id, tenant_id, plan_id, status, start_date, billing_cycle, auto_renew, created_at, updated_at)
VALUES (1, 1, 1, 'ACTIVE', CURRENT_TIMESTAMP, 'MONTHLY', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status;


-- ========================================
-- 3. 默认角色初始化
-- ========================================
INSERT INTO s2_role (id, name, code, description, tenant_id, is_system, status, scope, created_by)
VALUES
(1, '系统管理员', 'ADMIN', '拥有所有权限的超级管理员', 1, 1, 1, 'TENANT', 'system'),
(2, '分析师', 'ANALYST', '可以查询和配置模型', 1, 1, 1, 'TENANT', 'system'),
(3, '查看者', 'VIEWER', '只读权限', 1, 1, 1, 'TENANT', 'system'),
(4, '租户管理员', 'TENANT_ADMIN', '管理本租户内的用户、设置和使用量', 1, 1, 1, 'TENANT', 'system'),
(5, 'SaaS管理员', 'SAAS_ADMIN', '管理所有租户、订阅和平台配置', NULL, 1, 1, 'PLATFORM', 'system'),
(6, '平台超级管理员', 'PLATFORM_SUPER_ADMIN', '拥有平台所有权限', NULL, 1, 1, 'PLATFORM', 'system'),
(7, '平台运营', 'PLATFORM_OPERATOR', '平台日常运营管理', NULL, 1, 1, 'PLATFORM', 'system')
ON CONFLICT (code, COALESCE(tenant_id, 0)) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, scope = EXCLUDED.scope;


-- ========================================
-- 3.1 默认组织架构初始化
-- ========================================
INSERT INTO s2_organization (id, parent_id, name, full_name, is_root, sort_order, status, tenant_id, created_by)
VALUES
(1, 0, 'SuperSonic', 'SuperSonic', 1, 1, 1, 1, 'system'),
(2, 1, 'HR', 'SuperSonic/HR', 0, 1, 1, 1, 'system'),
(3, 1, 'Sales', 'SuperSonic/Sales', 0, 2, 1, 1, 'system'),
(4, 1, 'Marketing', 'SuperSonic/Marketing', 0, 3, 1, 1, 'system')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, full_name = EXCLUDED.full_name;


-- ========================================
-- 3.2 用户-组织关联初始化
-- ========================================
INSERT INTO s2_user_organization (user_id, organization_id, is_primary, created_by)
VALUES
(1, 1, 1, 'system'),  -- admin -> SuperSonic
(2, 2, 1, 'system'),  -- jack -> HR
(3, 3, 1, 'system'),  -- tom -> Sales
(4, 4, 1, 'system'),  -- lucy -> Marketing
(5, 3, 1, 'system')   -- alice -> Sales
ON CONFLICT (user_id, organization_id) DO UPDATE SET is_primary = EXCLUDED.is_primary;


-- ========================================
-- 4. 默认权限初始化 (菜单权限 + API权限)
-- ========================================
INSERT INTO s2_permission (id, name, code, type, parent_id, path, icon, sort_order, description, status, scope, created_by)
VALUES
-- 顶级菜单 (租户级)
(1, '对话', 'MENU_CHAT', 'MENU', NULL, '/chat', 'MessageOutlined', 1, '对话功能', 1, 'TENANT', 'system'),
(2, '模型管理', 'MENU_MODEL', 'MENU', NULL, '/model', 'DatabaseOutlined', 2, '语义模型管理', 1, 'TENANT', 'system'),
(3, '指标市场', 'MENU_METRIC', 'MENU', NULL, '/metric', 'BarChartOutlined', 3, '指标市场', 1, 'TENANT', 'system'),
(4, 'Agent管理', 'MENU_AGENT', 'MENU', NULL, '/agent', 'RobotOutlined', 4, 'Agent配置', 1, 'TENANT', 'system'),
(5, '插件管理', 'MENU_PLUGIN', 'MENU', NULL, '/plugin', 'AppstoreOutlined', 5, '插件配置', 1, 'TENANT', 'system'),
(6, '数据库管理', 'MENU_DATABASE', 'MENU', NULL, '/database', 'HddOutlined', 6, '数据库连接管理', 1, 'TENANT', 'system'),
(7, 'LLM配置', 'MENU_LLM', 'MENU', NULL, '/llm', 'ThunderboltOutlined', 7, 'LLM模型配置', 1, 'TENANT', 'system'),
(8, '系统管理', 'MENU_SYSTEM', 'MENU', NULL, '/system', 'SettingOutlined', 8, '系统设置', 1, 'TENANT', 'system'),
-- 租户管理菜单 (租户级)
(9, '租户设置', 'MENU_TENANT', 'MENU', NULL, '/tenant', 'SettingOutlined', 9, '本租户设置（租户管理员）', 1, 'TENANT', 'system'),
(10, '使用量查看', 'MENU_USAGE', 'MENU', NULL, '/usage', 'DashboardOutlined', 10, '本租户使用量仪表板', 1, 'TENANT', 'system'),
-- 平台管理菜单 (平台级)
(11, '平台租户管理', 'MENU_ADMIN_TENANT', 'MENU', NULL, '/admin/tenant', 'TeamOutlined', 11, '所有租户管理(SaaS管理员)', 1, 'PLATFORM', 'system'),
(12, '语义模板', 'MENU_SEMANTIC_TEMPLATE', 'MENU', NULL, '/semantic-template', 'BlockOutlined', 12, '语义模板管理', 1, 'TENANT', 'system'),
-- 模型管理API权限 (租户级)
(20, '模型创建', 'API_MODEL_CREATE', 'API', 2, '/api/semantic/model', NULL, 1, '创建模型', 1, 'TENANT', 'system'),
(21, '模型编辑', 'API_MODEL_UPDATE', 'API', 2, '/api/semantic/model', NULL, 2, '编辑模型', 1, 'TENANT', 'system'),
(22, '模型删除', 'API_MODEL_DELETE', 'API', 2, '/api/semantic/model', NULL, 3, '删除模型', 1, 'TENANT', 'system'),
-- Agent管理API权限 (租户级)
(23, 'Agent创建', 'API_AGENT_CREATE', 'API', 4, '/api/chat/agent', NULL, 1, '创建Agent', 1, 'TENANT', 'system'),
(24, 'Agent编辑', 'API_AGENT_UPDATE', 'API', 4, '/api/chat/agent', NULL, 2, '编辑Agent', 1, 'TENANT', 'system'),
(25, 'Agent删除', 'API_AGENT_DELETE', 'API', 4, '/api/chat/agent', NULL, 3, '删除Agent', 1, 'TENANT', 'system'),
-- 租户管理API权限 (租户级)
(30, '租户用户管理', 'API_TENANT_USER', 'API', 9, '/api/auth/tenant/user', NULL, 1, '管理本租户用户', 1, 'TENANT', 'system'),
(31, '租户设置修改', 'API_TENANT_SETTING', 'API', 9, '/api/auth/tenant/setting', NULL, 2, '修改本租户设置', 1, 'TENANT', 'system'),
-- SaaS管理API权限 (平台级)
(40, '租户创建', 'API_ADMIN_TENANT_CREATE', 'API', 11, '/api/auth/admin/tenant', NULL, 1, '创建租户', 1, 'PLATFORM', 'system'),
(41, '租户编辑', 'API_ADMIN_TENANT_UPDATE', 'API', 11, '/api/auth/admin/tenant', NULL, 2, '编辑租户', 1, 'PLATFORM', 'system'),
(42, '租户删除', 'API_ADMIN_TENANT_DELETE', 'API', 11, '/api/auth/admin/tenant', NULL, 3, '删除租户', 1, 'PLATFORM', 'system'),
(43, '订阅计划管理', 'API_ADMIN_SUBSCRIPTION', 'API', 11, '/api/v1/subscription-plans', NULL, 4, '管理订阅计划 (CRUD)', 1, 'PLATFORM', 'system'),
(44, '租户订阅管理', 'API_ADMIN_TENANT_SUBSCRIPTION', 'API', 11, '/api/v1/tenants/*/subscription', NULL, 5, '管理租户的订阅分配', 1, 'PLATFORM', 'system'),
(45, '我的订阅', 'API_MY_SUBSCRIPTION', 'API', 9, '/api/v1/my-subscription', NULL, 3, '查看和管理自己的订阅', 1, 'TENANT', 'system'),
-- 数据集权限 (租户级)
(50, '数据集查看', 'API_DATASET_VIEW', 'API', 2, '/api/semantic/dataset', NULL, 4, '查看数据集', 1, 'TENANT', 'system'),
(51, '数据集创建', 'API_DATASET_CREATE', 'API', 2, '/api/semantic/dataset', NULL, 5, '创建数据集', 1, 'TENANT', 'system'),
(52, '数据集编辑', 'API_DATASET_UPDATE', 'API', 2, '/api/semantic/dataset', NULL, 6, '编辑数据集', 1, 'TENANT', 'system'),
(53, '数据集删除', 'API_DATASET_DELETE', 'API', 2, '/api/semantic/dataset', NULL, 7, '删除数据集', 1, 'TENANT', 'system'),
-- 数据库权限 (租户级)
(60, '数据库查看', 'API_DATABASE_VIEW', 'API', 6, '/api/semantic/database', NULL, 1, '查看数据库连接', 1, 'TENANT', 'system'),
(61, '数据库创建', 'API_DATABASE_CREATE', 'API', 6, '/api/semantic/database', NULL, 2, '创建数据库连接', 1, 'TENANT', 'system'),
(62, '数据库编辑', 'API_DATABASE_UPDATE', 'API', 6, '/api/semantic/database', NULL, 3, '编辑数据库连接', 1, 'TENANT', 'system'),
(63, '数据库删除', 'API_DATABASE_DELETE', 'API', 6, '/api/semantic/database', NULL, 4, '删除数据库连接', 1, 'TENANT', 'system'),
-- 语义模板API权限 (租户级)
(70, '模板查看', 'API_TEMPLATE_VIEW', 'API', 12, '/api/semantic/template', NULL, 1, '查看语义模板', 1, 'TENANT', 'system'),
(71, '模板创建', 'API_TEMPLATE_CREATE', 'API', 12, '/api/semantic/template', NULL, 2, '创建语义模板', 1, 'TENANT', 'system'),
(72, '模板编辑', 'API_TEMPLATE_UPDATE', 'API', 12, '/api/semantic/template', NULL, 3, '编辑语义模板', 1, 'TENANT', 'system'),
(73, '模板删除', 'API_TEMPLATE_DELETE', 'API', 12, '/api/semantic/template', NULL, 4, '删除语义模板', 1, 'TENANT', 'system'),
(74, '模板部署', 'API_TEMPLATE_DEPLOY', 'API', 12, '/api/semantic/template/deploy', NULL, 5, '部署语义模板', 1, 'TENANT', 'system'),
-- 平台级权限 (新增)
(100, '平台管理员', 'PLATFORM_ADMIN', 'MENU', NULL, '/platform', 'CrownOutlined', 100, '平台管理入口权限', 1, 'PLATFORM', 'system'),
(101, '租户管理', 'PLATFORM_TENANT_MANAGE', 'MENU', 100, '/platform/tenants', 'TeamOutlined', 1, '管理所有租户', 1, 'PLATFORM', 'system'),
(102, '订阅计划管理', 'PLATFORM_SUBSCRIPTION', 'MENU', 100, '/platform/subscriptions', 'MoneyCollectOutlined', 2, '管理订阅计划', 1, 'PLATFORM', 'system'),
(103, '平台角色管理', 'PLATFORM_ROLE_MANAGE', 'MENU', 100, '/platform/roles', 'UserSwitchOutlined', 3, '管理平台级角色', 1, 'PLATFORM', 'system'),
(104, '平台权限管理', 'PLATFORM_PERMISSION', 'MENU', 100, '/platform/permissions', 'SafetyCertificateOutlined', 4, '管理平台级权限', 1, 'PLATFORM', 'system'),
(105, '系统设置', 'PLATFORM_SETTINGS', 'MENU', 100, '/platform/settings', 'SettingOutlined', 5, '系统全局配置', 1, 'PLATFORM', 'system'),
-- 租户级权限 (新增)
(110, '租户管理入口', 'TENANT_ADMIN', 'MENU', NULL, '/tenant', 'BankOutlined', 110, '租户管理入口权限', 1, 'TENANT', 'system'),
(111, '组织架构管理', 'TENANT_ORG_MANAGE', 'MENU', 110, '/tenant/organization', 'ApartmentOutlined', 1, '管理本租户组织架构', 1, 'TENANT', 'system'),
(112, '成员管理', 'TENANT_MEMBER_MANAGE', 'MENU', 110, '/tenant/members', 'UserOutlined', 2, '管理本租户成员', 1, 'TENANT', 'system'),
(113, '角色管理', 'TENANT_ROLE_MANAGE', 'MENU', 110, '/tenant/roles', 'IdcardOutlined', 3, '管理本租户角色', 1, 'TENANT', 'system'),
(114, '权限管理', 'TENANT_PERMISSION', 'MENU', 110, '/tenant/permissions', 'KeyOutlined', 4, '管理本租户权限', 1, 'TENANT', 'system'),
(115, '租户设置', 'TENANT_SETTINGS', 'MENU', 110, '/tenant/settings', 'ControlOutlined', 5, '本租户配置', 1, 'TENANT', 'system'),
(116, '用量统计', 'TENANT_USAGE_VIEW', 'MENU', 110, '/tenant/usage', 'LineChartOutlined', 6, '查看本租户用量', 1, 'TENANT', 'system')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, scope = EXCLUDED.scope;


-- ========================================
-- 5. 角色-权限关联初始化
-- ========================================

-- ADMIN角色 (id=1): 系统管理员拥有所有权限
INSERT INTO s2_role_permission (role_id, permission_id)
VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
(1, 9), (1, 10), (1, 11), (1, 12),
(1, 20), (1, 21), (1, 22), (1, 23), (1, 24), (1, 25),
(1, 30), (1, 31),
(1, 40), (1, 41), (1, 42), (1, 43),
(1, 50), (1, 51), (1, 52), (1, 53),
(1, 60), (1, 61), (1, 62), (1, 63),
(1, 70), (1, 71), (1, 72), (1, 73), (1, 74)
ON CONFLICT (role_id, permission_id) DO NOTHING;


-- ANALYST角色 (id=2): 分析师
INSERT INTO s2_role_permission (role_id, permission_id)
VALUES
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 12),
(2, 20), (2, 21), (2, 23), (2, 24),
(2, 50), (2, 51), (2, 52),
(2, 70), (2, 74)
ON CONFLICT (role_id, permission_id) DO NOTHING;


-- VIEWER角色 (id=3): 查看者（只读）
INSERT INTO s2_role_permission (role_id, permission_id)
VALUES
(3, 1), (3, 2), (3, 3),
(3, 50), (3, 60)
ON CONFLICT (role_id, permission_id) DO NOTHING;


-- TENANT_ADMIN角色 (id=4): 租户管理员
INSERT INTO s2_role_permission (role_id, permission_id)
VALUES
(4, 1), (4, 2), (4, 3), (4, 4), (4, 5), (4, 6), (4, 7),
(4, 9), (4, 10), (4, 12),
(4, 20), (4, 21), (4, 22), (4, 23), (4, 24), (4, 25),
(4, 30), (4, 31),
(4, 50), (4, 51), (4, 52), (4, 53),
(4, 60), (4, 61), (4, 62), (4, 63),
(4, 70), (4, 71), (4, 72), (4, 73), (4, 74),
(4, 110), (4, 111), (4, 112), (4, 113), (4, 114), (4, 115), (4, 116)
ON CONFLICT (role_id, permission_id) DO NOTHING;


-- SAAS_ADMIN角色 (id=5): SaaS管理员（全平台权限）
INSERT INTO s2_role_permission (role_id, permission_id)
VALUES
(5, 1), (5, 2), (5, 3), (5, 4), (5, 5), (5, 6), (5, 7), (5, 8),
(5, 9), (5, 10), (5, 11), (5, 12),
(5, 20), (5, 21), (5, 22), (5, 23), (5, 24), (5, 25),
(5, 30), (5, 31),
(5, 40), (5, 41), (5, 42), (5, 43),
(5, 50), (5, 51), (5, 52), (5, 53),
(5, 60), (5, 61), (5, 62), (5, 63),
(5, 70), (5, 71), (5, 72), (5, 73), (5, 74),
(5, 100), (5, 101), (5, 102), (5, 103), (5, 104), (5, 105)
ON CONFLICT (role_id, permission_id) DO NOTHING;


-- PLATFORM_SUPER_ADMIN角色 (id=6): 平台超级管理员（所有平台权限）
INSERT INTO s2_role_permission (role_id, permission_id)
VALUES
(6, 100), (6, 101), (6, 102), (6, 103), (6, 104), (6, 105),
(6, 11), (6, 40), (6, 41), (6, 42), (6, 43)
ON CONFLICT (role_id, permission_id) DO NOTHING;


-- PLATFORM_OPERATOR角色 (id=7): 平台运营（部分平台权限）
INSERT INTO s2_role_permission (role_id, permission_id)
VALUES
(7, 100), (7, 101), (7, 102)
ON CONFLICT (role_id, permission_id) DO NOTHING;


-- ========================================
-- 6. 默认用户初始化
-- 密码默认值: 123456
-- ========================================
INSERT INTO s2_user (id, name, password, salt, display_name, email, is_admin, status, tenant_id, created_at, created_by)
VALUES
(1, 'admin', 'c3VwZXJzb25pY0BiaWNvbdktJJYWw6A3rEmBUPzbn/6DNeYnD+y3mAwDKEMS3KVT', 'jGl25bVBBBW96Qi9Te4V3w==', 'admin', 'admin@supersonic.com', 1, 1, 1, CURRENT_TIMESTAMP, 'system'),
(2, 'jack', 'c3VwZXJzb25pY0BiaWNvbWxGalmwa0h/trkh/3CWOYMDiku0Op1VmOfESIKmN0HG', 'MWERWefm/3hD6kYndF6JIg==', 'jack', 'jack@supersonic.com', 0, 1, 1, CURRENT_TIMESTAMP, 'system'),
(3, 'tom', 'c3VwZXJzb25pY0BiaWNvbVWv0CZ6HzeX8GRUpw0C8NSaQ+0hE/dAcmzRpCFwAqxK', '4WCPdcXXgT89QDHLML+3hg==', 'tom', 'tom@supersonic.com', 0, 1, 1, CURRENT_TIMESTAMP, 'system'),
(4, 'lucy', 'c3VwZXJzb25pY0BiaWNvbc7Ychfu99lPL7rLmCkf/vgF4RASa4Z++Mxo1qlDCpci', '3Jnpqob6uDoGLP9eCAg5Fw==', 'lucy', 'lucy@supersonic.com', 1, 1, 1, CURRENT_TIMESTAMP, 'system'),
(5, 'alice', 'c3VwZXJzb25pY0BiaWNvbe9Z4F2/DVIfAJoN1HwUTuH1KgVuiusvfh7KkWYQSNHk', 'K9gGyX8OAK8aH8Myj6djqQ==', 'alice', 'alice@supersonic.com', 0, 1, 1, CURRENT_TIMESTAMP, 'system')
ON CONFLICT (name, tenant_id) DO UPDATE SET display_name = EXCLUDED.display_name, email = EXCLUDED.email;


-- ========================================
-- 7. 用户-角色关联初始化
-- ========================================
INSERT INTO s2_user_role (user_id, role_id, created_at, created_by)
VALUES
(1, 1, CURRENT_TIMESTAMP, 'system'),  -- admin -> 系统管理员 (租户级)
(1, 6, CURRENT_TIMESTAMP, 'system'),  -- admin -> 平台超级管理员 (平台级)
(2, 2, CURRENT_TIMESTAMP, 'system'),  -- jack -> 分析师
(3, 3, CURRENT_TIMESTAMP, 'system'),  -- tom -> 查看者
(4, 4, CURRENT_TIMESTAMP, 'system'),  -- lucy -> 租户管理员
(5, 2, CURRENT_TIMESTAMP, 'system')   -- alice -> 分析师
ON CONFLICT (user_id, role_id) DO NOTHING;


-- ========================================
-- 10. 系统配置初始化
-- ========================================
INSERT INTO s2_system_config (id, admin, parameters, tenant_id)
VALUES (1, 'admin', '{"systemName":"SuperSonic","version":"2.0","enableMultiTenant":true}', 1)
ON CONFLICT (id) DO UPDATE SET parameters = EXCLUDED.parameters;


-- ========================================
-- 11. 认证组初始化
-- ========================================
INSERT INTO s2_auth_groups (group_id, config, tenant_id)
VALUES (1, '{"name":"默认组","permissions":["read","write"]}', 1)
ON CONFLICT (group_id) DO UPDATE SET config = EXCLUDED.config;
