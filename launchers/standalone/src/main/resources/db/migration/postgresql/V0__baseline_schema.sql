-- ========================================
-- SuperSonic 基线数据库迁移脚本 (PostgreSQL)
-- 版本: V0
-- 说明: 创建核心业务表
-- ========================================

-- ========================================
-- 数据库实例表
-- ========================================

CREATE TABLE IF NOT EXISTS s2_database (
    id BIGSERIAL NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    version VARCHAR(64) DEFAULT NULL,
    type VARCHAR(20) NOT NULL,
    config TEXT NOT NULL,
    pool_config TEXT DEFAULT NULL,
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
COMMENT ON COLUMN s2_database.name IS '数据库名称';
COMMENT ON COLUMN s2_database.type IS '数据库类型：mysql,clickhouse,postgresql等';
COMMENT ON COLUMN s2_database.config IS '连接配置JSON';
COMMENT ON COLUMN s2_database.pool_config IS 'JSON configuration for connection pool settings';
CREATE INDEX IF NOT EXISTS idx_database_tenant ON s2_database(tenant_id);

-- ========================================
-- 主题域与模型表
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
COMMENT ON COLUMN s2_domain.name IS '主题域名称';
COMMENT ON COLUMN s2_domain.biz_name IS '业务名称';
COMMENT ON COLUMN s2_domain.parent_id IS '父主题域ID';
COMMENT ON COLUMN s2_domain.admin IS '管理员';
COMMENT ON COLUMN s2_domain.is_open IS '是否公开';
COMMENT ON COLUMN s2_domain.entity IS '实体信息';
CREATE INDEX IF NOT EXISTS idx_domain_tenant ON s2_domain(tenant_id);
CREATE INDEX IF NOT EXISTS idx_domain_parent ON s2_domain(parent_id);

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
COMMENT ON TABLE s2_model IS '模型信息表';
COMMENT ON COLUMN s2_model.name IS '模型名称';
COMMENT ON COLUMN s2_model.biz_name IS '业务名称';
COMMENT ON COLUMN s2_model.domain_id IS '主题域ID';
COMMENT ON COLUMN s2_model.database_id IS '数据库ID';
COMMENT ON COLUMN s2_model.model_detail IS '模型详情JSON';
CREATE INDEX IF NOT EXISTS idx_model_tenant ON s2_model(tenant_id);
CREATE INDEX IF NOT EXISTS idx_model_domain ON s2_model(domain_id);
CREATE INDEX IF NOT EXISTS idx_model_database ON s2_model(database_id);

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
COMMENT ON TABLE s2_model_rela IS '模型关联表';
COMMENT ON COLUMN s2_model_rela.join_type IS '关联类型';
COMMENT ON COLUMN s2_model_rela.join_condition IS '关联条件';
CREATE INDEX IF NOT EXISTS idx_model_rela_domain ON s2_model_rela(domain_id);

-- ========================================
-- 维度与指标表
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
    type_params TEXT DEFAULT NULL,
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
COMMENT ON TABLE s2_dimension IS '维度信息表';
COMMENT ON COLUMN s2_dimension.name IS '维度名称';
COMMENT ON COLUMN s2_dimension.biz_name IS '业务名称';
COMMENT ON COLUMN s2_dimension.type IS '维度类型：categorical,time';
COMMENT ON COLUMN s2_dimension.data_type IS '数据类型：varchar,array';
COMMENT ON COLUMN s2_dimension.expr IS '表达式';
COMMENT ON COLUMN s2_dimension.semantic_type IS '语义类型：DATE,ID,CATEGORY';
CREATE INDEX IF NOT EXISTS idx_dimension_model ON s2_dimension(model_id);

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
COMMENT ON TABLE s2_metric IS '指标信息表';
COMMENT ON COLUMN s2_metric.name IS '指标名称';
COMMENT ON COLUMN s2_metric.biz_name IS '业务名称';
COMMENT ON COLUMN s2_metric.status IS '状态：0=正常，1=下架';
COMMENT ON COLUMN s2_metric.sensitive_level IS '敏感级别';
COMMENT ON COLUMN s2_metric.type IS '指标类型';
COMMENT ON COLUMN s2_metric.classifications IS '分类';
COMMENT ON COLUMN s2_metric.relate_dimensions IS '关联维度';
COMMENT ON COLUMN s2_metric.is_publish IS '是否发布';
CREATE INDEX IF NOT EXISTS idx_metric_model ON s2_metric(model_id);

-- ========================================
-- 数据集表
-- ========================================

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
    created_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    PRIMARY KEY (id)
);
COMMENT ON TABLE s2_data_set IS '数据集表';
COMMENT ON COLUMN s2_data_set.name IS '数据集名称';
COMMENT ON COLUMN s2_data_set.data_set_detail IS '数据集详情JSON';
COMMENT ON COLUMN s2_data_set.query_config IS '查询配置';
CREATE INDEX IF NOT EXISTS idx_data_set_tenant ON s2_data_set(tenant_id);
CREATE INDEX IF NOT EXISTS idx_data_set_domain ON s2_data_set(domain_id);

