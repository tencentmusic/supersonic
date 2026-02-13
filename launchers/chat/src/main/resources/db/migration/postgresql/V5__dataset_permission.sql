-- ========================================
-- 数据集权限迁移脚本 (PostgreSQL)
-- 版本: V5
-- 说明: 为数据集添加权限相关字段和权限组表
-- ========================================

-- ========================================
-- 1: 为 s2_data_set 表添加权限相关字段
-- ========================================

-- 添加 viewer 字段
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 's2_data_set' AND column_name = 'viewer') THEN
        ALTER TABLE s2_data_set ADD COLUMN viewer TEXT DEFAULT NULL;
        COMMENT ON COLUMN s2_data_set.viewer IS '可查看用户';
    END IF;
END $$;

-- 添加 view_org 字段
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 's2_data_set' AND column_name = 'view_org') THEN
        ALTER TABLE s2_data_set ADD COLUMN view_org TEXT DEFAULT NULL;
        COMMENT ON COLUMN s2_data_set.view_org IS '可查看组织';
    END IF;
END $$;

-- 添加 is_open 字段
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 's2_data_set' AND column_name = 'is_open') THEN
        ALTER TABLE s2_data_set ADD COLUMN is_open SMALLINT DEFAULT 0;
        COMMENT ON COLUMN s2_data_set.is_open IS '是否公开';
    END IF;
END $$;

-- ========================================
-- 2: 创建数据集权限组表
-- ========================================

CREATE TABLE IF NOT EXISTS s2_dataset_auth_groups (
    group_id BIGSERIAL NOT NULL,
    dataset_id BIGINT NOT NULL,
    name VARCHAR(255) DEFAULT NULL,
    auth_rules TEXT DEFAULT NULL,
    dimension_filters TEXT DEFAULT NULL,
    dimension_filter_description VARCHAR(500) DEFAULT NULL,
    authorized_users TEXT DEFAULT NULL,
    authorized_department_ids TEXT DEFAULT NULL,
    inherit_from_model SMALLINT DEFAULT 1,
    tenant_id BIGINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (group_id)
);

COMMENT ON TABLE s2_dataset_auth_groups IS '数据集权限组表';
COMMENT ON COLUMN s2_dataset_auth_groups.dataset_id IS '数据集ID';
COMMENT ON COLUMN s2_dataset_auth_groups.name IS '权限组名称';
COMMENT ON COLUMN s2_dataset_auth_groups.auth_rules IS '列权限JSON';
COMMENT ON COLUMN s2_dataset_auth_groups.dimension_filters IS '行权限JSON';
COMMENT ON COLUMN s2_dataset_auth_groups.dimension_filter_description IS '行权限描述';
COMMENT ON COLUMN s2_dataset_auth_groups.authorized_users IS '授权用户列表';
COMMENT ON COLUMN s2_dataset_auth_groups.authorized_department_ids IS '授权部门ID列表';
COMMENT ON COLUMN s2_dataset_auth_groups.inherit_from_model IS '是否继承Model权限';
COMMENT ON COLUMN s2_dataset_auth_groups.tenant_id IS '租户ID';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_dataset_auth_dataset ON s2_dataset_auth_groups(dataset_id);
CREATE INDEX IF NOT EXISTS idx_dataset_auth_tenant ON s2_dataset_auth_groups(tenant_id);
