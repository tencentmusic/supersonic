CREATE TABLE `s2_agent` (
                            `id` int(11) NOT NULL AUTO_INCREMENT,
                            `name` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
                            `description` varchar(500) COLLATE utf8_unicode_ci DEFAULT NULL,
                            `examples` varchar(500) COLLATE utf8_unicode_ci DEFAULT NULL,
                            `status` int(11) DEFAULT NULL,
                            `model` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
                            `config` varchar(6000) COLLATE utf8_unicode_ci DEFAULT NULL,
                            `created_by` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
                            `created_at` datetime DEFAULT NULL,
                            `updated_by` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
                            `updated_at` datetime DEFAULT NULL,
                            `enable_search` int(11) DEFAULT NULL,
                            PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE `s2_auth_groups` (
                                  `group_id` int(11) NOT NULL,
                                  `config` varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                  PRIMARY KEY (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `s2_available_date_info` (
                                          `id` int(11) NOT NULL AUTO_INCREMENT,
                                          `item_id` int(11) NOT NULL,
                                          `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                          `date_format` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
                                          `date_period` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                          `start_date` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                          `end_date` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                                          `unavailable_date` text COLLATE utf8mb4_unicode_ci,
                                          `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                          `created_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
                                          `updated_at` timestamp NULL,
                                          `updated_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
                                          `status` int(11) DEFAULT '0',
                                          PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `s2_chat` (
                           `chat_id` bigint(8) NOT NULL AUTO_INCREMENT,
                           `agent_id` int(11) DEFAULT NULL,
                           `chat_name` varchar(300) DEFAULT NULL,
                           `create_time` datetime DEFAULT NULL,
                           `last_time` datetime DEFAULT NULL,
                           `creator` varchar(30) DEFAULT NULL,
                           `last_question` varchar(200) DEFAULT NULL,
                           `is_delete` int(2) DEFAULT '0' COMMENT 'is deleted',
                           `is_top` int(2) DEFAULT '0' COMMENT 'is top',
                           PRIMARY KEY (`chat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `s2_chat_config` (
                                  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
                                  `model_id` bigint(20) DEFAULT NULL,
                                  `chat_detail_config` mediumtext COMMENT '明细模式配置信息',
                                  `chat_agg_config` mediumtext COMMENT '指标模式配置信息',
                                  `recommended_questions` mediumtext COMMENT '推荐问题配置',
                                  `created_at` datetime NOT NULL COMMENT '创建时间',
                                  `updated_at` datetime NOT NULL COMMENT '更新时间',
                                  `created_by` varchar(100) NOT NULL COMMENT '创建人',
                                  `updated_by` varchar(100) NOT NULL COMMENT '更新人',
                                  `status` int(10) NOT NULL COMMENT '主题域扩展信息状态, 0-删除，1-生效',
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='主题域扩展信息表';

CREATE TABLE `s2_chat_context` (
                                   `chat_id` bigint(20) NOT NULL COMMENT 'context chat id',
                                   `modified_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'row modify time',
                                   `user` varchar(64) DEFAULT NULL COMMENT 'row modify user',
                                   `query_text` text COMMENT 'query text',
                                   `semantic_parse` text COMMENT 'parse data',
                                   `ext_data` text COMMENT 'extend data',
                                   PRIMARY KEY (`chat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `s2_chat_parse` (
                                 `question_id` bigint(20) NOT NULL,
                                 `chat_id` bigint(20) NOT NULL,
                                 `parse_id` int(11) NOT NULL,
                                 `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 `query_text` varchar(500) DEFAULT NULL,
                                 `user_name` varchar(150) DEFAULT NULL,
                                 `parse_info` mediumtext NOT NULL,
                                 `is_candidate` int(11) DEFAULT '1' COMMENT '1是candidate,0是selected',
                                 KEY `commonIndex` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE `s2_chat_query` (
                                 `question_id` bigint(20) NOT NULL AUTO_INCREMENT,
                                 `agent_id` int(11) DEFAULT NULL,
                                 `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 `query_text` mediumtext,
                                 `user_name` varchar(150) DEFAULT NULL,
                                 `query_state` int(1) DEFAULT NULL,
                                 `chat_id` bigint(20) NOT NULL,
                                 `query_result` mediumtext,
                                 `score` int(11) DEFAULT '0',
                                 `feedback` varchar(1024) DEFAULT '',
                                 PRIMARY KEY (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `s2_chat_statistics` (
                                      `question_id` bigint(20) NOT NULL,
                                      `chat_id` bigint(20) NOT NULL,
                                      `user_name` varchar(150) DEFAULT NULL,
                                      `query_text` varchar(200) DEFAULT NULL,
                                      `interface_name` varchar(100) DEFAULT NULL,
                                      `cost` int(6) DEFAULT '0',
                                      `type` int(11) DEFAULT NULL,
                                      `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      KEY `commonIndex` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `s2_database` (
                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
                               `name` varchar(255) NOT NULL COMMENT '名称',
                               `description` varchar(500) DEFAULT NULL COMMENT '描述',
                               `version` varchar(64) DEFAULT NULL,
                               `type` varchar(20) NOT NULL COMMENT '类型 mysql,clickhouse,tdw',
                               `config` text NOT NULL COMMENT '配置信息',
                               `created_at` datetime NOT NULL COMMENT '创建时间',
                               `created_by` varchar(100) NOT NULL COMMENT '创建人',
                               `updated_at` datetime NOT NULL COMMENT '更新时间',
                               `updated_by` varchar(100) NOT NULL COMMENT '更新人',
                               `admin` varchar(500) DEFAULT NULL,
                               `viewer` varchar(500) DEFAULT NULL,
                               PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数据库实例表';

CREATE TABLE `s2_datasource` (
                                 `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                 `model_id` bigint(20) DEFAULT NULL,
                                 `name` varchar(255) NOT NULL COMMENT '数据源名称',
                                 `biz_name` varchar(255) NOT NULL COMMENT '内部名称',
                                 `agg_time` varchar(32) DEFAULT 'day',
                                 `description` varchar(500) DEFAULT NULL COMMENT '数据源描述',
                                 `database_id` bigint(20) NOT NULL COMMENT '数据库实例ID',
                                 `datasource_detail` mediumtext NOT NULL COMMENT '数据源配置',
                                 `status` int(11) DEFAULT NULL ,
                                 `depends` text DEFAULT NULL COMMENT '上游依赖标识',
                                 `created_at` datetime NOT NULL COMMENT '创建时间',
                                 `created_by` varchar(100) NOT NULL COMMENT '创建人',
                                 `updated_at` datetime NOT NULL COMMENT '更新时间',
                                 `updated_by` varchar(100) NOT NULL COMMENT '更新人',
                                 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `s2_datasource_rela` (
                                      `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                      `model_id` bigint(20) DEFAULT NULL,
                                      `datasource_from` bigint(20) DEFAULT NULL,
                                      `datasource_to` bigint(20) DEFAULT NULL,
                                      `join_key` varchar(100) DEFAULT NULL,
                                      `created_at` datetime DEFAULT NULL,
                                      `created_by` varchar(100) DEFAULT NULL,
                                      `updated_at` datetime DEFAULT NULL,
                                      `updated_by` varchar(100) DEFAULT NULL,
                                      PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `s2_dictionary` (
                                 `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
                                 `item_id` bigint(20) DEFAULT NULL COMMENT '对应维度id、指标id等',
                                 `type` varchar(50) DEFAULT NULL COMMENT '对应维度、指标等',
                                 `black_list` mediumtext COMMENT '字典黑名单',
                                 `white_list` mediumtext COMMENT '字典白名单',
                                 `rule_list` mediumtext COMMENT '字典规则',
                                 `is_dict_Info` tinyint(1) NOT NULL DEFAULT '0' COMMENT '1-开启写入字典，0-不开启',
                                 `created_at` datetime NOT NULL COMMENT '创建时间',
                                 `updated_at` datetime NOT NULL COMMENT '更新时间',
                                 `created_by` varchar(100) NOT NULL COMMENT '创建人',
                                 `updated_by` varchar(100) DEFAULT NULL COMMENT '更新人',
                                 `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '1-删除,0-可用',
                                 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='字典配置信息表';

CREATE TABLE `s2_dictionary_task` (
                                      `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
                                      `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
                                      `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任务描述',
                                      `command` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务请求参数',
                                      `command_md5` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务请求参数',
                                      `dimension_ids` mediumtext  NULL COMMENT '本次执行维度列表',
                                      `status` int(10) NOT NULL COMMENT '任务最终运行状态',
                                      `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `created_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建人',
                                      `progress` double(3,2) DEFAULT '0.00' COMMENT '任务进度',
  `elapsed_ms` bigint(10) DEFAULT NULL COMMENT '任务耗时',
  `message` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '备注相关信息',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典任务信息表';


CREATE TABLE `s2_dimension` (
                                `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '维度ID',
                                `model_id` bigint(20) DEFAULT NULL,
                                `datasource_id` bigint(20) NOT NULL COMMENT '所属数据源id',
                                `name` varchar(255) NOT NULL COMMENT '维度名称',
                                `biz_name` varchar(255) NOT NULL COMMENT '字段名称',
                                `description` varchar(500) NOT NULL COMMENT '描述',
                                `status` int(10) NOT NULL COMMENT '维度状态,0正常,1下架',
                                `sensitive_level` int(10) DEFAULT NULL COMMENT '敏感级别',
                                `type` varchar(50) NOT NULL COMMENT '维度类型 categorical,time',
                                `type_params` text COMMENT '类型参数',
                                `data_type` varchar(50)  DEFAULT null comment '维度数据类型 varchar、array',
                                `expr` text NOT NULL COMMENT '表达式',
                                `created_at` datetime NOT NULL COMMENT '创建时间',
                                `created_by` varchar(100) NOT NULL COMMENT '创建人',
                                `updated_at` datetime NOT NULL COMMENT '更新时间',
                                `updated_by` varchar(100) NOT NULL COMMENT '更新人',
                                `semantic_type` varchar(20) NOT NULL COMMENT '语义类型DATE, ID, CATEGORY',
                                `alias` varchar(500) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
                                `default_values` varchar(500) DEFAULT NULL,
                                `dim_value_maps` varchar(5000) DEFAULT NULL,
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='维度表';

CREATE TABLE `s2_domain` (
                             `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
                             `name` varchar(255) DEFAULT NULL COMMENT '主题域名称',
                             `biz_name` varchar(255) DEFAULT NULL COMMENT '内部名称',
                             `parent_id` bigint(20) DEFAULT '0' COMMENT '父主题域ID',
                             `status` int(10) NOT NULL COMMENT '主题域状态',
                             `created_at` datetime DEFAULT NULL COMMENT '创建时间',
                             `created_by` varchar(100) DEFAULT NULL COMMENT '创建人',
                             `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
                             `updated_by` varchar(100) DEFAULT NULL COMMENT '更新人',
                             `admin` varchar(3000) DEFAULT NULL COMMENT '主题域管理员',
                             `admin_org` varchar(3000) DEFAULT NULL COMMENT '主题域管理员组织',
                             `is_open` int(11) DEFAULT NULL COMMENT '主题域是否公开',
                             `viewer` varchar(3000) DEFAULT NULL COMMENT '主题域可用用户',
                             `view_org` varchar(3000) DEFAULT NULL COMMENT '主题域可用组织',
                             `entity` varchar(500) DEFAULT NULL COMMENT '主题域实体信息',
                             PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='主题域基础信息表';


CREATE TABLE `s2_metric` (
                             `id` bigint(20) NOT NULL AUTO_INCREMENT,
                             `model_id` bigint(20) DEFAULT NULL,
                             `name` varchar(255) NOT NULL COMMENT '指标名称',
                             `biz_name` varchar(255) NOT NULL COMMENT '字段名称',
                             `description` varchar(500) DEFAULT NULL COMMENT '描述',
                             `status` int(10) NOT NULL COMMENT '指标状态,0未启用,1启用',
                             `sensitive_level` int(10) NOT NULL COMMENT '敏感级别',
                             `type` varchar(50) NOT NULL COMMENT '指标类型',
                             `type_params` text NOT NULL COMMENT '类型参数',
                             `created_at` datetime NOT NULL COMMENT '创建时间',
                             `created_by` varchar(100) NOT NULL COMMENT '创建人',
                             `updated_at` datetime NOT NULL COMMENT '更新时间',
                             `updated_by` varchar(100) NOT NULL COMMENT '更新人',
                             `data_format_type` varchar(50) DEFAULT NULL COMMENT '数值类型',
                             `data_format` varchar(500) DEFAULT NULL COMMENT '数值类型参数',
                             `alias` varchar(500) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
                             `tags` varchar(500) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
                             `relate_dimensions` varchar(500) DEFAULT NULL COMMENT '指标相关维度',
                             PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='指标表';


CREATE TABLE `s2_model` (
                            `id` bigint(20) NOT NULL AUTO_INCREMENT,
                            `name` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
                            `biz_name` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
                            `domain_id` bigint(20) DEFAULT NULL,
                            `alias` varchar(200) COLLATE utf8_unicode_ci DEFAULT NULL,
                            `status` int(11) DEFAULT NULL,
                            `description` varchar(500) DEFAULT NULL,
                            `viewer` varchar(500) COLLATE utf8_unicode_ci DEFAULT NULL,
                            `view_org` varchar(500) COLLATE utf8_unicode_ci DEFAULT NULL,
                            `admin` varchar(500) COLLATE utf8_unicode_ci DEFAULT NULL,
                            `admin_org` varchar(500) COLLATE utf8_unicode_ci DEFAULT NULL,
                            `is_open` int(11) DEFAULT NULL,
                            `created_by` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
                            `created_at` datetime DEFAULT NULL,
                            `updated_by` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
                            `updated_at` datetime DEFAULT NULL,
                            `entity` text COLLATE utf8_unicode_ci,
                            `drill_down_dimensions` varchar(500) DEFAULT NULL,
                            PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE `s2_plugin` (
                             `id` bigint(20) NOT NULL AUTO_INCREMENT,
                             `type` varchar(50) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL COMMENT 'DASHBOARD,WIDGET,URL',
                             `model` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                             `pattern` varchar(500) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
                             `parse_mode` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                             `parse_mode_config` text COLLATE utf8mb4_unicode_ci,
                             `name` varchar(100) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
                             `created_at` datetime DEFAULT NULL,
                             `created_by` varchar(100) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
                             `updated_at` datetime DEFAULT NULL,
                             `updated_by` varchar(100) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
                             `config` text CHARACTER SET utf8 COLLATE utf8_unicode_ci,
                             `comment` text COLLATE utf8mb4_unicode_ci,
                             PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `s2_query_stat_info` (
                                      `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
                                      `trace_id` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '查询标识',
                                      `model_id` bigint(20) DEFAULT NULL,
                                      `user` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '执行sql的用户',
                                      `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `query_type` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '查询对应的场景',
                                      `query_type_back` int(10) DEFAULT '0' COMMENT '查询类型, 0-正常查询, 1-预刷类型',
                                      `query_sql_cmd` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '对应查询的struct',
                                      `sql_cmd_md5` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'sql md5值',
                                      `query_struct_cmd` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '对应查询的struct',
                                      `struct_cmd_md5` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'sql md5值',
                                      `sql` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '对应查询的sql',
                                      `sql_md5` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'sql md5值',
                                      `query_engine` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '查询引擎',
                                      `elapsed_ms` bigint(10) DEFAULT NULL COMMENT '查询耗时',
                                      `query_state` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '查询最终状态',
                                      `native_query` int(10) DEFAULT NULL COMMENT '1-明细查询,0-聚合查询',
                                      `start_date` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'sql开始日期',
                                      `end_date` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'sql结束日期',
                                      `dimensions` mediumtext COLLATE utf8mb4_unicode_ci COMMENT 'sql 涉及的维度',
                                      `metrics` mediumtext COLLATE utf8mb4_unicode_ci COMMENT 'sql 涉及的指标',
                                      `select_cols` mediumtext COLLATE utf8mb4_unicode_ci COMMENT 'sql select部分涉及的标签',
                                      `agg_cols` mediumtext COLLATE utf8mb4_unicode_ci COMMENT 'sql agg部分涉及的标签',
                                      `filter_cols` mediumtext COLLATE utf8mb4_unicode_ci COMMENT 'sql where部分涉及的标签',
                                      `group_by_cols` mediumtext COLLATE utf8mb4_unicode_ci COMMENT 'sql grouy by部分涉及的标签',
                                      `order_by_cols` mediumtext COLLATE utf8mb4_unicode_ci COMMENT 'sql order by部分涉及的标签',
                                      `use_result_cache` tinyint(1) DEFAULT '-1' COMMENT '是否命中sql缓存',
                                      `use_sql_cache` tinyint(1) DEFAULT '-1' COMMENT '是否命中sql缓存',
                                      `sql_cache_key` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '缓存的key',
                                      `result_cache_key` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '缓存的key',
                                      `query_opt_mode` varchar(20) null comment '优化模式',
                                      PRIMARY KEY (`id`),
                                      KEY `domain_index` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='查询统计信息表';

CREATE TABLE `s2_semantic_pasre_info` (
                                          `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
                                          `trace_id` varchar(200) NOT NULL COMMENT '查询标识',
                                          `domain_id` bigint(20) NOT NULL COMMENT '主体域ID',
                                          `dimensions` mediumtext COMMENT '查询相关的维度信息',
                                          `metrics` mediumtext COMMENT '查询相关的指标信息',
                                          `orders` mediumtext COMMENT '查询相关的排序信息',
                                          `filters` mediumtext COMMENT '查询相关的过滤信息',
                                          `date_info` mediumtext COMMENT '查询相关的日期信息',
                                          `limit` bigint(20) NOT NULL COMMENT '查询相关的limit信息',
                                          `native_query` tinyint(1) NOT NULL DEFAULT '0' COMMENT '1-明细查询,0-聚合查询',
                                          `sql` mediumtext COMMENT '解析后的sql',
                                          `created_at` datetime NOT NULL COMMENT '创建时间',
                                          `created_by` varchar(100) NOT NULL COMMENT '创建人',
                                          `status` int(10) NOT NULL COMMENT '运行状态',
                                          `elapsed_ms` bigint(10) DEFAULT NULL COMMENT 'sql解析耗时',
                                          PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='语义层sql解析信息表';


CREATE TABLE `s2_view_info` (
                                `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                `model_id` bigint(20) DEFAULT NULL,
                                `type` varchar(20) DEFAULT NULL COMMENT 'datasource、dimension、metric',
                                `config` text COMMENT 'config detail',
                                `created_at` datetime DEFAULT NULL,
                                `created_by` varchar(100) DEFAULT NULL,
                                `updated_at` datetime DEFAULT NULL,
                                `updated_by` varchar(100) NOT NULL,
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table s2_user
(
    id       int(11) NOT NULL AUTO_INCREMENT,
    name     varchar(100) not null,
    display_name varchar(100) null,
    password varchar(100) null,
    email varchar(100) null,
    is_admin int(11) null,
    PRIMARY KEY (`id`)
);

insert into s2_user (id, `name`, password, display_name, email, is_admin) values (1, 'admin','admin','admin','admin@xx.com', 1);


CREATE TABLE `s2_materialization`
(
    `id`                bigint(20) NOT NULL AUTO_INCREMENT,
    `name`              varchar(255) NOT NULL COMMENT '物化名称',
    `materialized_type` varchar(255) NOT NULL COMMENT '物化类型 partition 分区 zipper 拉链 ',
    `update_cycle`      varchar(255) DEFAULT NULL COMMENT '更新周期，天更新、周更新、月更新',
    `model_id`          bigint(20) DEFAULT NULL,
    `database_id`       bigint(20) NOT NULL COMMENT '数据库实例ID',
    `level`             int(11) NOT NULL DEFAULT '0' COMMENT '优先级，数值越大优先级越高',
    `status`            int(11) NOT NULL DEFAULT '1' COMMENT '0-废弃，1-使用中',
    `destination_table` varchar(255) NOT NULL COMMENT '物化表名称',
    `date_info`         mediumtext COMMENT '时间字段',
    `entities`          mediumtext COMMENT 'primary字段',
    `principals`        varchar(255) DEFAULT NULL COMMENT '责任人',
    `created_at`        datetime     DEFAULT NULL,
    `created_by`        varchar(100) DEFAULT NULL,
    `updated_at`        datetime     DEFAULT NULL,
    `updated_by`        varchar(100) NOT NULL,
    `description`       mediumtext COMMENT '备注说明',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
CREATE TABLE `s2_materialization_element`
(
    `id`                 bigint(20) NOT NULL COMMENT 'element类型对应id',
    `type`               varchar(255) NOT NULL COMMENT 'element类型 metric、dimension',
    `materialization_id` bigint(20) NOT NULL COMMENT '关联s2_materialization主键',
    `depends`            text COMMENT '上游依赖标识',
    `element_type`       varchar(255) DEFAULT NULL COMMENT 'varchar,double,bigint,int,array',
    `default_value`      varchar(255) DEFAULT NULL COMMENT '默认值',
    `outlier`            varchar(255) DEFAULT NULL COMMENT '异常值',
    `frequency`          varchar(255) DEFAULT NULL COMMENT '变化频率, UNKNOWN, HIGH, LOW',
    `created_at`         datetime     DEFAULT NULL,
    `created_by`         varchar(100) DEFAULT NULL,
    `updated_at`         datetime     DEFAULT NULL,
    `updated_by`         varchar(100) NOT NULL,
    `description`        mediumtext COMMENT '备注说明',
    `status`             int(11) NOT NULL DEFAULT '1' COMMENT '0-废弃，1-使用中',
    PRIMARY KEY (`id`, `type`, `materialization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE `s2_materialization_record`
(
    `id`                 bigint(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `materialization_id` bigint(20) NOT NULL COMMENT '物化配置ID',
    `element_type`       varchar(255) NOT NULL COMMENT 'element类型 metric、dimension',
    `element_id`         bigint(20) DEFAULT NULL COMMENT '数据名字: 例如标签名、指标名',
    `element_name`       varchar(255) NOT NULL COMMENT 'element 名称',
    `data_time`          varchar(64)  DEFAULT NULL COMMENT '数据时间',
    `state`              varchar(255) DEFAULT NULL COMMENT '任务运行状态',
    `task_id`            varchar(255) DEFAULT NULL COMMENT '任务id',
    `created_at`         datetime     DEFAULT NULL,
    `updated_at`         datetime     DEFAULT NULL,
    `created_by`         varchar(100) DEFAULT NULL,
    `updated_by`         varchar(100) NOT NULL,
    `retry_count`        bigint(20) NOT NULL DEFAULT '0',
    `source_count`       bigint(20) NOT NULL DEFAULT '0',
    `sink_count`         bigint(20) NOT NULL DEFAULT '0',
    `message`            text COMMENT '信息',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_id` (`materialization_id`,`element_type`,`element_id`,`data_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

