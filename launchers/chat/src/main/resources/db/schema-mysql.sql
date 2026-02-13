-- Chat module schema (MySQL)


-- ========================================
-- 8. 智能体与对话表
-- ========================================

-- 智能体表
CREATE TABLE IF NOT EXISTS `s2_agent` (
                                          `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '智能体名称',
    `description` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
    `examples` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '示例',
    `status` tinyint DEFAULT NULL COMMENT '状态',
    `model` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用模型',
    `tool_config` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工具配置',
    `llm_config` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'LLM配置',
    `chat_model_config` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '对话模型配置',
    `visual_config` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '可视化配置',
    `enable_search` tinyint DEFAULT 1 COMMENT '是否启用搜索',
    `enable_feedback` tinyint DEFAULT 1 COMMENT '是否启用反馈',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `admin` varchar(1000) DEFAULT NULL COMMENT '管理员',
    `admin_org` varchar(1000) DEFAULT NULL COMMENT '管理员组织',
    `is_open` tinyint DEFAULT NULL COMMENT '是否公开',
    `viewer` varchar(1000) DEFAULT NULL COMMENT '可用用户',
    `view_org` varchar(1000) DEFAULT NULL COMMENT '可用组织',
    `created_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `updated_at` datetime DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_agent_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体表';


-- 对话表
CREATE TABLE IF NOT EXISTS `s2_chat` (
                                         `chat_id` bigint(20) NOT NULL AUTO_INCREMENT,
    `agent_id` bigint(20) DEFAULT NULL COMMENT '智能体ID',
    `chat_name` varchar(300) DEFAULT NULL COMMENT '对话名称',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `last_time` datetime DEFAULT NULL COMMENT '最后更新时间',
    `creator` varchar(30) DEFAULT NULL COMMENT '创建者',
    `last_question` varchar(200) DEFAULT NULL COMMENT '最后问题',
    `is_delete` tinyint DEFAULT 0 COMMENT '是否删除',
    `is_top` tinyint DEFAULT 0 COMMENT '是否置顶',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`chat_id`),
    KEY `idx_chat_tenant` (`tenant_id`),
    KEY `idx_chat_agent` (`agent_id`),
    KEY `idx_chat_creator` (`creator`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话表';


-- 对话配置表
CREATE TABLE IF NOT EXISTS `s2_chat_config` (
                                                `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `model_id` bigint(20) DEFAULT NULL COMMENT '模型ID',
    `chat_detail_config` mediumtext COMMENT '明细模式配置信息',
    `chat_agg_config` mediumtext COMMENT '指标模式配置信息',
    `recommended_questions` mediumtext COMMENT '推荐问题配置',
    `llm_examples` text COMMENT 'LLM示例',
    `status` tinyint NOT NULL COMMENT '状态: 0=删除, 1=生效',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `updated_at` datetime NOT NULL COMMENT '更新时间',
    `created_by` varchar(100) NOT NULL COMMENT '创建人',
    `updated_by` varchar(100) NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_chat_config_model` (`model_id`),
    KEY `idx_chat_config_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话配置表';


-- 对话记忆表
CREATE TABLE IF NOT EXISTS `s2_chat_memory` (
                                                `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `question` varchar(655) COMMENT '用户问题',
    `side_info` text COMMENT '辅助信息',
    `query_id` bigint(20) COMMENT '问答ID',
    `agent_id` bigint(20) COMMENT '智能体ID',
    `db_schema` text COMMENT 'Schema映射',
    `s2_sql` text COMMENT '大模型解析SQL',
    `status` varchar(10) COMMENT '状态',
    `llm_review` varchar(10) COMMENT '大模型评估结果',
    `llm_comment` text COMMENT '大模型评估意见',
    `human_review` varchar(10) COMMENT '管理员评估结果',
    `human_comment` text COMMENT '管理员评估意见',
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_chat_memory_agent` (`agent_id`),
    KEY `idx_chat_memory_query` (`query_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话记忆表';


-- 对话上下文表
CREATE TABLE IF NOT EXISTS `s2_chat_context` (
                                                 `chat_id` bigint(20) NOT NULL COMMENT '对话ID',
    `modified_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `query_user` varchar(64) DEFAULT NULL COMMENT '查询用户',
    `query_text` text COMMENT '查询文本',
    `semantic_parse` text COMMENT '语义解析数据',
    `ext_data` text COMMENT '扩展数据',
    PRIMARY KEY (`chat_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话上下文表';


-- 对话解析表
CREATE TABLE IF NOT EXISTS `s2_chat_parse` (
                                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `question_id` bigint(20) NOT NULL COMMENT '问题ID',
    `chat_id` bigint(20) NOT NULL COMMENT '对话ID',
    `parse_id` bigint(20) NOT NULL COMMENT '解析ID',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `query_text` varchar(500) DEFAULT NULL COMMENT '查询文本',
    `user_name` varchar(150) DEFAULT NULL COMMENT '用户名',
    `parse_info` mediumtext NOT NULL COMMENT '解析信息',
    `is_candidate` int(11) DEFAULT 1 COMMENT '1=候选, 0=已选',
    PRIMARY KEY (`id`),
    KEY `idx_chat_parse_question` (`question_id`),
    KEY `idx_chat_parse_chat` (`chat_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话解析表';


-- 对话查询表
CREATE TABLE IF NOT EXISTS `s2_chat_query` (
                                               `question_id` bigint(20) NOT NULL AUTO_INCREMENT,
    `agent_id` bigint(20) DEFAULT NULL COMMENT '智能体ID',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `query_text` mediumtext COMMENT '查询文本',
    `user_name` varchar(150) DEFAULT NULL COMMENT '用户名',
    `query_state` int(1) DEFAULT NULL COMMENT '查询状态',
    `chat_id` bigint(20) NOT NULL COMMENT '对话ID',
    `query_result` mediumtext COMMENT '查询结果',
    `score` int(11) DEFAULT 0 COMMENT '评分',
    `feedback` varchar(1024) DEFAULT '' COMMENT '反馈',
    `similar_queries` varchar(1024) DEFAULT '' COMMENT '相似查询',
    `parse_time_cost` varchar(1024) DEFAULT '' COMMENT '解析耗时',
    PRIMARY KEY (`question_id`),
    KEY `idx_chat_query_agent` (`agent_id`),
    KEY `idx_chat_query_chat` (`chat_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话查询表';


-- 对话统计表
CREATE TABLE IF NOT EXISTS `s2_chat_statistics` (
                                                    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `question_id` bigint(20) NOT NULL COMMENT '问题ID',
    `chat_id` bigint(20) NOT NULL COMMENT '对话ID',
    `user_name` varchar(150) DEFAULT NULL COMMENT '用户名',
    `query_text` varchar(200) DEFAULT NULL COMMENT '查询文本',
    `interface_name` varchar(100) DEFAULT NULL COMMENT '接口名称',
    `cost` int(6) DEFAULT 0 COMMENT '耗时(ms)',
    `type` int(11) DEFAULT NULL COMMENT '类型',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_chat_statistics_question` (`question_id`),
    KEY `idx_chat_statistics_chat` (`chat_id`),
    KEY `idx_chat_statistics_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话统计表';


-- ========================================
-- 12. 插件与应用表
-- ========================================

-- 插件表
CREATE TABLE IF NOT EXISTS `s2_plugin` (
                                           `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `type` varchar(50) DEFAULT NULL COMMENT '类型: DASHBOARD, WIDGET, URL',
    `data_set` varchar(100) DEFAULT NULL COMMENT '数据集',
    `pattern` varchar(500) DEFAULT NULL COMMENT '模式',
    `parse_mode` varchar(100) DEFAULT NULL COMMENT '解析模式',
    `parse_mode_config` text COMMENT '解析模式配置',
    `name` varchar(100) DEFAULT NULL COMMENT '名称',
    `config` text COMMENT '配置',
    `comment` text COMMENT '备注',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT NULL,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_plugin_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='插件表';