-- ========================================
-- 智能体与插件表
-- ========================================

-- 智能助理表
CREATE TABLE IF NOT EXISTS s2_agent (
    id BIGSERIAL NOT NULL,
    name VARCHAR(100) DEFAULT NULL,
    description TEXT DEFAULT NULL,
    examples TEXT DEFAULT NULL,
    status SMALLINT DEFAULT NULL,
    model VARCHAR(100) DEFAULT NULL,
    tool_config TEXT DEFAULT NULL,
    llm_config TEXT DEFAULT NULL,
    chat_model_config TEXT DEFAULT NULL,
    visual_config TEXT DEFAULT NULL,
    enable_search SMALLINT DEFAULT 1,
    enable_feedback SMALLINT DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    admin VARCHAR(1000) DEFAULT NULL,
    admin_org VARCHAR(1000) DEFAULT NULL,
    is_open SMALLINT DEFAULT NULL,
    viewer VARCHAR(1000) DEFAULT NULL,
    view_org VARCHAR(1000) DEFAULT NULL,
    created_by VARCHAR(100) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT NULL,
    updated_by VARCHAR(100) DEFAULT NULL,
    updated_at TIMESTAMP DEFAULT NULL,
    PRIMARY KEY (id)
);
COMMENT ON TABLE s2_agent IS '智能助理表';
COMMENT ON COLUMN s2_agent.name IS '助理名称';
COMMENT ON COLUMN s2_agent.description IS '助理描述';
COMMENT ON COLUMN s2_agent.status IS '状态：0=下线，1=上线';
COMMENT ON COLUMN s2_agent.model IS 'LLM模型名称';
COMMENT ON COLUMN s2_agent.tool_config IS '工具配置JSON';
COMMENT ON COLUMN s2_agent.llm_config IS 'LLM配置JSON';
COMMENT ON COLUMN s2_agent.enable_search IS '启用搜索：0=否，1=是';
COMMENT ON COLUMN s2_agent.enable_feedback IS '启用反馈：0=否，1=是';
CREATE INDEX IF NOT EXISTS idx_agent_tenant ON s2_agent(tenant_id);

-- 插件表
CREATE TABLE IF NOT EXISTS s2_plugin (
    id BIGSERIAL NOT NULL,
    type VARCHAR(50) DEFAULT NULL,
    data_set VARCHAR(100) DEFAULT NULL,
    pattern VARCHAR(500) DEFAULT NULL,
    parse_mode VARCHAR(100) DEFAULT NULL,
    parse_mode_config TEXT,
    name VARCHAR(100) DEFAULT NULL,
    config TEXT,
    comment TEXT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT NULL,
    created_by VARCHAR(100) DEFAULT NULL,
    updated_at TIMESTAMP DEFAULT NULL,
    updated_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id)
);
COMMENT ON TABLE s2_plugin IS '插件配置表';
COMMENT ON COLUMN s2_plugin.type IS '插件类型：DASHBOARD,WIDGET,URL';
COMMENT ON COLUMN s2_plugin.pattern IS '匹配模式';
COMMENT ON COLUMN s2_plugin.parse_mode IS '解析模式';
CREATE INDEX IF NOT EXISTS idx_plugin_tenant ON s2_plugin(tenant_id);

