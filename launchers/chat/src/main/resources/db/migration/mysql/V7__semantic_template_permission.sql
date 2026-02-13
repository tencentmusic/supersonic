-- ========================================
-- 语义模板权限迁移脚本 (MySQL)
-- 版本: V8
-- 说明: 添加语义模板菜单和API权限
-- ========================================

-- 添加语义模板菜单权限
INSERT INTO `s2_permission` (`id`, `name`, `code`, `type`, `parent_id`, `path`, `icon`, `sort_order`, `description`, `status`) VALUES
(12, '语义模板', 'MENU_SEMANTIC_TEMPLATE', 'MENU', NULL, '/semantic-template', 'BlockOutlined', 12, '语义模板管理', 1),
-- 语义模板API权限
(70, '模板查看', 'API_TEMPLATE_VIEW', 'API', 12, '/api/semantic/template', NULL, 1, '查看语义模板', 1),
(71, '模板创建', 'API_TEMPLATE_CREATE', 'API', 12, '/api/semantic/template', NULL, 2, '创建语义模板', 1),
(72, '模板编辑', 'API_TEMPLATE_UPDATE', 'API', 12, '/api/semantic/template', NULL, 3, '编辑语义模板', 1),
(73, '模板删除', 'API_TEMPLATE_DELETE', 'API', 12, '/api/semantic/template', NULL, 4, '删除语义模板', 1),
(74, '模板部署', 'API_TEMPLATE_DEPLOY', 'API', 12, '/api/semantic/template/deploy', NULL, 5, '部署语义模板', 1)
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `description`=VALUES(`description`);

-- 为ADMIN角色(id=1)添加语义模板权限
INSERT INTO `s2_role_permission` (`role_id`, `permission_id`) VALUES
(1, 12), (1, 70), (1, 71), (1, 72), (1, 73), (1, 74)
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);

-- 为ANALYST角色(id=2)添加语义模板权限（查看和部署）
INSERT INTO `s2_role_permission` (`role_id`, `permission_id`) VALUES
(2, 12), (2, 70), (2, 74)
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);

-- 为TENANT_ADMIN角色(id=4)添加语义模板权限
INSERT INTO `s2_role_permission` (`role_id`, `permission_id`) VALUES
(4, 12), (4, 70), (4, 71), (4, 72), (4, 73), (4, 74)
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);

-- 为SUPER_ADMIN角色(id=5)添加语义模板权限
INSERT INTO `s2_role_permission` (`role_id`, `permission_id`) VALUES
(5, 12), (5, 70), (5, 71), (5, 72), (5, 73), (5, 74)
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
