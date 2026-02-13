-- ========================================
-- 模板版本管理迁移脚本 (MySQL)
-- 版本: V19
-- 说明: 为模板添加版本号，为部署记录添加版本快照
-- ========================================

-- 模板表: 添加当前版本号
ALTER TABLE `s2_semantic_template`
    ADD COLUMN `current_version` bigint DEFAULT 1 COMMENT '当前版本号，每次编辑自增';

-- 部署记录表: 添加部署时的模板版本号和配置快照
ALTER TABLE `s2_semantic_deployment`
    ADD COLUMN `template_version` bigint DEFAULT NULL COMMENT '部署时的模板版本号',
    ADD COLUMN `template_config_snapshot` longtext DEFAULT NULL COMMENT 'JSON: 部署时的模板配置快照';