-- ========================================
-- 对话模块表
-- ========================================

-- 对话会话表
CREATE TABLE IF NOT EXISTS s2_chat (
    chat_id BIGSERIAL NOT NULL,
    agent_id BIGINT DEFAULT NULL,
    chat_name VARCHAR(300) DEFAULT NULL,
    create_time TIMESTAMP DEFAULT NULL,
    last_time TIMESTAMP DEFAULT NULL,
    creator VARCHAR(30) DEFAULT NULL,
    last_question VARCHAR(200) DEFAULT NULL,
    is_delete SMALLINT DEFAULT 0,
    is_top SMALLINT DEFAULT 0,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (chat_id)
);
COMMENT ON TABLE s2_chat IS '对话会话表';
COMMENT ON COLUMN s2_chat.chat_id IS '对话ID';
COMMENT ON COLUMN s2_chat.agent_id IS '助理ID';
COMMENT ON COLUMN s2_chat.chat_name IS '对话名称';
COMMENT ON COLUMN s2_chat.is_delete IS '是否删除：0=否，1=是';
COMMENT ON COLUMN s2_chat.is_top IS '是否置顶：0=否，1=是';
COMMENT ON COLUMN s2_chat.tenant_id IS '租户ID';
CREATE INDEX IF NOT EXISTS idx_chat_tenant ON s2_chat(tenant_id);
CREATE INDEX IF NOT EXISTS idx_chat_agent ON s2_chat(agent_id);

-- 对话上下文表
CREATE TABLE IF NOT EXISTS s2_chat_context (
    chat_id BIGINT NOT NULL,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    query_user VARCHAR(64) DEFAULT NULL,
    query_text TEXT DEFAULT NULL,
    semantic_parse TEXT DEFAULT NULL,
    ext_data TEXT DEFAULT NULL,
    PRIMARY KEY (chat_id)
);
COMMENT ON TABLE s2_chat_context IS '对话上下文表';
COMMENT ON COLUMN s2_chat_context.chat_id IS '对话ID';
COMMENT ON COLUMN s2_chat_context.query_text IS '查询文本';
COMMENT ON COLUMN s2_chat_context.semantic_parse IS '语义解析数据';

-- 对话查询表
CREATE TABLE IF NOT EXISTS s2_chat_query (
    question_id BIGSERIAL NOT NULL,
    agent_id BIGINT DEFAULT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    query_text TEXT,
    user_name VARCHAR(150) DEFAULT NULL,
    query_state INTEGER DEFAULT NULL,
    chat_id BIGINT NOT NULL,
    query_result TEXT,
    score INTEGER DEFAULT 0,
    feedback VARCHAR(1024) DEFAULT '',
    similar_queries VARCHAR(1024) DEFAULT '',
    parse_time_cost VARCHAR(1024) DEFAULT '',
    PRIMARY KEY (question_id)
);
COMMENT ON TABLE s2_chat_query IS '对话查询表';
COMMENT ON COLUMN s2_chat_query.question_id IS '问题ID';
COMMENT ON COLUMN s2_chat_query.query_text IS '查询文本';
COMMENT ON COLUMN s2_chat_query.query_result IS '查询结果';
COMMENT ON COLUMN s2_chat_query.score IS '评分';
COMMENT ON COLUMN s2_chat_query.feedback IS '反馈';
CREATE INDEX IF NOT EXISTS idx_chat_query_agent ON s2_chat_query(agent_id);
CREATE INDEX IF NOT EXISTS idx_chat_query_chat ON s2_chat_query(chat_id);

