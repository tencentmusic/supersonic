-- ========================================
-- 语义模板管理迁移脚本 (PostgreSQL)
-- 版本: V6
-- 说明: 添加语义模板定义表和部署记录表
-- ========================================

-- ========================================
-- 1: 语义模板定义表
-- ========================================

CREATE TABLE IF NOT EXISTS s2_semantic_template (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    biz_name VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    category VARCHAR(50) NOT NULL,
    template_config TEXT NOT NULL,
    preview_image VARCHAR(500) DEFAULT NULL,
    status SMALLINT DEFAULT 0,
    is_builtin SMALLINT DEFAULT 0,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) DEFAULT NULL,
    CONSTRAINT uk_semantic_template_tenant_biz UNIQUE (tenant_id, biz_name)
);

COMMENT ON TABLE s2_semantic_template IS '语义模板定义表';
COMMENT ON COLUMN s2_semantic_template.name IS '模板名称';
COMMENT ON COLUMN s2_semantic_template.biz_name IS '模板代码';
COMMENT ON COLUMN s2_semantic_template.description IS '模板描述';
COMMENT ON COLUMN s2_semantic_template.category IS '模板类别: VISITS/SINGER/COMPANY/ECOMMERCE';
COMMENT ON COLUMN s2_semantic_template.template_config IS 'JSON: 模板配置';
COMMENT ON COLUMN s2_semantic_template.preview_image IS '预览图URL';
COMMENT ON COLUMN s2_semantic_template.status IS '状态: 0-草稿 1-已部署';
COMMENT ON COLUMN s2_semantic_template.is_builtin IS '是否内置模板: 0-租户自定义 1-系统内置';
COMMENT ON COLUMN s2_semantic_template.tenant_id IS '租户ID: 1表示系统级(内置模板)';

CREATE INDEX IF NOT EXISTS idx_semantic_template_tenant ON s2_semantic_template(tenant_id);
CREATE INDEX IF NOT EXISTS idx_semantic_template_builtin ON s2_semantic_template(is_builtin);

-- ========================================
-- 2: 语义模板部署记录表
-- ========================================

CREATE TABLE IF NOT EXISTS s2_semantic_deployment (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,
    template_name VARCHAR(100) DEFAULT NULL,
    database_id BIGINT DEFAULT NULL,
    param_config TEXT DEFAULT NULL,
    status VARCHAR(20) NOT NULL,
    result_detail TEXT DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT NULL
);

COMMENT ON TABLE s2_semantic_deployment IS '语义模板部署记录表';
COMMENT ON COLUMN s2_semantic_deployment.template_id IS '模板ID';
COMMENT ON COLUMN s2_semantic_deployment.template_name IS '模板名称快照';
COMMENT ON COLUMN s2_semantic_deployment.database_id IS '目标数据库ID';
COMMENT ON COLUMN s2_semantic_deployment.param_config IS 'JSON: 用户自定义参数';
COMMENT ON COLUMN s2_semantic_deployment.status IS '状态: PENDING/RUNNING/SUCCESS/FAILED';
COMMENT ON COLUMN s2_semantic_deployment.result_detail IS 'JSON: 创建的对象详情';
COMMENT ON COLUMN s2_semantic_deployment.error_message IS '错误信息';
COMMENT ON COLUMN s2_semantic_deployment.start_time IS '开始时间';
COMMENT ON COLUMN s2_semantic_deployment.end_time IS '结束时间';
COMMENT ON COLUMN s2_semantic_deployment.tenant_id IS '租户ID';

CREATE INDEX IF NOT EXISTS idx_semantic_deployment_template ON s2_semantic_deployment(template_id);
CREATE INDEX IF NOT EXISTS idx_semantic_deployment_tenant ON s2_semantic_deployment(tenant_id);
CREATE INDEX IF NOT EXISTS idx_semantic_deployment_status ON s2_semantic_deployment(status);
