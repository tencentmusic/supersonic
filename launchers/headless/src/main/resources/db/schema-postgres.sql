-- Headless module schema (PostgreSQL)


-- ========================================
-- 4. 核心业务表 - 数据源与数据库
-- ========================================

-- 数据库实例表
CREATE TABLE IF NOT EXISTS s2_database (
    id BIGSERIAL NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    version VARCHAR(64) DEFAULT NULL,
    type VARCHAR(20) NOT NULL,
    config TEXT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    admin VARCHAR(500) DEFAULT NULL,
    viewer VARCHAR(500) DEFAULT NULL,
    is_open SMALLINT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_database IS '数据库实例表';


-- ========================================
-- 5. 核心业务表 - 主题域与模型
-- ========================================

-- 主题域表
CREATE TABLE IF NOT EXISTS s2_domain (
    id BIGSERIAL NOT NULL,
    name VARCHAR(255) DEFAULT NULL,
    biz_name VARCHAR(255) DEFAULT NULL,
    parent_id BIGINT DEFAULT 0,
    status SMALLINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    admin VARCHAR(3000) DEFAULT NULL,
    admin_org VARCHAR(3000) DEFAULT NULL,
    is_open SMALLINT DEFAULT NULL,
    viewer VARCHAR(3000) DEFAULT NULL,
    view_org VARCHAR(3000) DEFAULT NULL,
    entity VARCHAR(500) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT NULL,
    created_by VARCHAR(100) DEFAULT NULL,
    updated_at TIMESTAMP DEFAULT NULL,
    updated_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_domain IS '主题域基础信息表';


-- 模型表
CREATE TABLE IF NOT EXISTS s2_model (
    id BIGSERIAL NOT NULL,
    name VARCHAR(100) DEFAULT NULL,
    biz_name VARCHAR(100) DEFAULT NULL,
    domain_id BIGINT DEFAULT NULL,
    alias VARCHAR(200) DEFAULT NULL,
    status SMALLINT DEFAULT NULL,
    description VARCHAR(500) DEFAULT NULL,
    database_id BIGINT NOT NULL,
    model_detail TEXT NOT NULL,
    source_type VARCHAR(128) DEFAULT NULL,
    depends VARCHAR(500) DEFAULT NULL,
    filter_sql VARCHAR(1000) DEFAULT NULL,
    drill_down_dimensions TEXT DEFAULT NULL,
    tag_object_id BIGINT DEFAULT 0,
    ext VARCHAR(1000) DEFAULT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    viewer VARCHAR(500) DEFAULT NULL,
    view_org VARCHAR(500) DEFAULT NULL,
    admin VARCHAR(500) DEFAULT NULL,
    admin_org VARCHAR(500) DEFAULT NULL,
    is_open SMALLINT DEFAULT NULL,
    created_by VARCHAR(100) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT NULL,
    updated_by VARCHAR(100) DEFAULT NULL,
    updated_at TIMESTAMP DEFAULT NULL,
    entity TEXT,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_model IS '模型表';


-- 模型关系表
CREATE TABLE IF NOT EXISTS s2_model_rela (
    id BIGSERIAL NOT NULL,
    domain_id BIGINT,
    from_model_id BIGINT NOT NULL,
    to_model_id BIGINT NOT NULL,
    join_type VARCHAR(255),
    join_condition TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_model_rela IS '模型关系表';


-- ========================================
-- 6. 核心业务表 - 维度与指标
-- ========================================

-- 维度表
CREATE TABLE IF NOT EXISTS s2_dimension (
    id BIGSERIAL NOT NULL,
    model_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    biz_name VARCHAR(255) NOT NULL,
    description VARCHAR(500) NOT NULL,
    status SMALLINT NOT NULL,
    sensitive_level INTEGER DEFAULT NULL,
    type VARCHAR(50) NOT NULL,
    type_params TEXT,
    data_type VARCHAR(50) DEFAULT NULL,
    expr TEXT NOT NULL,
    semantic_type VARCHAR(20) NOT NULL,
    alias VARCHAR(500) DEFAULT NULL,
    default_values VARCHAR(500) DEFAULT NULL,
    dim_value_maps VARCHAR(5000) DEFAULT NULL,
    is_tag SMALLINT DEFAULT NULL,
    ext VARCHAR(1000) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_dimension IS '维度表';


-- 指标表
CREATE TABLE IF NOT EXISTS s2_metric (
    id BIGSERIAL NOT NULL,
    model_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    biz_name VARCHAR(255) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    status SMALLINT NOT NULL,
    sensitive_level SMALLINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    type_params TEXT NOT NULL,
    data_format_type VARCHAR(50) DEFAULT NULL,
    data_format VARCHAR(500) DEFAULT NULL,
    alias VARCHAR(500) DEFAULT NULL,
    classifications VARCHAR(500) DEFAULT NULL,
    relate_dimensions VARCHAR(500) DEFAULT NULL,
    ext TEXT DEFAULT NULL,
    define_type VARCHAR(50) DEFAULT NULL,
    is_publish SMALLINT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_metric IS '指标表';


-- ========================================
-- 7. 核心业务表 - 数据集
-- ========================================

-- 数据集表
CREATE TABLE IF NOT EXISTS s2_data_set (
    id BIGSERIAL NOT NULL,
    domain_id BIGINT,
    name VARCHAR(255),
    biz_name VARCHAR(255),
    description VARCHAR(255),
    status INTEGER,
    alias VARCHAR(255),
    data_set_detail TEXT,
    query_config VARCHAR(3000),
    tenant_id BIGINT NOT NULL DEFAULT 1,
    admin VARCHAR(3000) DEFAULT NULL,
    admin_org VARCHAR(3000) DEFAULT NULL,
    viewer VARCHAR(3000) DEFAULT NULL,
    view_org VARCHAR(3000) DEFAULT NULL,
    is_open SMALLINT DEFAULT 0,
    created_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_data_set IS '数据集表';


-- 数据集权限组表
CREATE TABLE IF NOT EXISTS s2_dataset_auth_groups (
    group_id BIGSERIAL NOT NULL,
    dataset_id BIGINT NOT NULL,
    name VARCHAR(255),
    auth_rules TEXT,
    dimension_filters TEXT,
    dimension_filter_description VARCHAR(500),
    authorized_users TEXT,
    authorized_department_ids TEXT,
    inherit_from_model SMALLINT DEFAULT 1,
    tenant_id BIGINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    PRIMARY KEY (group_id)
);

COMMENT ON TABLE s2_dataset_auth_groups IS '数据集权限组表';


-- ========================================
-- 9. 标签与分类表
-- ========================================

-- 标签对象表
CREATE TABLE IF NOT EXISTS s2_tag_object (
    id BIGSERIAL NOT NULL,
    domain_id BIGINT DEFAULT NULL,
    name VARCHAR(255) NOT NULL,
    biz_name VARCHAR(255) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    sensitive_level SMALLINT NOT NULL DEFAULT 0,
    ext TEXT DEFAULT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_tag_object IS '标签对象表';


-- 标签表
CREATE TABLE IF NOT EXISTS s2_tag (
    id BIGSERIAL NOT NULL,
    item_id BIGINT NOT NULL,
    type VARCHAR(255) NOT NULL,
    ext TEXT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP DEFAULT NULL,
    updated_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_tag IS '标签表';


-- ========================================
-- 10. 术语与规则表
-- ========================================

-- 术语表
CREATE TABLE IF NOT EXISTS s2_term (
    id BIGSERIAL NOT NULL,
    domain_id BIGINT,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    alias VARCHAR(1000) NOT NULL,
    related_metrics VARCHAR(1000) DEFAULT NULL,
    related_dimensions VARCHAR(1000) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP DEFAULT NULL,
    updated_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_term IS '术语表';


-- 查询规则表
CREATE TABLE IF NOT EXISTS s2_query_rule (
    id BIGSERIAL NOT NULL,
    data_set_id BIGINT,
    priority INTEGER NOT NULL DEFAULT 1,
    rule_type VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    biz_name VARCHAR(255) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    rule TEXT DEFAULT NULL,
    action TEXT DEFAULT NULL,
    status INTEGER NOT NULL DEFAULT 1,
    ext TEXT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP DEFAULT NULL,
    updated_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_query_rule IS '查询规则表';


-- ========================================
-- 11. 字典与配置表
-- ========================================

-- 字典配置表
CREATE TABLE IF NOT EXISTS s2_dictionary_conf (
    id BIGSERIAL NOT NULL,
    description VARCHAR(255),
    type VARCHAR(255) NOT NULL,
    item_id BIGINT NOT NULL,
    config TEXT,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_dictionary_conf IS '字典配置信息表';


-- 字典任务表
CREATE TABLE IF NOT EXISTS s2_dictionary_task (
    id BIGSERIAL NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    type VARCHAR(255) NOT NULL,
    item_id BIGINT NOT NULL,
    config TEXT,
    status VARCHAR(255) NOT NULL,
    elapsed_ms INTEGER DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_dictionary_task IS '字典运行任务表';


-- 可用日期信息表
CREATE TABLE IF NOT EXISTS s2_available_date_info (
    id BIGSERIAL NOT NULL,
    item_id BIGINT NOT NULL,
    type VARCHAR(255) NOT NULL,
    date_format VARCHAR(64) NOT NULL,
    date_period VARCHAR(64) DEFAULT NULL,
    start_date VARCHAR(64) DEFAULT NULL,
    end_date VARCHAR(64) DEFAULT NULL,
    unavailable_date TEXT,
    status SMALLINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_item_type UNIQUE (item_id, type)
);

COMMENT ON TABLE s2_available_date_info IS '可用日期信息表';


-- ========================================
-- 13. 统计与分析表
-- ========================================

-- 查询统计信息表
CREATE TABLE IF NOT EXISTS s2_query_stat_info (
    id BIGSERIAL NOT NULL,
    trace_id VARCHAR(200) DEFAULT NULL,
    model_id BIGINT DEFAULT NULL,
    data_set_id BIGINT DEFAULT NULL,
    query_user VARCHAR(200) DEFAULT NULL,
    query_type VARCHAR(200) DEFAULT NULL,
    query_type_back INTEGER DEFAULT 0,
    query_sql_cmd TEXT,
    sql_cmd_md5 VARCHAR(200) DEFAULT NULL,
    query_struct_cmd TEXT,
    struct_cmd_md5 VARCHAR(200) DEFAULT NULL,
    query_sql TEXT,
    sql_md5 VARCHAR(200) DEFAULT NULL,
    query_engine VARCHAR(20) DEFAULT NULL,
    elapsed_ms BIGINT DEFAULT NULL,
    query_state VARCHAR(20) DEFAULT NULL,
    native_query INTEGER DEFAULT NULL,
    start_date VARCHAR(50) DEFAULT NULL,
    end_date VARCHAR(50) DEFAULT NULL,
    dimensions TEXT,
    metrics TEXT,
    select_cols TEXT,
    agg_cols TEXT,
    filter_cols TEXT,
    group_by_cols TEXT,
    order_by_cols TEXT,
    use_result_cache SMALLINT DEFAULT -1,
    use_sql_cache SMALLINT DEFAULT -1,
    sql_cache_key TEXT,
    result_cache_key TEXT,
    query_opt_mode VARCHAR(20) DEFAULT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_query_stat_info IS '查询统计信息表';


-- ========================================
-- 14. 其他辅助表
-- ========================================

-- 画布表
CREATE TABLE IF NOT EXISTS s2_canvas (
    id BIGSERIAL NOT NULL,
    domain_id BIGINT DEFAULT NULL,
    type VARCHAR(20) DEFAULT NULL,
    config TEXT,
    created_at TIMESTAMP DEFAULT NULL,
    created_by VARCHAR(100) DEFAULT NULL,
    updated_at TIMESTAMP DEFAULT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_canvas IS '画布表';


-- 收藏表
CREATE TABLE IF NOT EXISTS s2_collect (
    id BIGSERIAL NOT NULL,
    type VARCHAR(20) NOT NULL,
    username VARCHAR(20) NOT NULL,
    collect_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_collect IS '收藏表';


-- 指标查询默认配置表
CREATE TABLE IF NOT EXISTS s2_metric_query_default_config (
    id BIGSERIAL NOT NULL,
    metric_id BIGINT,
    user_name VARCHAR(255) NOT NULL,
    default_config VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE s2_metric_query_default_config IS '指标查询默认配置表';


-- ========================================
-- 15. 语义模板管理表
-- ========================================

-- 语义模板定义表
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


-- 语义模板部署记录表
CREATE TABLE IF NOT EXISTS s2_semantic_deployment (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,
    template_name VARCHAR(100) DEFAULT NULL,
    database_id BIGINT DEFAULT NULL,
    param_config TEXT DEFAULT NULL,
    status VARCHAR(20) NOT NULL,
    result_detail TEXT DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    current_step VARCHAR(50) DEFAULT NULL,
    start_time TIMESTAMP DEFAULT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT NULL,
    active_lock VARCHAR(100) DEFAULT NULL,
    CONSTRAINT uk_deployment_active_lock UNIQUE (active_lock)
);

COMMENT ON TABLE s2_semantic_deployment IS '语义模板部署记录表';