-- 对话解析表
CREATE TABLE IF NOT EXISTS s2_chat_parse (
    id BIGSERIAL NOT NULL,
    question_id BIGINT NOT NULL,
    chat_id BIGINT NOT NULL,
    parse_id BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    query_text VARCHAR(500) DEFAULT NULL,
    user_name VARCHAR(150) DEFAULT NULL,
    parse_info TEXT NOT NULL,
    is_candidate INTEGER DEFAULT 1,
    PRIMARY KEY (id)
);
COMMENT ON TABLE s2_chat_parse IS '对话解析表';
COMMENT ON COLUMN s2_chat_parse.is_candidate IS '1=候选，0=已选';
CREATE INDEX IF NOT EXISTS idx_chat_parse_question ON s2_chat_parse(question_id);

-- 对话统计表
CREATE TABLE IF NOT EXISTS s2_chat_statistics (
    id BIGSERIAL NOT NULL,
    question_id BIGINT NOT NULL,
    chat_id BIGINT NOT NULL,
    user_name VARCHAR(150) DEFAULT NULL,
    query_text VARCHAR(200) DEFAULT NULL,
    interface_name VARCHAR(100) DEFAULT NULL,
    cost INTEGER DEFAULT 0,
    type INTEGER DEFAULT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
);
COMMENT ON TABLE s2_chat_statistics IS '对话统计表';
COMMENT ON COLUMN s2_chat_statistics.cost IS '耗时(毫秒)';
CREATE INDEX IF NOT EXISTS idx_chat_statistics_question ON s2_chat_statistics(question_id);

-- 对话配置表
CREATE TABLE IF NOT EXISTS s2_chat_config (
    id BIGSERIAL NOT NULL,
    model_id BIGINT DEFAULT NULL,
    chat_detail_config TEXT,
    chat_agg_config TEXT,
    recommended_questions TEXT,
    llm_examples TEXT,
    status SMALLINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);
COMMENT ON TABLE s2_chat_config IS '对话配置表';
COMMENT ON COLUMN s2_chat_config.chat_detail_config IS '明细模式配置';
COMMENT ON COLUMN s2_chat_config.chat_agg_config IS '指标模式配置';
COMMENT ON COLUMN s2_chat_config.recommended_questions IS '推荐问题配置';
CREATE INDEX IF NOT EXISTS idx_chat_config_model ON s2_chat_config(model_id);

-- 对话记忆表
CREATE TABLE IF NOT EXISTS s2_chat_memory (
    id BIGSERIAL NOT NULL,
    question VARCHAR(655),
    side_info TEXT,
    query_id BIGINT,
    agent_id BIGINT,
    db_schema TEXT,
    s2_sql TEXT,
    status VARCHAR(10),
    llm_review VARCHAR(10),
    llm_comment TEXT,
    human_review VARCHAR(10),
    human_comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT NULL,
    updated_by VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id)
);
COMMENT ON TABLE s2_chat_memory IS '对话记忆表';
COMMENT ON COLUMN s2_chat_memory.question IS '用户问题';
COMMENT ON COLUMN s2_chat_memory.s2_sql IS '大模型解析SQL';
COMMENT ON COLUMN s2_chat_memory.llm_review IS '大模型评估结果';
COMMENT ON COLUMN s2_chat_memory.human_review IS '管理员评估结果';
CREATE INDEX IF NOT EXISTS idx_chat_memory_agent ON s2_chat_memory(agent_id);

