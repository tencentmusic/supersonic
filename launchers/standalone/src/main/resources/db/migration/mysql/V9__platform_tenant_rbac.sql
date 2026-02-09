-- ========================================
-- Platform/Tenant RBAC 迁移脚本 (MySQL)
-- 版本: V9
-- 说明: 区分平台级和租户级的角色权限管理
-- ========================================

-- ========================================
-- 1: 修改表结构，添加 scope 字段
-- ========================================

-- 使用存储过程安全添加列（兼容 MySQL 5.7+）
DROP PROCEDURE IF EXISTS add_column_if_not_exists;

DELIMITER //
CREATE PROCEDURE add_column_if_not_exists()
BEGIN
    -- s2_role.scope
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 's2_role' AND column_name = 'scope') THEN
        ALTER TABLE `s2_role` ADD COLUMN `scope` varchar(20) DEFAULT 'TENANT' COMMENT '作用域: PLATFORM=平台级, TENANT=租户级';
    END IF;

    -- s2_permission.scope
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 's2_permission' AND column_name = 'scope') THEN
        ALTER TABLE `s2_permission` ADD COLUMN `scope` varchar(20) DEFAULT 'TENANT' COMMENT '作用域: PLATFORM=平台级, TENANT=租户级';
    END IF;

    -- s2_permission.type (如果不存在)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 's2_permission' AND column_name = 'type') THEN
        ALTER TABLE `s2_permission` ADD COLUMN `type` varchar(20) DEFAULT 'MENU' COMMENT '权限类型: MENU=菜单, BUTTON=按钮, DATA=数据, API=接口';
    END IF;

    -- s2_permission.created_by (如果不存在)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 's2_permission' AND column_name = 'created_by') THEN
        ALTER TABLE `s2_permission` ADD COLUMN `created_by` varchar(100) DEFAULT NULL COMMENT '创建人';
    END IF;

    -- s2_role_permission.created_by (如果不存在)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 's2_role_permission' AND column_name = 'created_by') THEN
        ALTER TABLE `s2_role_permission` ADD COLUMN `created_by` varchar(100) DEFAULT NULL COMMENT '创建人';
    END IF;

    -- s2_user_organization.tenant_id (如果不存在)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 's2_user_organization' AND column_name = 'tenant_id') THEN
        ALTER TABLE `s2_user_organization` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID';
        CREATE INDEX `idx_user_org_tenant` ON `s2_user_organization` (`tenant_id`);
    END IF;
END //
DELIMITER ;

CALL add_column_if_not_exists();
DROP PROCEDURE IF EXISTS add_column_if_not_exists;

-- ========================================
-- 2: 添加平台级权限数据
-- ========================================

INSERT INTO `s2_permission` (`code`, `name`, `description`, `scope`, `type`, `status`, `created_by`)
VALUES
('PLATFORM_ADMIN', '平台管理员', '平台管理入口权限', 'PLATFORM', 'MENU', 1, 'system'),
('PLATFORM_TENANT_MANAGE', '租户管理', '管理所有租户', 'PLATFORM', 'MENU', 1, 'system'),
('PLATFORM_SUBSCRIPTION', '订阅计划管理', '管理订阅计划', 'PLATFORM', 'MENU', 1, 'system'),
('PLATFORM_ROLE_MANAGE', '平台角色管理', '管理平台级角色', 'PLATFORM', 'MENU', 1, 'system'),
('PLATFORM_PERMISSION', '平台权限管理', '管理平台级权限', 'PLATFORM', 'MENU', 1, 'system'),
('PLATFORM_SETTINGS', '系统设置', '系统全局配置', 'PLATFORM', 'MENU', 1, 'system')
AS new_val ON DUPLICATE KEY UPDATE `name`=new_val.`name`, `scope`=new_val.`scope`;

-- ========================================
-- 3: 添加租户级权限数据
-- ========================================

