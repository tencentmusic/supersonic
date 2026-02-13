-- ========================================
-- 初始化数据迁移脚本 (MySQL)
-- 版本: V4
-- 说明: 插入系统初始化数据
-- 注意: 使用 MySQL 8.0.20+ 兼容的别名语法替代已弃用的 VALUES() 函数
-- ========================================

-- ========================================
-- 1. 默认租户初始化
-- ========================================

-- 插入默认订阅计划
INSERT INTO `s2_subscription_plan` (`id`, `name`, `code`, `description`, `price_monthly`, `price_yearly`, `max_users`, `max_datasets`, `max_models`, `max_agents`, `max_api_calls_per_day`, `max_tokens_per_month`, `is_default`, `status`) VALUES
(1, '免费版', 'FREE', '免费版本，基础功能', 0, 0, 3, 5, 10, 2, 1000, 100000, 1, 'ACTIVE'),
(2, '基础版', 'BASIC', '基础版，适合小团队', 29.00, 290.00, 10, 20, 50, 10, 10000, 1000000, 0, 'ACTIVE'),
(3, '专业版', 'PRO', '专业版，适合成长中的团队', 99.00, 990.00, 50, 100, 200, 50, 100000, 10000000, 0, 'ACTIVE'),
(4, '企业版', 'ENTERPRISE', '企业版，无限制功能', 299.00, 2990.00, -1, -1, -1, -1, -1, -1, 0, 'ACTIVE')
AS new_values
ON DUPLICATE KEY UPDATE `name`=new_values.`name`, `description`=new_values.`description`;

-- 插入默认租户 (plan_id removed - plan is tracked via s2_tenant_subscription)
INSERT INTO `s2_tenant` (`id`, `name`, `code`, `description`, `status`, `created_by`) VALUES
(1, '默认租户', 'default', '系统默认租户', 'ACTIVE', 'system')
AS new_values
ON DUPLICATE KEY UPDATE `name`=new_values.`name`;

-- 插入默认租户订阅
INSERT INTO `s2_tenant_subscription` (`id`, `tenant_id`, `plan_id`, `status`, `start_date`, `billing_cycle`, `auto_renew`, `created_at`, `updated_at`) VALUES
(1, 1, 1, 'ACTIVE', NOW(), 'MONTHLY', 1, NOW(), NOW())
AS new_values
ON DUPLICATE KEY UPDATE `status`=new_values.`status`;

-- ========================================
-- 2. 默认用户初始化
-- 密码默认值: 123456
-- ========================================

INSERT INTO s2_user (`id`, `name`, `password`, `salt`, `display_name`, `email`, `is_admin`, `status`, `tenant_id`, `created_at`, `created_by`) VALUES
(1, 'admin', 'c3VwZXJzb25pY0BiaWNvbdktJJYWw6A3rEmBUPzbn/6DNeYnD+y3mAwDKEMS3KVT', 'jGl25bVBBBW96Qi9Te4V3w==', 'admin', 'admin@supersonic.com', 1, 1, 1, NOW(), 'system'),
(2, 'jack', 'c3VwZXJzb25pY0BiaWNvbWxGalmwa0h/trkh/3CWOYMDiku0Op1VmOfESIKmN0HG', 'MWERWefm/3hD6kYndF6JIg==', 'jack', 'jack@supersonic.com', 0, 1, 1, NOW(), 'system'),
(3, 'tom', 'c3VwZXJzb25pY0BiaWNvbVWv0CZ6HzeX8GRUpw0C8NSaQ+0hE/dAcmzRpCFwAqxK', '4WCPdcXXgT89QDHLML+3hg==', 'tom', 'tom@supersonic.com', 0, 1, 1, NOW(), 'system'),
(4, 'lucy', 'c3VwZXJzb25pY0BiaWNvbc7Ychfu99lPL7rLmCkf/vgF4RASa4Z++Mxo1qlDCpci', '3Jnpqob6uDoGLP9eCAg5Fw==', 'lucy', 'lucy@supersonic.com', 1, 1, 1, NOW(), 'system'),
(5, 'alice', 'c3VwZXJzb25pY0BiaWNvbe9Z4F2/DVIfAJoN1HwUTuH1KgVuiusvfh7KkWYQSNHk', 'K9gGyX8OAK8aH8Myj6djqQ==', 'alice', 'alice@supersonic.com', 0, 1, 1, NOW(), 'system')
AS new_values
ON DUPLICATE KEY UPDATE `display_name`=new_values.`display_name`, `email`=new_values.`email`;

-- ========================================
-- 3: 插入默认角色
-- ========================================

