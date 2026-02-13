-- Headless module schema (MySQL)


-- ========================================
-- 4. 核心业务表 - 数据源与数据库
-- ========================================

-- 数据库实例表
CREATE TABLE IF NOT EXISTS `s2_database` (
                                             `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL COMMENT '名称',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `version` varchar(64) DEFAULT NULL COMMENT '版本',
    `type` varchar(20) NOT NULL COMMENT '类型: mysql, clickhouse, tdw等',
    `config` text NOT NULL COMMENT '配置信息(JSON)',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
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
-- 5. 核心业务表 - 主题域与模型
-- ========================================

-- 主题域表
CREATE TABLE IF NOT EXISTS `s2_domain` (
                                           `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `name` varchar(255) DEFAULT NULL COMMENT '主题域名称',
    `biz_name` varchar(255) DEFAULT NULL COMMENT '业务名称',
    `parent_id` bigint(20) DEFAULT 0 COMMENT '父主题域ID',
    `status` tinyint NOT NULL COMMENT '主题域状态',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
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
                                          `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模型名称',
    `biz_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务名称',
    `domain_id` bigint(20) DEFAULT NULL COMMENT '主题域ID',
    `alias` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '别名',
    `status` tinyint DEFAULT NULL COMMENT '状态',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `database_id` bigint(20) NOT NULL COMMENT '数据库ID',
    `model_detail` text NOT NULL COMMENT '模型详情(JSON)',
    `source_type` varchar(128) DEFAULT NULL COMMENT '来源类型',
    `depends` varchar(500) DEFAULT NULL COMMENT '依赖',
    `filter_sql` varchar(1000) DEFAULT NULL COMMENT '过滤SQL',
    `drill_down_dimensions` text DEFAULT NULL COMMENT '下钻维度',
    `tag_object_id` bigint(20) DEFAULT 0 COMMENT '标签对象ID',
    `ext` varchar(1000) DEFAULT NULL COMMENT '扩展信息',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
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
                                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `domain_id` bigint(20) COMMENT '主题域ID',
    `from_model_id` bigint(20) NOT NULL COMMENT '源模型ID',
    `to_model_id` bigint(20) NOT NULL COMMENT '目标模型ID',
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
-- 6. 核心业务表 - 维度与指标
-- ========================================

-- 维度表
CREATE TABLE IF NOT EXISTS `s2_dimension` (
                                              `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '维度ID',
    `model_id` bigint(20) NOT NULL COMMENT '模型ID',
    `name` varchar(255) NOT NULL COMMENT '维度名称',
    `biz_name` varchar(255) NOT NULL COMMENT '字段名称',
    `description` varchar(500) NOT NULL COMMENT '描述',
    `status` tinyint NOT NULL COMMENT '维度状态: 0=正常, 1=下架',
    `sensitive_level` int(10) DEFAULT NULL COMMENT '敏感级别',
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
                                           `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `model_id` bigint(20) NOT NULL COMMENT '模型ID',
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
-- 7. 核心业务表 - 数据集
-- ========================================

-- 数据集表
CREATE TABLE IF NOT EXISTS `s2_data_set` (
                                             `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `domain_id` bigint(20) COMMENT '主题域ID',
    `name` varchar(255) COMMENT '名称',
    `biz_name` varchar(255) COMMENT '业务名称',
    `description` varchar(255) COMMENT '描述',
    `status` int COMMENT '状态',
    `alias` varchar(255) COMMENT '别名',
    `data_set_detail` text COMMENT '数据集详情',
    `query_config` varchar(3000) COMMENT '查询配置',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `admin` varchar(3000) DEFAULT NULL COMMENT '管理员',
    `admin_org` varchar(3000) DEFAULT NULL COMMENT '管理员组织',
    `viewer` text DEFAULT NULL COMMENT '可查看用户',
    `view_org` text DEFAULT NULL COMMENT '可查看组织',
    `is_open` tinyint DEFAULT 0 COMMENT '是否公开',
    `created_at` datetime,
    `created_by` varchar(255),
    `updated_at` datetime,
    `updated_by` varchar(255),
    PRIMARY KEY (`id`),
    KEY `idx_data_set_tenant` (`tenant_id`),
    KEY `idx_data_set_domain` (`domain_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据集表';


-- 数据集权限组表
CREATE TABLE IF NOT EXISTS `s2_dataset_auth_groups` (
    `group_id` bigint(20) NOT NULL AUTO_INCREMENT,
    `dataset_id` bigint(20) NOT NULL COMMENT '数据集ID',
    `name` varchar(255) COMMENT '权限组名称',
    `auth_rules` text COMMENT '列权限JSON',
    `dimension_filters` text COMMENT '行权限JSON',
    `dimension_filter_description` varchar(500) COMMENT '行权限描述',
    `authorized_users` text COMMENT '授权用户列表',
    `authorized_department_ids` text COMMENT '授权部门ID列表',
    `inherit_from_model` tinyint DEFAULT 1 COMMENT '是否继承Model权限',
    `tenant_id` bigint(20) DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100),
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` varchar(100),
    PRIMARY KEY (`group_id`),
    KEY `idx_dataset_auth_dataset` (`dataset_id`),
    KEY `idx_dataset_auth_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据集权限组表';


-- ========================================
-- 9. 标签与分类表
-- ========================================

-- 标签对象表
CREATE TABLE IF NOT EXISTS `s2_tag_object` (
                                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `domain_id` bigint(20) DEFAULT NULL COMMENT '主题域ID',
    `name` varchar(255) NOT NULL COMMENT '名称',
    `biz_name` varchar(255) NOT NULL COMMENT '英文名称',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
    `sensitive_level` tinyint NOT NULL DEFAULT 0 COMMENT '敏感级别',
    `ext` text DEFAULT NULL COMMENT '扩展信息',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
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
                                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `item_id` bigint(20) NOT NULL COMMENT '关联项ID',
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
-- 10. 术语与规则表
-- ========================================

-- 术语表
CREATE TABLE IF NOT EXISTS `s2_term` (
                                         `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `domain_id` bigint(20) COMMENT '主题域ID',
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
                                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `data_set_id` bigint(20) COMMENT '数据集ID',
    `priority` int(10) NOT NULL DEFAULT 1 COMMENT '优先级',
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
-- 11. 字典与配置表
-- ========================================

-- 字典配置表
CREATE TABLE IF NOT EXISTS `s2_dictionary_conf` (
                                                    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `description` varchar(255) COMMENT '描述',
    `type` varchar(255) NOT NULL COMMENT '类型',
    `item_id` bigint(20) NOT NULL COMMENT '关联项ID',
    `config` mediumtext COMMENT '配置',
    `status` varchar(255) NOT NULL COMMENT '状态',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_dictionary_conf_item` (`item_id`, `type`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典配置信息表';


-- 字典任务表
CREATE TABLE IF NOT EXISTS `s2_dictionary_task` (
                                                    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL COMMENT '名称',
    `description` varchar(255) COMMENT '描述',
    `type` varchar(255) NOT NULL COMMENT '类型',
    `item_id` bigint(20) NOT NULL COMMENT '关联项ID',
    `config` mediumtext COMMENT '配置',
    `status` varchar(255) NOT NULL COMMENT '状态',
    `elapsed_ms` int(10) DEFAULT NULL COMMENT '耗时(ms)',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_dictionary_task_item` (`item_id`, `type`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典运行任务表';


-- 可用日期信息表
CREATE TABLE IF NOT EXISTS `s2_available_date_info` (
                                                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `item_id` bigint(20) NOT NULL COMMENT '关联项ID',
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
-- 13. 统计与分析表
-- ========================================

-- 查询统计信息表
CREATE TABLE IF NOT EXISTS `s2_query_stat_info` (
                                                    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `trace_id` varchar(200) DEFAULT NULL COMMENT '查询标识',
    `model_id` bigint(20) DEFAULT NULL COMMENT '模型ID',
    `data_set_id` bigint(20) DEFAULT NULL COMMENT '数据集ID',
    `query_user` varchar(200) DEFAULT NULL COMMENT '执行SQL的用户',
    `query_type` varchar(200) DEFAULT NULL COMMENT '查询对应的场景',
    `query_type_back` int(10) DEFAULT 0 COMMENT '查询类型: 0=正常查询, 1=预刷类型',
    `query_sql_cmd` mediumtext COMMENT '对应查询的SQL命令',
    `sql_cmd_md5` varchar(200) DEFAULT NULL COMMENT 'SQL命令MD5值',
    `query_struct_cmd` mediumtext COMMENT '对应查询的struct',
    `struct_cmd_md5` varchar(200) DEFAULT NULL COMMENT 'struct MD5值',
    `query_sql` mediumtext COMMENT '对应查询的SQL',
    `sql_md5` varchar(200) DEFAULT NULL COMMENT 'SQL MD5值',
    `query_engine` varchar(20) DEFAULT NULL COMMENT '查询引擎',
    `elapsed_ms` bigint(10) DEFAULT NULL COMMENT '查询耗时(ms)',
    `query_state` varchar(20) DEFAULT NULL COMMENT '查询最终状态',
    `native_query` int(10) DEFAULT NULL COMMENT '1=明细查询, 0=聚合查询',
    `start_date` varchar(50) DEFAULT NULL COMMENT 'SQL开始日期',
    `end_date` varchar(50) DEFAULT NULL COMMENT 'SQL结束日期',
    `dimensions` mediumtext COMMENT 'SQL涉及的维度',
    `metrics` mediumtext COMMENT 'SQL涉及的指标',
    `select_cols` mediumtext COMMENT 'SQL select部分涉及的标签',
    `agg_cols` mediumtext COMMENT 'SQL agg部分涉及的标签',
    `filter_cols` mediumtext COMMENT 'SQL where部分涉及的标签',
    `group_by_cols` mediumtext COMMENT 'SQL group by部分涉及的标签',
    `order_by_cols` mediumtext COMMENT 'SQL order by部分涉及的标签',
    `use_result_cache` tinyint(1) DEFAULT -1 COMMENT '是否命中结果缓存',
    `use_sql_cache` tinyint(1) DEFAULT -1 COMMENT '是否命中SQL缓存',
    `sql_cache_key` mediumtext COMMENT 'SQL缓存key',
    `result_cache_key` mediumtext COMMENT '结果缓存key',
    `query_opt_mode` varchar(20) DEFAULT NULL COMMENT '优化模式',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_query_stat_model` (`model_id`),
    KEY `idx_query_stat_data_set` (`data_set_id`),
    KEY `idx_query_stat_tenant` (`tenant_id`),
    KEY `idx_query_stat_user` (`query_user`),
    KEY `idx_query_stat_created` (`created_at`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='查询统计信息表';


-- ========================================
-- 14. 其他辅助表
-- ========================================

-- 画布表
CREATE TABLE IF NOT EXISTS `s2_canvas` (
                                           `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `domain_id` bigint(20) DEFAULT NULL COMMENT '主题域ID',
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
                                            `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `type` varchar(20) NOT NULL COMMENT '类型',
    `username` varchar(20) NOT NULL COMMENT '用户名',
    `collect_id` bigint(20) NOT NULL COMMENT '收藏对象ID',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time` datetime,
    `update_time` datetime,
    PRIMARY KEY (`id`),
    KEY `idx_collect_user` (`username`),
    KEY `idx_collect_tenant` (`tenant_id`),
    KEY `idx_collect_type_id` (`type`, `collect_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';


-- 指标查询默认配置表
CREATE TABLE IF NOT EXISTS `s2_metric_query_default_config` (
                                                                `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `metric_id` bigint(20) COMMENT '指标ID',
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

-- ========================================
-- 15. 语义模板管理表
-- ========================================

-- 语义模板定义表
CREATE TABLE IF NOT EXISTS `s2_semantic_template` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(100) NOT NULL COMMENT '模板名称',
    `biz_name` varchar(100) NOT NULL COMMENT '模板代码',
    `description` varchar(500) DEFAULT NULL COMMENT '模板描述',
    `category` varchar(50) NOT NULL COMMENT '模板类别: VISITS/SINGER/COMPANY/ECOMMERCE',
    `template_config` longtext NOT NULL COMMENT 'JSON: 模板配置',
    `preview_image` varchar(500) DEFAULT NULL COMMENT '预览图URL',
    `status` tinyint DEFAULT 0 COMMENT '状态: 0-草稿 1-已部署',
    `is_builtin` tinyint DEFAULT 0 COMMENT '是否内置模板: 0-租户自定义 1-系统内置',
    `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户ID: 1表示系统级(内置模板)',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_semantic_template_tenant_biz` (`tenant_id`, `biz_name`),
    KEY `idx_semantic_template_tenant` (`tenant_id`),
    KEY `idx_semantic_template_builtin` (`is_builtin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语义模板定义表';


-- 语义模板部署记录表
CREATE TABLE IF NOT EXISTS `s2_semantic_deployment` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `template_id` bigint(20) NOT NULL COMMENT '模板ID',
    `template_name` varchar(100) DEFAULT NULL COMMENT '模板名称快照',
    `database_id` bigint(20) DEFAULT NULL COMMENT '目标数据库ID',
    `param_config` text DEFAULT NULL COMMENT 'JSON: 用户自定义参数',
    `status` varchar(20) NOT NULL COMMENT '状态: PENDING/RUNNING/SUCCESS/FAILED',
    `result_detail` longtext DEFAULT NULL COMMENT 'JSON: 创建的对象详情',
    `error_message` text DEFAULT NULL COMMENT '错误信息',
    `current_step` varchar(50) DEFAULT NULL COMMENT '当前执行步骤',
    `start_time` datetime DEFAULT NULL COMMENT '开始时间',
    `end_time` datetime DEFAULT NULL COMMENT '结束时间',
    `tenant_id` bigint(20) NOT NULL COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `active_lock` varchar(100) DEFAULT NULL COMMENT '部署并发锁: PENDING/RUNNING时为templateId_tenantId, 否则为NULL',
    PRIMARY KEY (`id`),
    KEY `idx_semantic_deployment_template` (`template_id`),
    KEY `idx_semantic_deployment_tenant` (`tenant_id`),
    KEY `idx_semantic_deployment_status` (`status`),
    UNIQUE KEY `uk_deployment_active_lock` (`active_lock`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语义模板部署记录表';
