-- ========================================
-- 模板版本管理迁移脚本 (PostgreSQL)
-- 版本: V19
-- 说明: 为模板添加版本号，为部署记录添加版本快照
-- ========================================

-- 模板表: 添加当前版本号
ALTER TABLE s2_semantic_template
    ADD COLUMN IF NOT EXISTS current_version BIGINT DEFAULT 1;

COMMENT ON COLUMN s2_semantic_template.current_version IS '当前版本号，每次编辑自增';

-- 部署记录表: 添加部署时的模板版本号和配置快照
ALTER TABLE s2_semantic_deployment
    ADD COLUMN IF NOT EXISTS template_version BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS template_config_snapshot TEXT DEFAULT NULL;

COMMENT ON COLUMN s2_semantic_deployment.template_version IS '部署时的模板版本号';
COMMENT ON COLUMN s2_semantic_deployment.template_config_snapshot IS 'JSON: 部署时的模板配置快照';
