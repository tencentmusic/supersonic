-- Chat module schema (PostgreSQL)


-- ========================================
-- 8. 智能体与对话表
-- ========================================

-- 智能体表
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

COMMENT ON TABLE s2_agent IS '智能体表';


-- 对话表
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

COMMENT ON TABLE s2_chat IS '对话表';


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


-- 对话上下文表
CREATE TABLE IF NOT EXISTS s2_chat_context (
    chat_id BIGINT NOT NULL,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    query_user VARCHAR(64) DEFAULT NULL,
    query_text TEXT,
    semantic_parse TEXT,
    ext_data TEXT,
    PRIMARY KEY (chat_id)
);

COMMENT ON TABLE s2_chat_context IS '对话上下文表';


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


-- ========================================
-- 12. 插件与应用表
-- ========================================

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

COMMENT ON TABLE s2_plugin IS '插件表';