INSERT INTO `s2_permission` (`code`, `name`, `description`, `scope`, `type`, `status`, `created_by`)
VALUES
('TENANT_ADMIN', '租户管理员', '租户管理入口权限', 'TENANT', 'MENU', 1, 'system'),
('TENANT_ORG_MANAGE', '组织架构管理', '管理本租户组织架构', 'TENANT', 'MENU', 1, 'system'),
('TENANT_MEMBER_MANAGE', '成员管理', '管理本租户成员', 'TENANT', 'MENU', 1, 'system'),
('TENANT_ROLE_MANAGE', '角色管理', '管理本租户角色', 'TENANT', 'MENU', 1, 'system'),
('TENANT_PERMISSION', '权限管理', '管理本租户权限', 'TENANT', 'MENU', 1, 'system'),
('TENANT_SETTINGS', '租户设置', '本租户配置', 'TENANT', 'MENU', 1, 'system'),
('TENANT_USAGE_VIEW', '用量统计', '查看本租户用量', 'TENANT', 'MENU', 1, 'system')
AS new_val ON DUPLICATE KEY UPDATE `name`=new_val.`name`, `scope`=new_val.`scope`;

-- ========================================
-- 4: 添加平台级角色
-- ========================================

INSERT INTO `s2_role` (`name`, `code`, `description`, `scope`, `status`, `created_by`)
VALUES
('平台超级管理员', 'PLATFORM_SUPER_ADMIN', '拥有平台所有权限', 'PLATFORM', 1, 'system'),
('平台运营', 'PLATFORM_OPERATOR', '平台日常运营管理', 'PLATFORM', 1, 'system')
AS new_val ON DUPLICATE KEY UPDATE `name`=new_val.`name`, `scope`=new_val.`scope`;

-- ========================================
-- 5: 更新现有角色的 scope 字段
-- ========================================

-- 将现有租户级角色设置为 TENANT scope
UPDATE `s2_role` SET `scope` = 'TENANT' WHERE `code` IN ('ADMIN', 'ANALYST', 'VIEWER', 'TENANT_ADMIN') AND `scope` IS NULL;

-- 将 SAAS_ADMIN 设置为 PLATFORM scope
UPDATE `s2_role` SET `scope` = 'PLATFORM' WHERE `code` = 'SAAS_ADMIN';

-- ========================================
-- 6: 为平台超级管理员分配所有平台权限
-- ========================================

INSERT INTO `s2_role_permission` (`role_id`, `permission_id`, `created_by`)
SELECT r.id, p.id, 'system'
FROM `s2_role` r, `s2_permission` p
WHERE r.code = 'PLATFORM_SUPER_ADMIN' AND p.scope = 'PLATFORM'
ON DUPLICATE KEY UPDATE `role_id`=`role_id`;

-- ========================================
-- 7: 为租户管理员分配新的租户级权限
-- ========================================

INSERT INTO `s2_role_permission` (`role_id`, `permission_id`, `created_by`)
SELECT r.id, p.id, 'system'
FROM `s2_role` r, `s2_permission` p
WHERE r.code = 'TENANT_ADMIN' AND r.tenant_id = 1 AND p.code IN ('TENANT_ADMIN', 'TENANT_ORG_MANAGE', 'TENANT_MEMBER_MANAGE', 'TENANT_ROLE_MANAGE', 'TENANT_PERMISSION', 'TENANT_SETTINGS', 'TENANT_USAGE_VIEW')
ON DUPLICATE KEY UPDATE `role_id`=`role_id`;

-- ========================================
-- 8: 为现有 admin 用户分配平台超级管理员角色
-- ========================================

INSERT INTO `s2_user_role` (`user_id`, `role_id`, `created_at`, `created_by`)
SELECT u.id, r.id, NOW(), 'system'
FROM `s2_user` u, `s2_role` r
WHERE u.name = 'admin' AND u.is_admin = 1 AND r.code = 'PLATFORM_SUPER_ADMIN'
ON DUPLICATE KEY UPDATE `role_id`=`role_id`;
