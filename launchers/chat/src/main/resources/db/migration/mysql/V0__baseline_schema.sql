-- ========================================
-- SuperSonic 基线数据库迁移脚本 (MySQL)
-- 版本: V0
-- 说明: 创建核心业务表
-- ========================================

-- ========================================
-- 1. 数据源与数据库表
-- ========================================

-- 数据库实例表
CREATE TABLE IF NOT EXISTS `s2_database` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL COMMENT '名称',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `version` varchar(64) DEFAULT NULL COMMENT '版本',
    `type` varchar(20) NOT NULL COMMENT '类型: mysql, clickhouse, tdw等',
    `config` text NOT NULL COMMENT '配置信息(JSON)',
    `pool_config` text DEFAULT NULL COMMENT 'JSON configuration for connection pool settings per pool type',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `admin` varchar(500) DEFAULT NULL COMMENT '管理员',
    `viewer` varchar(500) DEFAULT NULL COMMENT '查看者',
    `is_open` tinyint DEFAULT NULL COMMENT '是否公开',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL COMMENT '创建人',
    `updated_at` datetime NOT NULL COMMENT '更新时间',
    `updated_by` varchar(100) NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_database_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据库实例表';

-- ========================================
-- 2. 主题域与模型表
-- ========================================

-- 主题域表
CREATE TABLE IF NOT EXISTS `s2_domain` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `name` varchar(255) DEFAULT NULL COMMENT '主题域名称',
    `biz_name` varchar(255) DEFAULT NULL COMMENT '业务名称',
    `parent_id` bigint DEFAULT 0 COMMENT '父主题域ID',
    `status` tinyint NOT NULL COMMENT '主题域状态',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `admin` varchar(3000) DEFAULT NULL COMMENT '主题域管理员',
    `admin_org` varchar(3000) DEFAULT NULL COMMENT '主题域管理员组织',
    `is_open` tinyint DEFAULT NULL COMMENT '主题域是否公开',
    `viewer` varchar(3000) DEFAULT NULL COMMENT '主题域可用用户',
    `view_org` varchar(3000) DEFAULT NULL COMMENT '主题域可用组织',
    `entity` varchar(500) DEFAULT NULL COMMENT '主题域实体信息',
    `created_at` datetime DEFAULT NULL COMMENT '创建时间',
    `created_by` varchar(100) DEFAULT NULL COMMENT '创建人',
    `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
    `updated_by` varchar(100) DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_domain_tenant` (`tenant_id`),
    KEY `idx_domain_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主题域基础信息表';

-- 模型表
CREATE TABLE IF NOT EXISTS `s2_model` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模型名称',
    `biz_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务名称',
    `domain_id` bigint DEFAULT NULL COMMENT '主题域ID',
    `alias` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '别名',
    `status` tinyint DEFAULT NULL COMMENT '状态',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `database_id` bigint NOT NULL COMMENT '数据库ID',
    `model_detail` text NOT NULL COMMENT '模型详情(JSON)',
    `source_type` varchar(128) DEFAULT NULL COMMENT '来源类型',
    `depends` varchar(500) DEFAULT NULL COMMENT '依赖',
    `filter_sql` varchar(1000) DEFAULT NULL COMMENT '过滤SQL',
    `drill_down_dimensions` text DEFAULT NULL COMMENT '下钻维度',
    `tag_object_id` bigint DEFAULT 0 COMMENT '标签对象ID',
    `ext` varchar(1000) DEFAULT NULL COMMENT '扩展信息',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `viewer` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '查看者',
    `view_org` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '查看组织',
    `admin` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '管理员',
    `admin_org` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '管理员组织',
    `is_open` tinyint DEFAULT NULL COMMENT '是否公开',
    `created_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `created_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `updated_at` datetime DEFAULT NULL,
    `entity` text COLLATE utf8mb4_unicode_ci COMMENT '实体信息',
    PRIMARY KEY (`id`),
    KEY `idx_model_tenant` (`tenant_id`),
    KEY `idx_model_domain` (`domain_id`),
    KEY `idx_model_database` (`database_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型表';

-- 模型关系表
CREATE TABLE IF NOT EXISTS `s2_model_rela` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `domain_id` bigint COMMENT '主题域ID',
    `from_model_id` bigint NOT NULL COMMENT '源模型ID',
    `to_model_id` bigint NOT NULL COMMENT '目标模型ID',
    `join_type` varchar(255) COMMENT '关联类型',
    `join_condition` text COMMENT '关联条件',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_model_rela_domain` (`domain_id`),
    KEY `idx_model_rela_from` (`from_model_id`),
    KEY `idx_model_rela_to` (`to_model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型关系表';

-- ========================================
-- 3. 维度与指标表
-- ========================================

-- 维度表
CREATE TABLE IF NOT EXISTS `s2_dimension` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '维度ID',
    `model_id` bigint NOT NULL COMMENT '模型ID',
    `name` varchar(255) NOT NULL COMMENT '维度名称',
    `biz_name` varchar(255) NOT NULL COMMENT '字段名称',
    `description` varchar(500) NOT NULL COMMENT '描述',
    `status` tinyint NOT NULL COMMENT '维度状态: 0=正常, 1=下架',
    `sensitive_level` int DEFAULT NULL COMMENT '敏感级别',
    `type` varchar(50) NOT NULL COMMENT '维度类型: categorical, time',
    `type_params` text COMMENT '类型参数',
    `data_type` varchar(50) DEFAULT NULL COMMENT '维度数据类型: varchar, array',
    `expr` text NOT NULL COMMENT '表达式',
    `semantic_type` varchar(20) NOT NULL COMMENT '语义类型: DATE, ID, CATEGORY',
    `alias` varchar(500) DEFAULT NULL COMMENT '别名',
    `default_values` varchar(500) DEFAULT NULL COMMENT '默认值',
    `dim_value_maps` varchar(5000) DEFAULT NULL COMMENT '维度值映射',
    `is_tag` tinyint DEFAULT NULL COMMENT '是否标签',
    `ext` varchar(1000) DEFAULT NULL COMMENT '扩展信息',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL COMMENT '创建人',
    `updated_at` datetime NOT NULL COMMENT '更新时间',
    `updated_by` varchar(100) NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_dimension_model` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='维度表';

-- 指标表
CREATE TABLE IF NOT EXISTS `s2_metric` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `model_id` bigint NOT NULL COMMENT '模型ID',
    `name` varchar(255) NOT NULL COMMENT '指标名称',
    `biz_name` varchar(255) NOT NULL COMMENT '字段名称',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `status` tinyint NOT NULL COMMENT '指标状态',
    `sensitive_level` tinyint NOT NULL COMMENT '敏感级别',
    `type` varchar(50) NOT NULL COMMENT '指标类型',
    `type_params` text NOT NULL COMMENT '类型参数',
    `data_format_type` varchar(50) DEFAULT NULL COMMENT '数值类型',
    `data_format` varchar(500) DEFAULT NULL COMMENT '数值类型参数',
    `alias` varchar(500) DEFAULT NULL COMMENT '别名',
    `classifications` varchar(500) DEFAULT NULL COMMENT '分类',
    `relate_dimensions` varchar(500) DEFAULT NULL COMMENT '指标相关维度',
    `ext` text DEFAULT NULL COMMENT '扩展信息',
    `define_type` varchar(50) DEFAULT NULL COMMENT '定义类型: MEASURE, FIELD, METRIC',
    `is_publish` tinyint DEFAULT NULL COMMENT '是否发布',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL COMMENT '创建人',
    `updated_at` datetime NOT NULL COMMENT '更新时间',
    `updated_by` varchar(100) NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_metric_model` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标表';

-- ========================================
-- 4. 数据集表
-- ========================================

-- 数据集表
CREATE TABLE IF NOT EXISTS `s2_data_set` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `domain_id` bigint COMMENT '主题域ID',
    `name` varchar(255) COMMENT '名称',
    `biz_name` varchar(255) COMMENT '业务名称',
    `description` varchar(255) COMMENT '描述',
    `status` int COMMENT '状态',
    `alias` varchar(255) COMMENT '别名',
    `data_set_detail` text COMMENT '数据集详情',
    `query_config` varchar(3000) COMMENT '查询配置',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `admin` varchar(3000) DEFAULT NULL COMMENT '管理员',
    `admin_org` varchar(3000) DEFAULT NULL COMMENT '管理员组织',
    `created_at` datetime,
    `created_by` varchar(255),
    `updated_at` datetime,
    `updated_by` varchar(255),
    PRIMARY KEY (`id`),
    KEY `idx_data_set_tenant` (`tenant_id`),
    KEY `idx_data_set_domain` (`domain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据集表';

-- ========================================
-- 5. 智能体与对话表
-- ========================================

-- 智能体表
CREATE TABLE IF NOT EXISTS `s2_agent` (
    `id` bigint NOT NULL AUTO_INCREMENT,
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
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
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
    `chat_id` bigint NOT NULL AUTO_INCREMENT,
    `agent_id` bigint DEFAULT NULL COMMENT '智能体ID',
    `chat_name` varchar(300) DEFAULT NULL COMMENT '对话名称',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `last_time` datetime DEFAULT NULL COMMENT '最后更新时间',
    `creator` varchar(30) DEFAULT NULL COMMENT '创建者',
    `last_question` varchar(200) DEFAULT NULL COMMENT '最后问题',
    `is_delete` tinyint DEFAULT 0 COMMENT '是否删除',
    `is_top` tinyint DEFAULT 0 COMMENT '是否置顶',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`chat_id`),
    KEY `idx_chat_tenant` (`tenant_id`),
    KEY `idx_chat_agent` (`agent_id`),
    KEY `idx_chat_creator` (`creator`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话表';

-- 对话配置表
CREATE TABLE IF NOT EXISTS `s2_chat_config` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `model_id` bigint DEFAULT NULL COMMENT '模型ID',
    `chat_detail_config` mediumtext COMMENT '明细模式配置信息',
    `chat_agg_config` mediumtext COMMENT '指标模式配置信息',
    `recommended_questions` mediumtext COMMENT '推荐问题配置',
    `llm_examples` text COMMENT 'LLM示例',
    `status` tinyint NOT NULL COMMENT '状态: 0=删除, 1=生效',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
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
    `id` bigint NOT NULL AUTO_INCREMENT,
    `question` varchar(655) COMMENT '用户问题',
    `side_info` text COMMENT '辅助信息',
    `query_id` bigint COMMENT '问答ID',
    `agent_id` bigint COMMENT '智能体ID',
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
    `chat_id` bigint NOT NULL COMMENT '对话ID',
    `modified_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `query_user` varchar(64) DEFAULT NULL COMMENT '查询用户',
    `query_text` text COMMENT '查询文本',
    `semantic_parse` text COMMENT '语义解析数据',
    `ext_data` text COMMENT '扩展数据',
    PRIMARY KEY (`chat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话上下文表';

-- 对话解析表
CREATE TABLE IF NOT EXISTS `s2_chat_parse` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `question_id` bigint NOT NULL COMMENT '问题ID',
    `chat_id` bigint NOT NULL COMMENT '对话ID',
    `parse_id` bigint NOT NULL COMMENT '解析ID',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `query_text` varchar(500) DEFAULT NULL COMMENT '查询文本',
    `user_name` varchar(150) DEFAULT NULL COMMENT '用户名',
    `parse_info` mediumtext NOT NULL COMMENT '解析信息',
    `is_candidate` int DEFAULT 1 COMMENT '1=候选, 0=已选',
    PRIMARY KEY (`id`),
    KEY `idx_chat_parse_question` (`question_id`),
    KEY `idx_chat_parse_chat` (`chat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话解析表';

-- 对话查询表
CREATE TABLE IF NOT EXISTS `s2_chat_query` (
    `question_id` bigint NOT NULL AUTO_INCREMENT,
    `agent_id` bigint DEFAULT NULL COMMENT '智能体ID',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `query_text` mediumtext COMMENT '查询文本',
    `user_name` varchar(150) DEFAULT NULL COMMENT '用户名',
    `query_state` int DEFAULT NULL COMMENT '查询状态',
    `chat_id` bigint NOT NULL COMMENT '对话ID',
    `query_result` mediumtext COMMENT '查询结果',
    `score` int DEFAULT 0 COMMENT '评分',
    `feedback` varchar(1024) DEFAULT '' COMMENT '反馈',
    `similar_queries` varchar(1024) DEFAULT '' COMMENT '相似查询',
    `parse_time_cost` varchar(1024) DEFAULT '' COMMENT '解析耗时',
    PRIMARY KEY (`question_id`),
    KEY `idx_chat_query_agent` (`agent_id`),
    KEY `idx_chat_query_chat` (`chat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话查询表';

-- 对话统计表
CREATE TABLE IF NOT EXISTS `s2_chat_statistics` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `question_id` bigint NOT NULL COMMENT '问题ID',
    `chat_id` bigint NOT NULL COMMENT '对话ID',
    `user_name` varchar(150) DEFAULT NULL COMMENT '用户名',
    `query_text` varchar(200) DEFAULT NULL COMMENT '查询文本',
    `interface_name` varchar(100) DEFAULT NULL COMMENT '接口名称',
    `cost` int DEFAULT 0 COMMENT '耗时(ms)',
    `type` int DEFAULT NULL COMMENT '类型',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_chat_statistics_question` (`question_id`),
    KEY `idx_chat_statistics_chat` (`chat_id`),
    KEY `idx_chat_statistics_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话统计表';

-- 对话模型实例表
CREATE TABLE IF NOT EXISTS `s2_chat_model` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL COMMENT '名称',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `config` text NOT NULL COMMENT '配置信息',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `admin` varchar(500) DEFAULT NULL COMMENT '管理员',
    `viewer` varchar(500) DEFAULT NULL COMMENT '查看者',
    `is_open` tinyint DEFAULT NULL COMMENT '是否公开',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL COMMENT '创建人',
    `updated_at` datetime NOT NULL COMMENT '更新时间',
    `updated_by` varchar(100) NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_chat_model_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话大模型实例表';

-- ========================================
-- 6. 标签与分类表
-- ========================================

-- 标签对象表
CREATE TABLE IF NOT EXISTS `s2_tag_object` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `domain_id` bigint DEFAULT NULL COMMENT '主题域ID',
    `name` varchar(255) NOT NULL COMMENT '名称',
    `biz_name` varchar(255) NOT NULL COMMENT '英文名称',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
    `sensitive_level` tinyint NOT NULL DEFAULT 0 COMMENT '敏感级别',
    `ext` text DEFAULT NULL COMMENT '扩展信息',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL COMMENT '创建人',
    `updated_at` datetime NULL COMMENT '更新时间',
    `updated_by` varchar(100) NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_tag_object_domain` (`domain_id`),
    KEY `idx_tag_object_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签对象表';

-- 标签表
CREATE TABLE IF NOT EXISTS `s2_tag` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `item_id` bigint NOT NULL COMMENT '关联项ID',
    `type` varchar(255) NOT NULL COMMENT '类型',
    `ext` text DEFAULT NULL COMMENT '扩展信息',
    `created_at` datetime NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_tag_item` (`item_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- ========================================
-- 7. 术语与规则表
-- ========================================

-- 术语表
CREATE TABLE IF NOT EXISTS `s2_term` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `domain_id` bigint COMMENT '主题域ID',
    `name` varchar(255) NOT NULL COMMENT '术语名称',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `alias` varchar(1000) NOT NULL COMMENT '别名',
    `related_metrics` varchar(1000) DEFAULT NULL COMMENT '关联指标',
    `related_dimensions` varchar(1000) DEFAULT NULL COMMENT '关联维度',
    `created_at` datetime NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_term_domain` (`domain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='术语表';

-- 查询规则表
CREATE TABLE IF NOT EXISTS `s2_query_rule` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `data_set_id` bigint COMMENT '数据集ID',
    `priority` int NOT NULL DEFAULT 1 COMMENT '优先级',
    `rule_type` varchar(255) NOT NULL COMMENT '规则类型',
    `name` varchar(255) NOT NULL COMMENT '名称',
    `biz_name` varchar(255) NOT NULL COMMENT '业务名称',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `rule` text DEFAULT NULL COMMENT '规则内容',
    `action` text DEFAULT NULL COMMENT '动作',
    `status` int NOT NULL DEFAULT 1 COMMENT '状态',
    `ext` text DEFAULT NULL COMMENT '扩展信息',
    `created_at` datetime NOT NULL,
    `created_by` varchar(100) NOT NULL,
    `updated_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_query_rule_data_set` (`data_set_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='查询规则表';

-- ========================================
-- 8. 字典与配置表
-- ========================================

-- 字典配置表
CREATE TABLE IF NOT EXISTS `s2_dictionary_conf` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `description` varchar(255) COMMENT '描述',
    `type` varchar(255) NOT NULL COMMENT '类型',
    `item_id` bigint NOT NULL COMMENT '关联项ID',
    `config` mediumtext COMMENT '配置',
    `status` varchar(255) NOT NULL COMMENT '状态',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_dictionary_conf_item` (`item_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典配置信息表';

-- 字典任务表
CREATE TABLE IF NOT EXISTS `s2_dictionary_task` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL COMMENT '名称',
    `description` varchar(255) COMMENT '描述',
    `type` varchar(255) NOT NULL COMMENT '类型',
    `item_id` bigint NOT NULL COMMENT '关联项ID',
    `config` mediumtext COMMENT '配置',
    `status` varchar(255) NOT NULL COMMENT '状态',
    `elapsed_ms` int DEFAULT NULL COMMENT '耗时(ms)',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_dictionary_task_item` (`item_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典运行任务表';

-- 可用日期信息表
CREATE TABLE IF NOT EXISTS `s2_available_date_info` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `item_id` bigint NOT NULL COMMENT '关联项ID',
    `type` varchar(255) NOT NULL COMMENT '类型',
    `date_format` varchar(64) NOT NULL COMMENT '日期格式',
    `date_period` varchar(64) DEFAULT NULL COMMENT '日期周期',
    `start_date` varchar(64) DEFAULT NULL COMMENT '开始日期',
    `end_date` varchar(64) DEFAULT NULL COMMENT '结束日期',
    `unavailable_date` text COMMENT '不可用日期',
    `status` tinyint DEFAULT 0 COMMENT '状态',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` varchar(100) NOT NULL,
    `updated_at` timestamp NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_item_type` (`item_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='可用日期信息表';

-- ========================================
-- 9. 插件与应用表
-- ========================================

-- 插件表
CREATE TABLE IF NOT EXISTS `s2_plugin` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `type` varchar(50) DEFAULT NULL COMMENT '类型: DASHBOARD, WIDGET, URL',
    `data_set` varchar(100) DEFAULT NULL COMMENT '数据集',
    `pattern` varchar(500) DEFAULT NULL COMMENT '模式',
    `parse_mode` varchar(100) DEFAULT NULL COMMENT '解析模式',
    `parse_mode_config` text COMMENT '解析模式配置',
    `name` varchar(100) DEFAULT NULL COMMENT '名称',
    `config` text COMMENT '配置',
    `comment` text COMMENT '备注',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT NULL,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_plugin_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='插件表';

-- 应用表
CREATE TABLE IF NOT EXISTS `s2_app` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) COMMENT '名称',
    `description` varchar(255) COMMENT '描述',
    `status` int COMMENT '状态',
    `config` text COMMENT '配置',
    `end_date` datetime COMMENT '结束日期',
    `qps` int COMMENT 'QPS限制',
    `app_secret` varchar(255) COMMENT '应用密钥',
    `owner` varchar(255) COMMENT '所有者',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime NULL,
    `updated_at` datetime NULL,
    `created_by` varchar(255) NULL,
    `updated_by` varchar(255) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_app_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用表';

-- ========================================
-- 10. 统计与分析表
-- ========================================

-- 查询统计信息表
CREATE TABLE IF NOT EXISTS `s2_query_stat_info` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `trace_id` varchar(200) DEFAULT NULL COMMENT '查询标识',
    `model_id` bigint DEFAULT NULL COMMENT '模型ID',
    `data_set_id` bigint DEFAULT NULL COMMENT '数据集ID',
    `query_user` varchar(200) DEFAULT NULL COMMENT '执行SQL的用户',
    `query_type` varchar(200) DEFAULT NULL COMMENT '查询对应的场景',
    `query_type_back` int DEFAULT 0 COMMENT '查询类型: 0=正常查询, 1=预刷类型',
    `query_sql_cmd` mediumtext COMMENT '对应查询的SQL命令',
    `sql_cmd_md5` varchar(200) DEFAULT NULL COMMENT 'SQL命令MD5值',
    `query_struct_cmd` mediumtext COMMENT '对应查询的struct',
    `struct_cmd_md5` varchar(200) DEFAULT NULL COMMENT 'struct MD5值',
    `query_sql` mediumtext COMMENT '对应查询的SQL',
    `sql_md5` varchar(200) DEFAULT NULL COMMENT 'SQL MD5值',
    `query_engine` varchar(20) DEFAULT NULL COMMENT '查询引擎',
    `elapsed_ms` bigint DEFAULT NULL COMMENT '查询耗时(ms)',
    `query_state` varchar(20) DEFAULT NULL COMMENT '查询最终状态',
    `native_query` int DEFAULT NULL COMMENT '1=明细查询, 0=聚合查询',
    `start_date` varchar(50) DEFAULT NULL COMMENT 'SQL开始日期',
    `end_date` varchar(50) DEFAULT NULL COMMENT 'SQL结束日期',
    `dimensions` mediumtext COMMENT 'SQL涉及的维度',
    `metrics` mediumtext COMMENT 'SQL涉及的指标',
    `select_cols` mediumtext COMMENT 'SQL select部分涉及的标签',
    `agg_cols` mediumtext COMMENT 'SQL agg部分涉及的标签',
    `filter_cols` mediumtext COMMENT 'SQL where部分涉及的标签',
    `group_by_cols` mediumtext COMMENT 'SQL group by部分涉及的标签',
    `order_by_cols` mediumtext COMMENT 'SQL order by部分涉及的标签',
    `use_result_cache` tinyint DEFAULT -1 COMMENT '是否命中结果缓存',
    `use_sql_cache` tinyint DEFAULT -1 COMMENT '是否命中SQL缓存',
    `sql_cache_key` mediumtext COMMENT 'SQL缓存key',
    `result_cache_key` mediumtext COMMENT '结果缓存key',
    `query_opt_mode` varchar(20) DEFAULT NULL COMMENT '优化模式',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_query_stat_model` (`model_id`),
    KEY `idx_query_stat_data_set` (`data_set_id`),
    KEY `idx_query_stat_tenant` (`tenant_id`),
    KEY `idx_query_stat_user` (`query_user`),
    KEY `idx_query_stat_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='查询统计信息表';

-- ========================================
-- 11. 其他辅助表
-- ========================================

-- 画布表
CREATE TABLE IF NOT EXISTS `s2_canvas` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `domain_id` bigint DEFAULT NULL COMMENT '主题域ID',
    `type` varchar(20) DEFAULT NULL COMMENT '类型: datasource, dimension, metric',
    `config` text COMMENT '配置详情',
    `created_at` datetime DEFAULT NULL,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_canvas_domain` (`domain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='画布表';

-- 收藏表
CREATE TABLE IF NOT EXISTS `s2_collect` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `type` varchar(20) NOT NULL COMMENT '类型',
    `username` varchar(20) NOT NULL COMMENT '用户名',
    `collect_id` bigint NOT NULL COMMENT '收藏对象ID',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time` datetime,
    `update_time` datetime,
    PRIMARY KEY (`id`),
    KEY `idx_collect_user` (`username`),
    KEY `idx_collect_tenant` (`tenant_id`),
    KEY `idx_collect_type_id` (`type`, `collect_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- 指标查询默认配置表
CREATE TABLE IF NOT EXISTS `s2_metric_query_default_config` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `metric_id` bigint COMMENT '指标ID',
    `user_name` varchar(255) NOT NULL COMMENT '用户名',
    `default_config` varchar(1000) NOT NULL COMMENT '默认配置',
    `created_at` datetime NULL,
    `updated_at` datetime NULL,
    `created_by` varchar(100) NULL,
    `updated_by` varchar(100) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_metric_query_config_metric` (`metric_id`),
    KEY `idx_metric_query_config_user` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标查询默认配置表';

-- 认证组表
CREATE TABLE IF NOT EXISTS `s2_auth_groups` (
    `group_id` bigint NOT NULL,
    `config` varchar(2048) DEFAULT NULL COMMENT '配置',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`group_id`),
    KEY `idx_auth_groups_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认证组表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `s2_system_config` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `admin` varchar(500) COMMENT '系统管理员',
    `parameters` text NULL COMMENT '配置项',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_system_config_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