INSERT INTO `s2_role` (`id`, `name`, `code`, `description`, `tenant_id`, `is_system`, `status`, `created_by`) VALUES
(1, '系统管理员', 'ADMIN', '拥有所有权限的超级管理员', 1, 1, 1, 'system'),
(2, '分析师', 'ANALYST', '可以查询和配置模型', 1, 1, 1, 'system'),
(3, '查看者', 'VIEWER', '只读权限', 1, 1, 1, 'system'),
(4, '租户管理员', 'TENANT_ADMIN', '管理本租户内的用户、设置和使用量', 1, 1, 1, 'system'),
(5, 'SaaS管理员', 'SAAS_ADMIN', '管理所有租户、订阅和平台配置', 1, 1, 1, 'system')
AS new_values
ON DUPLICATE KEY UPDATE `name`=new_values.`name`, `description`=new_values.`description`;

-- ========================================
-- 4. 用户-角色关联初始化
-- ========================================

INSERT INTO s2_user_role (`user_id`, `role_id`, `created_at`, `created_by`) VALUES
(1, 1, NOW(), 'system'),  -- admin -> 系统管理员
(2, 2, NOW(), 'system'),  -- jack -> 分析师
(3, 3, NOW(), 'system'),  -- tom -> 查看者
(4, 4, NOW(), 'system'),  -- lucy -> 租户管理员
(5, 2, NOW(), 'system')   -- alice -> 分析师
AS new_values
ON DUPLICATE KEY UPDATE `role_id`=new_values.`role_id`;

-- ========================================
-- 5: 插入默认权限
-- ========================================

INSERT INTO `s2_permission` (`id`, `name`, `code`, `type`, `parent_id`, `path`, `icon`, `sort_order`, `description`, `status`) VALUES
-- 顶级菜单
(1, '对话', 'MENU_CHAT', 'MENU', NULL, '/chat', 'MessageOutlined', 1, '对话功能', 1),
(2, '模型管理', 'MENU_MODEL', 'MENU', NULL, '/model', 'DatabaseOutlined', 2, '语义模型管理', 1),
(3, '指标市场', 'MENU_METRIC', 'MENU', NULL, '/metric', 'BarChartOutlined', 3, '指标市场', 1),
(4, 'Agent管理', 'MENU_AGENT', 'MENU', NULL, '/agent', 'RobotOutlined', 4, 'Agent配置', 1),
(5, '插件管理', 'MENU_PLUGIN', 'MENU', NULL, '/plugin', 'AppstoreOutlined', 5, '插件配置', 1),
(6, '数据库管理', 'MENU_DATABASE', 'MENU', NULL, '/database', 'HddOutlined', 6, '数据库连接管理', 1),
(7, 'LLM配置', 'MENU_LLM', 'MENU', NULL, '/llm', 'ThunderboltOutlined', 7, 'LLM模型配置', 1),
(8, '系统管理', 'MENU_SYSTEM', 'MENU', NULL, '/system', 'SettingOutlined', 8, '系统设置', 1),
-- 租户管理员权限（本租户范围）
(9, '租户设置', 'MENU_TENANT', 'MENU', NULL, '/tenant', 'SettingOutlined', 9, '本租户设置（租户管理员）', 1),
(10, '使用量查看', 'MENU_USAGE', 'MENU', NULL, '/usage', 'DashboardOutlined', 10, '本租户使用量仪表板', 1),
-- SaaS管理员权限（全平台范围）
(11, '平台租户管理', 'MENU_ADMIN_TENANT', 'MENU', NULL, '/admin/tenant', 'TeamOutlined', 11, '所有租户管理(SaaS管理员)', 1),
-- 模型管理API权限
(20, '模型创建', 'API_MODEL_CREATE', 'API', 2, '/api/semantic/model', NULL, 1, '创建模型', 1),
(21, '模型编辑', 'API_MODEL_UPDATE', 'API', 2, '/api/semantic/model', NULL, 2, '编辑模型', 1),
(22, '模型删除', 'API_MODEL_DELETE', 'API', 2, '/api/semantic/model', NULL, 3, '删除模型', 1),
-- Agent管理API权限
(23, 'Agent创建', 'API_AGENT_CREATE', 'API', 4, '/api/chat/agent', NULL, 1, '创建Agent', 1),
(24, 'Agent编辑', 'API_AGENT_UPDATE', 'API', 4, '/api/chat/agent', NULL, 2, '编辑Agent', 1),
(25, 'Agent删除', 'API_AGENT_DELETE', 'API', 4, '/api/chat/agent', NULL, 3, '删除Agent', 1),
-- 租户管理API权限
(30, '租户用户管理', 'API_TENANT_USER', 'API', 9, '/api/auth/tenant/user', NULL, 1, '管理本租户用户', 1),
(31, '租户设置修改', 'API_TENANT_SETTING', 'API', 9, '/api/auth/tenant/setting', NULL, 2, '修改本租户设置', 1),
-- SaaS管理API权限
(40, '租户创建', 'API_ADMIN_TENANT_CREATE', 'API', 11, '/api/auth/admin/tenant', NULL, 1, '创建租户', 1),
(41, '租户编辑', 'API_ADMIN_TENANT_UPDATE', 'API', 11, '/api/auth/admin/tenant', NULL, 2, '编辑租户', 1),
(42, '租户删除', 'API_ADMIN_TENANT_DELETE', 'API', 11, '/api/auth/admin/tenant', NULL, 3, '删除租户', 1),
(43, '订阅管理', 'API_ADMIN_SUBSCRIPTION', 'API', 11, '/api/auth/admin/subscription', NULL, 4, '管理订阅计划', 1),
-- 数据集权限
(50, '数据集查看', 'API_DATASET_VIEW', 'API', 2, '/api/semantic/dataset', NULL, 4, '查看数据集', 1),
(51, '数据集创建', 'API_DATASET_CREATE', 'API', 2, '/api/semantic/dataset', NULL, 5, '创建数据集', 1),
(52, '数据集编辑', 'API_DATASET_UPDATE', 'API', 2, '/api/semantic/dataset', NULL, 6, '编辑数据集', 1),
(53, '数据集删除', 'API_DATASET_DELETE', 'API', 2, '/api/semantic/dataset', NULL, 7, '删除数据集', 1),
-- 数据库权限
(60, '数据库查看', 'API_DATABASE_VIEW', 'API', 6, '/api/semantic/database', NULL, 1, '查看数据库连接', 1),
(61, '数据库创建', 'API_DATABASE_CREATE', 'API', 6, '/api/semantic/database', NULL, 2, '创建数据库连接', 1),
(62, '数据库编辑', 'API_DATABASE_UPDATE', 'API', 6, '/api/semantic/database', NULL, 3, '编辑数据库连接', 1),
(63, '数据库删除', 'API_DATABASE_DELETE', 'API', 6, '/api/semantic/database', NULL, 4, '删除数据库连接', 1)
AS new_values
ON DUPLICATE KEY UPDATE `name`=new_values.`name`, `description`=new_values.`description`;