-- 对话大模型实例表
CREATE TABLE IF NOT EXISTS s2_chat_model (
    id BIGSERIAL NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
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
COMMENT ON TABLE s2_chat_model IS '对话大模型实例表';
COMMENT ON COLUMN s2_chat_model.config IS '配置信息JSON';
COMMENT ON COLUMN s2_chat_model.is_open IS '是否公开';
CREATE INDEX IF NOT EXISTS idx_chat_model_tenant ON s2_chat_model(tenant_id);

-- ========================================
-- 标签与分类表
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
CREATE INDEX IF NOT EXISTS idx_tag_object_domain ON s2_tag_object(domain_id);

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
COMMENT ON TABLE s2_tag IS '标签信息表';
CREATE INDEX IF NOT EXISTS idx_tag_item ON s2_tag(item_id, type);

-- ========================================
-- 术语与规则表
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
COMMENT ON COLUMN s2_term.alias IS '别名';
COMMENT ON COLUMN s2_term.related_metrics IS '关联指标';
COMMENT ON COLUMN s2_term.related_dimensions IS '关联维度';
CREATE INDEX IF NOT EXISTS idx_term_domain ON s2_term(domain_id);

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
CREATE INDEX IF NOT EXISTS idx_query_rule_data_set ON s2_query_rule(data_set_id);

-- ========================================
-- 字典与配置表
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
CREATE INDEX IF NOT EXISTS idx_dictionary_conf_item ON s2_dictionary_conf(item_id, type);

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
CREATE INDEX IF NOT EXISTS idx_dictionary_task_item ON s2_dictionary_task(item_id, type);

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
-- 应用与统计表
-- ========================================

-- 应用表
CREATE TABLE IF NOT EXISTS s2_app (
    id BIGSERIAL NOT NULL,
    name VARCHAR(255),
    description VARCHAR(255),
    status INTEGER,
    config TEXT,
    end_date TIMESTAMP,
    qps INTEGER,
    app_secret VARCHAR(255),
    owner VARCHAR(255),
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    PRIMARY KEY (id)
);
COMMENT ON TABLE s2_app IS '应用表';
COMMENT ON COLUMN s2_app.app_secret IS '应用密钥';
COMMENT ON COLUMN s2_app.qps IS 'QPS限制';
CREATE INDEX IF NOT EXISTS idx_app_tenant ON s2_app(tenant_id);

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
COMMENT ON COLUMN s2_query_stat_info.trace_id IS '查询标识';
COMMENT ON COLUMN s2_query_stat_info.query_user IS '执行用户';
COMMENT ON COLUMN s2_query_stat_info.elapsed_ms IS '查询耗时(毫秒)';
CREATE INDEX IF NOT EXISTS idx_query_stat_model ON s2_query_stat_info(model_id);
CREATE INDEX IF NOT EXISTS idx_query_stat_data_set ON s2_query_stat_info(data_set_id);
CREATE INDEX IF NOT EXISTS idx_query_stat_tenant ON s2_query_stat_info(tenant_id);

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
CREATE INDEX IF NOT EXISTS idx_metric_query_config_metric ON s2_metric_query_default_config(metric_id);

-- ========================================
-- 其他辅助表
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
COMMENT ON COLUMN s2_canvas.type IS '类型：datasource,dimension,metric';
CREATE INDEX IF NOT EXISTS idx_canvas_domain ON s2_canvas(domain_id);

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
COMMENT ON TABLE s2_collect IS '用户收藏表';
CREATE INDEX IF NOT EXISTS idx_collect_user ON s2_collect(username);
CREATE INDEX IF NOT EXISTS idx_collect_tenant ON s2_collect(tenant_id);

-- 认证组表
CREATE TABLE IF NOT EXISTS s2_auth_groups (
    group_id BIGINT NOT NULL,
    config VARCHAR(2048) DEFAULT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (group_id)
);
COMMENT ON TABLE s2_auth_groups IS '权限组表';
CREATE INDEX IF NOT EXISTS idx_auth_groups_tenant ON s2_auth_groups(tenant_id);

-- 系统配置表
CREATE TABLE IF NOT EXISTS s2_system_config (
    id BIGSERIAL NOT NULL,
    admin VARCHAR(500),
    parameters TEXT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
);
COMMENT ON TABLE s2_system_config IS '系统配置表';
COMMENT ON COLUMN s2_system_config.admin IS '系统管理员';
COMMENT ON COLUMN s2_system_config.parameters IS '配置参数JSON';
CREATE INDEX IF NOT EXISTS idx_system_config_tenant ON s2_system_config(tenant_id);
