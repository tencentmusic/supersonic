-- ========================================
-- 语义模板管理迁移脚本 (MySQL)
-- 版本: V6
-- 说明: 添加语义模板定义表和部署记录表
-- ========================================

-- ========================================
-- 1: 语义模板定义表
-- ========================================

CREATE TABLE IF NOT EXISTS `s2_semantic_template` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(100) NOT NULL COMMENT '模板名称',
    `biz_name` varchar(100) NOT NULL COMMENT '模板代码',
    `description` varchar(500) DEFAULT NULL COMMENT '模板描述',
    `category` varchar(50) NOT NULL COMMENT '模板类别: VISITS/SINGER/COMPANY/ECOMMERCE',
    `template_config` longtext NOT NULL COMMENT 'JSON: 模板配置',
    `preview_image` varchar(500) DEFAULT NULL COMMENT '预览图URL',
    `status` tinyint DEFAULT 0 COMMENT '状态: 0-草稿 1-已部署',
    `is_builtin` tinyint DEFAULT 0 COMMENT '是否内置模板: 0-租户自定义 1-系统内置',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID: 1表示系统级(内置模板)',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_semantic_template_tenant_biz` (`tenant_id`, `biz_name`),
    KEY `idx_semantic_template_tenant` (`tenant_id`),
    KEY `idx_semantic_template_builtin` (`is_builtin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语义模板定义表';

-- ========================================
-- 2: 语义模板部署记录表
-- ========================================

CREATE TABLE IF NOT EXISTS `s2_semantic_deployment` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `template_id` bigint NOT NULL COMMENT '模板ID',
    `template_name` varchar(100) DEFAULT NULL COMMENT '模板名称快照',
    `database_id` bigint DEFAULT NULL COMMENT '目标数据库ID',
    `param_config` text DEFAULT NULL COMMENT 'JSON: 用户自定义参数',
    `status` varchar(20) NOT NULL COMMENT '状态: PENDING/RUNNING/SUCCESS/FAILED',
    `result_detail` longtext DEFAULT NULL COMMENT 'JSON: 创建的对象详情',
    `error_message` text DEFAULT NULL COMMENT '错误信息',
    `start_time` datetime DEFAULT NULL COMMENT '开始时间',
    `end_time` datetime DEFAULT NULL COMMENT '结束时间',
    `tenant_id` bigint NOT NULL COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_semantic_deployment_template` (`template_id`),
    KEY `idx_semantic_deployment_tenant` (`tenant_id`),
    KEY `idx_semantic_deployment_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语义模板部署记录表';