-- ========================================
-- 6: 角色-权限关联初始化
-- ========================================

-- ADMIN角色 (id=1): 系统管理员拥有所有权限
INSERT INTO `s2_role_permission` (`role_id`, `permission_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
(1, 9), (1, 10), (1, 11),
(1, 20), (1, 21), (1, 22), (1, 23), (1, 24), (1, 25),
(1, 30), (1, 31),
(1, 40), (1, 41), (1, 42), (1, 43),
(1, 50), (1, 51), (1, 52), (1, 53),
(1, 60), (1, 61), (1, 62), (1, 63)
AS new_values
ON DUPLICATE KEY UPDATE `role_id`=new_values.`role_id`;

-- ANALYST角色 (id=2): 分析师
INSERT INTO `s2_role_permission` (`role_id`, `permission_id`) VALUES
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5),
(2, 20), (2, 21), (2, 23), (2, 24),
(2, 50), (2, 51), (2, 52)
AS new_values
ON DUPLICATE KEY UPDATE `role_id`=new_values.`role_id`;

-- VIEWER角色 (id=3): 查看者（只读）
INSERT INTO `s2_role_permission` (`role_id`, `permission_id`) VALUES
(3, 1), (3, 2), (3, 3),
(3, 50), (3, 60)
AS new_values
ON DUPLICATE KEY UPDATE `role_id`=new_values.`role_id`;

-- TENANT_ADMIN角色 (id=4): 租户管理员
INSERT INTO `s2_role_permission` (`role_id`, `permission_id`) VALUES
(4, 1), (4, 2), (4, 3), (4, 4), (4, 5), (4, 6), (4, 7),
(4, 9), (4, 10),
(4, 20), (4, 21), (4, 22), (4, 23), (4, 24), (4, 25),
(4, 30), (4, 31),
(4, 50), (4, 51), (4, 52), (4, 53),
(4, 60), (4, 61), (4, 62), (4, 63)
AS new_values
ON DUPLICATE KEY UPDATE `role_id`=new_values.`role_id`;

-- SAAS_ADMIN角色 (id=5): SaaS管理员（全平台权限）
INSERT INTO `s2_role_permission` (`role_id`, `permission_id`) VALUES
(5, 1), (5, 2), (5, 3), (5, 4), (5, 5), (5, 6), (5, 7), (5, 8),
(5, 9), (5, 10), (5, 11),
(5, 20), (5, 21), (5, 22), (5, 23), (5, 24), (5, 25),
(5, 30), (5, 31),
(5, 40), (5, 41), (5, 42), (5, 43),
(5, 50), (5, 51), (5, 52), (5, 53),
(5, 60), (5, 61), (5, 62), (5, 63)
AS new_values
ON DUPLICATE KEY UPDATE `role_id`=new_values.`role_id`;

-- ========================================
-- 7. 可用日期信息初始化
-- ========================================

INSERT INTO s2_available_date_info (`id`, `item_id`, `type`, `date_format`, `start_date`, `end_date`, `unavailable_date`, `status`, `created_at`, `created_by`, `updated_at`, `updated_by`) VALUES
(1, 1, 'dimension', 'yyyy-MM-dd', DATE_SUB(CURRENT_DATE(), INTERVAL 28 DAY), DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY), '[]', 1, NOW(), 'admin', NOW(), 'admin'),
(2, 2, 'dimension', 'yyyy-MM-dd', DATE_SUB(CURRENT_DATE(), INTERVAL 28 DAY), DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY), '[]', 1, NOW(), 'admin', NOW(), 'admin'),
(3, 3, 'dimension', 'yyyy-MM-dd', DATE_SUB(CURRENT_DATE(), INTERVAL 28 DAY), DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY), '[]', 1, NOW(), 'admin', NOW(), 'admin')
AS new_values
ON DUPLICATE KEY UPDATE `start_date`=new_values.`start_date`, `end_date`=new_values.`end_date`;

-- ========================================
-- 8. 画布配置初始化
-- ========================================

INSERT INTO s2_canvas (`id`, `domain_id`, `type`, `config`, `created_at`, `created_by`, `updated_at`, `updated_by`) VALUES
(1, 1, 'modelEdgeRelation', '[{"source":"datasource-1","target":"datasource-3","type":"polyline","id":"edge-0.305251275235679741702883718912","style":{"active":{"stroke":"rgb(95, 149, 255)","lineWidth":1},"selected":{"stroke":"rgb(95, 149, 255)","lineWidth":2,"shadowColor":"rgb(95, 149, 255)","shadowBlur":10,"text-shape":{"fontWeight":500}},"highlight":{"stroke":"rgb(95, 149, 255)","lineWidth":2,"text-shape":{"fontWeight":500}},"inactive":{"stroke":"rgb(234, 234, 234)","lineWidth":1},"disable":{"stroke":"rgb(245, 245, 245)","lineWidth":1},"stroke":"#296df3","endArrow":true},"startPoint":{"x":-94,"y":-137.5,"anchorIndex":0,"id":"-94|||-137.5"},"endPoint":{"x":-234,"y":-45,"anchorIndex":1,"id":"-234|||-45"},"sourceAnchor":2,"targetAnchor":1,"label":"模型关系编辑"},{"source":"datasource-1","target":"datasource-2","type":"polyline","id":"edge-0.466237264629309141702883756359","style":{"active":{"stroke":"rgb(95, 149, 255)","lineWidth":1},"selected":{"stroke":"rgb(95, 149, 255)","lineWidth":2,"shadowColor":"rgb(95, 149, 255)","shadowBlur":10,"text-shape":{"fontWeight":500}},"highlight":{"stroke":"rgb(95, 149, 255)","lineWidth":2,"text-shape":{"fontWeight":500}},"inactive":{"stroke":"rgb(234, 234, 234)","lineWidth":1},"disable":{"stroke":"rgb(245, 245, 245)","lineWidth":1},"stroke":"#296df3","endArrow":true},"startPoint":{"x":-12,"y":-137.5,"anchorIndex":1,"id":"-12|||-137.5"},"endPoint":{"x":85,"y":31.5,"anchorIndex":0,"id":"85|||31.5"},"sourceAnchor":1,"targetAnchor":2,"label":"模型关系编辑"}]', NOW(), 'admin', NOW(), 'admin')
AS new_values
ON DUPLICATE KEY UPDATE `config`=new_values.`config`;

-- ========================================
-- 9. 系统配置初始化
-- ========================================

INSERT INTO s2_system_config (`id`, `admin`, `parameters`, `tenant_id`) VALUES
(1, 'admin', '{"systemName":"SuperSonic","version":"2.0","enableMultiTenant":true}', 1)
AS new_values
ON DUPLICATE KEY UPDATE `parameters`=new_values.`parameters`;

-- ========================================
-- 10. 认证组初始化
-- ========================================

INSERT INTO s2_auth_groups (`group_id`, `config`, `tenant_id`) VALUES
(1, '{"name":"默认组","permissions":["read","write"]}', 1)
AS new_values
ON DUPLICATE KEY UPDATE `config`=new_values.`config`;
