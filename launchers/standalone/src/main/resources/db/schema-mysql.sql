CREATE TABLE IF NOT EXISTS `s2_agent` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `name` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
    `description` TEXT COLLATE utf8_unicode_ci DEFAULT NULL,
    `examples` TEXT COLLATE utf8_unicode_ci DEFAULT NULL,
    `status` tinyint DEFAULT NULL,
    `model` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
    `tool_config` varchar(6000) COLLATE utf8_unicode_ci DEFAULT NULL,
    `llm_config` varchar(2000) COLLATE utf8_unicode_ci DEFAULT NULL,
    `chat_model_config` text COLLATE utf8_unicode_ci DEFAULT NULL,
    `visual_config` varchar(2000)  COLLATE utf8_unicode_ci DEFAULT NULL,
    `enable_search` tinyint DEFAULT 1,
    `enable_feedback` tinyint DEFAULT 1,
    `created_by` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
    `created_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
    `updated_at` datetime DEFAULT NULL,
     `admin` varchar(3000) DEFAULT NULL COMMENT '管理员',
     `admin_org` varchar(3000) DEFAULT NULL COMMENT '管理员组织',
     `is_open` tinyint DEFAULT NULL COMMENT '是否公开',
     `viewer` varchar(3000) DEFAULT NULL COMMENT '可用用户',
     `view_org` varchar(3000) DEFAULT NULL COMMENT '可用组织',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE IF NOT EXISTS `s2_auth_groups` (
      `group_id` int(11) NOT NULL,
      `config` varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
      PRIMARY KEY (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `s2_available_date_info` (
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
  `status` tinyint DEFAULT 0,
  UNIQUE(`item_id`, `type`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `s2_chat` (
   `chat_id` bigint(8) NOT NULL AUTO_INCREMENT,
   `agent_id` int(11) DEFAULT NULL,
   `chat_name` varchar(300) DEFAULT NULL,
   `create_time` datetime DEFAULT NULL,
   `last_time` datetime DEFAULT NULL,
   `creator` varchar(30) DEFAULT NULL,
   `last_question` varchar(200) DEFAULT NULL,
   `is_delete` tinyint DEFAULT '0' COMMENT 'is deleted',
   `is_top` tinyint DEFAULT '0' COMMENT 'is top',
   PRIMARY KEY (`chat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `s2_chat_config` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `model_id` bigint(20) DEFAULT NULL,
  `chat_detail_config` mediumtext COMMENT '明细模式配置信息',
  `chat_agg_config` mediumtext COMMENT '指标模式配置信息',
  `recommended_questions` mediumtext COMMENT '推荐问题配置',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  `created_by` varchar(100) NOT NULL COMMENT '创建人',
  `updated_by` varchar(100) NOT NULL COMMENT '更新人',
  `status` tinyint NOT NULL COMMENT '主题域扩展信息状态, 0-删除，1-生效',
  `llm_examples` text COMMENT 'llm examples',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='主题域扩展信息表';

CREATE TABLE IF NOT EXISTS `s2_chat_memory` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `question` varchar(655)   COMMENT '用户问题' ,
    `side_info` TEXT COMMENT '辅助信息' ,
    `query_id`  BIGINT    COMMENT '问答ID' ,
    `agent_id`  INT    COMMENT '助理ID' ,
    `db_schema`  TEXT    COMMENT 'Schema映射' ,
    `s2_sql` TEXT   COMMENT '大模型解析SQL' ,
    `status` varchar(10)   COMMENT '状态' ,
    `llm_review` varchar(10)    COMMENT '大模型评估结果' ,
    `llm_comment`   TEXT COMMENT '大模型评估意见' ,
    `human_review` varchar(10) COMMENT '管理员评估结果',
    `human_comment` TEXT    COMMENT '管理员评估意见',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP  ,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
    `created_by` varchar(100) DEFAULT NULL   ,
    `updated_by` varchar(100) DEFAULT NULL   ,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `s2_chat_context` (
   `chat_id` bigint(20) NOT NULL COMMENT 'context chat id',
   `modified_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'row modify time',
   `query_user` varchar(64) DEFAULT NULL COMMENT 'row modify user',
   `query_text` text COMMENT 'query text',
   `semantic_parse` text COMMENT 'parse data',
   `ext_data` text COMMENT 'extend data',
   PRIMARY KEY (`chat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `s2_chat_parse` (
     `question_id` bigint NOT NULL,
     `chat_id` int(11) NOT NULL,
     `parse_id` int(11) NOT NULL,
     `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     `query_text` varchar(500) DEFAULT NULL,
     `user_name` varchar(150) DEFAULT NULL,
     `parse_info` mediumtext NOT NULL,
     `is_candidate` int(11) DEFAULT '1' COMMENT '1是candidate,0是selected',
     KEY `commonIndex` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS `s2_chat_query`
(
    `question_id`     bigint(20) NOT NULL AUTO_INCREMENT,
    `agent_id`        int(11)             DEFAULT NULL,
    `create_time`     timestamp  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `query_text`      mediumtext,
    `user_name`       varchar(150)        DEFAULT NULL,
    `query_state`     int(1)              DEFAULT NULL,
    `chat_id`         bigint(20) NOT NULL,
    `query_result`    mediumtext,
    `score`           int(11)             DEFAULT '0',
    `feedback`        varchar(1024)       DEFAULT '',
    `similar_queries` varchar(1024)       DEFAULT '',
    `parse_time_cost` varchar(1024)       DEFAULT '',
    PRIMARY KEY (`question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;


CREATE TABLE IF NOT EXISTS `s2_chat_statistics` (
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

CREATE TABLE IF NOT EXISTS `s2_chat_model` (
   `id` bigint(20) NOT NULL AUTO_INCREMENT,
   `name` varchar(255) NOT NULL COMMENT '名称',
   `description` varchar(500) DEFAULT NULL COMMENT '描述',
   `config` text NOT NULL COMMENT '配置信息',
   `created_at` datetime NOT NULL COMMENT '创建时间',
   `created_by` varchar(100) NOT NULL COMMENT '创建人',
   `updated_at` datetime NOT NULL COMMENT '更新时间',
   `updated_by` varchar(100) NOT NULL COMMENT '更新人',
   `admin` varchar(500) DEFAULT NULL,
   `viewer` varchar(500) DEFAULT NULL,
   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='对话大模型实例表';

CREATE TABLE IF NOT EXISTS `s2_database` (
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

CREATE TABLE IF NOT EXISTS `s2_dictionary_conf` (
   `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
   `description` varchar(255) ,
   `type` varchar(255)  NOT NULL ,
   `item_id` INT  NOT NULL ,
   `config` mediumtext  ,
   `status` varchar(255) NOT NULL ,
   `created_at` datetime NOT NULL COMMENT '创建时间' ,
   `created_by` varchar(100) NOT NULL ,
   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='字典配置信息表';


CREATE TABLE IF NOT EXISTS `s2_dictionary_task` (
   `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
   `name` varchar(255) NOT NULL ,
   `description` varchar(255) ,
   `type` varchar(255)  NOT NULL ,
   `item_id` INT  NOT NULL ,
   `config` mediumtext  ,
   `status` varchar(255) NOT NULL ,
   `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
   `created_by` varchar(100) NOT NULL ,
   `elapsed_ms` int(10) DEFAULT NULL ,
   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='字典运行任务表';


CREATE TABLE IF NOT EXISTS `s2_dimension` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '维度ID',
    `model_id` bigint(20) DEFAULT NULL,
    `name` varchar(255) NOT NULL COMMENT '维度名称',
    `biz_name` varchar(255) NOT NULL COMMENT '字段名称',
    `description` varchar(500) NOT NULL COMMENT '描述',
    `status` tinyint NOT NULL COMMENT '维度状态,0正常,1下架',
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
    `is_tag` tinyint DEFAULT NULL,
    `ext` varchar(1000) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='维度表';

CREATE TABLE IF NOT EXISTS `s2_domain` (
     `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
     `name` varchar(255) DEFAULT NULL COMMENT '主题域名称',
     `biz_name` varchar(255) DEFAULT NULL COMMENT '内部名称',
     `parent_id` bigint(20) DEFAULT '0' COMMENT '父主题域ID',
     `status` tinyint NOT NULL COMMENT '主题域状态',
     `created_at` datetime DEFAULT NULL COMMENT '创建时间',
     `created_by` varchar(100) DEFAULT NULL COMMENT '创建人',
     `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
     `updated_by` varchar(100) DEFAULT NULL COMMENT '更新人',
     `admin` varchar(3000) DEFAULT NULL COMMENT '主题域管理员',
     `admin_org` varchar(3000) DEFAULT NULL COMMENT '主题域管理员组织',
     `is_open` tinyint DEFAULT NULL COMMENT '主题域是否公开',
     `viewer` varchar(3000) DEFAULT NULL COMMENT '主题域可用用户',
     `view_org` varchar(3000) DEFAULT NULL COMMENT '主题域可用组织',
     `entity` varchar(500) DEFAULT NULL COMMENT '主题域实体信息',
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='主题域基础信息表';


CREATE TABLE IF NOT EXISTS `s2_metric`
(
    `id`                bigint(20)   NOT NULL AUTO_INCREMENT,
    `model_id`          bigint(20)   DEFAULT NULL,
    `name`              varchar(255) NOT NULL COMMENT '指标名称',
    `biz_name`          varchar(255) NOT NULL COMMENT '字段名称',
    `description`       varchar(500) DEFAULT NULL COMMENT '描述',
    `status`            tinyint      NOT NULL COMMENT '指标状态',
    `sensitive_level`   tinyint      NOT NULL COMMENT '敏感级别',
    `type`              varchar(50)  NOT NULL COMMENT '指标类型',
    `type_params`       text         NOT NULL COMMENT '类型参数',
    `created_at`        datetime     NOT NULL COMMENT '创建时间',
    `created_by`        varchar(100) NOT NULL COMMENT '创建人',
    `updated_at`        datetime     NOT NULL COMMENT '更新时间',
    `updated_by`        varchar(100) NOT NULL COMMENT '更新人',
    `data_format_type`  varchar(50)  DEFAULT NULL COMMENT '数值类型',
    `data_format`       varchar(500) DEFAULT NULL COMMENT '数值类型参数',
    `alias`             varchar(500) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
    `classifications`   varchar(500) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
    `relate_dimensions` varchar(500) DEFAULT NULL COMMENT '指标相关维度',
    `ext`               text DEFAULT NULL,
    `define_type` varchar(50)  DEFAULT NULL, -- MEASURE, FIELD, METRIC
    `is_publish` tinyint DEFAULT NULL COMMENT '是否发布',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='指标表';


CREATE TABLE IF NOT EXISTS `s2_model` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `name` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
    `biz_name` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
    `domain_id` bigint(20) DEFAULT NULL,
    `alias` varchar(200) COLLATE utf8_unicode_ci DEFAULT NULL,
    `status` tinyint DEFAULT NULL,
    `description` varchar(500) DEFAULT NULL,
    `viewer` varchar(500) COLLATE utf8_unicode_ci DEFAULT NULL,
    `view_org` varchar(500) COLLATE utf8_unicode_ci DEFAULT NULL,
    `admin` varchar(500) COLLATE utf8_unicode_ci DEFAULT NULL,
    `admin_org` varchar(500) COLLATE utf8_unicode_ci DEFAULT NULL,
    `is_open` tinyint DEFAULT NULL,
    `created_by` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
    `created_at` datetime DEFAULT NULL,
    `updated_by` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
    `updated_at` datetime DEFAULT NULL,
    `entity` text COLLATE utf8_unicode_ci,
    `drill_down_dimensions` TEXT DEFAULT NULL,
    `database_id` INT NOT  NULL ,
    `model_detail` text NOT  NULL ,
    `source_type` varchar(128) DEFAULT NULL ,
    `depends` varchar(500) DEFAULT NULL ,
    `filter_sql` varchar(1000) DEFAULT NULL ,
    `tag_object_id` int(11) DEFAULT '0',
    `ext` varchar(1000) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE IF NOT EXISTS `s2_plugin` (
     `id` bigint(20) NOT NULL AUTO_INCREMENT,
     `type` varchar(50) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL COMMENT 'DASHBOARD,WIDGET,URL',
     `data_set` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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

CREATE TABLE IF NOT EXISTS `s2_query_stat_info` (
      `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
      `trace_id` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '查询标识',
      `model_id` bigint(20) DEFAULT NULL,
      `data_set_id` bigint(20) DEFAULT NULL,
      `query_user` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '执行sql的用户',
      `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `query_type` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '查询对应的场景',
      `query_type_back` int(10) DEFAULT '0' COMMENT '查询类型, 0-正常查询, 1-预刷类型',
      `query_sql_cmd` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '对应查询的struct',
      `sql_cmd_md5` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'sql md5值',
      `query_struct_cmd` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '对应查询的struct',
      `struct_cmd_md5` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'sql md5值',
      `query_sql` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '对应查询的sql',
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

CREATE TABLE IF NOT EXISTS `s2_canvas`
(
    `id`         bigint(20)   NOT NULL AUTO_INCREMENT,
    `domain_id`  bigint(20)   DEFAULT NULL,
    `type`       varchar(20)  DEFAULT NULL COMMENT 'datasource、dimension、metric',
    `config`     text COMMENT 'config detail',
    `created_at` datetime     DEFAULT NULL,
    `created_by` varchar(100) DEFAULT NULL,
    `updated_at` datetime     DEFAULT NULL,
    `updated_by` varchar(100) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS s2_user
(
    id       int(11) NOT NULL AUTO_INCREMENT,
    name     varchar(100) not null,
    display_name varchar(100) null,
    password varchar(256) null,
    salt varchar(256) DEFAULT NULL COMMENT 'md5密码盐',
    email varchar(100) null,
    is_admin tinyint null,
    UNIQUE (`name`),
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS s2_system_config
(
    id  int primary key AUTO_INCREMENT COMMENT '主键id',
    admin varchar(500) COMMENT '系统管理员',
    parameters text null COMMENT '配置项'
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS s2_model_rela
(
    id             bigint primary key AUTO_INCREMENT,
    domain_id       bigint,
    from_model_id    bigint,
    to_model_id      bigint,
    join_type       VARCHAR(255),
    join_condition  VARCHAR(255)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `s2_collect` (
    `id` bigint NOT NULL primary key AUTO_INCREMENT,
    `type` varchar(20) NOT NULL,
    `username` varchar(20) NOT NULL,
    `collect_id` bigint NOT NULL,
    `create_time` datetime,
    `update_time` datetime
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `s2_metric_query_default_config` (
    `id` bigint NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `metric_id` bigint,
    `user_name` varchar(255) NOT NULL,
    `default_config` varchar(1000) NOT NULL,
    `created_at` datetime null,
    `updated_at` datetime null,
    `created_by` varchar(100) null,
    `updated_by` varchar(100) null
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `s2_app`
(
    id          bigint PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(255),
    description VARCHAR(255),
    status      INT,
    config      TEXT,
    end_date    datetime,
    qps         INT,
    app_secret  VARCHAR(255),
    owner       VARCHAR(255),
    `created_at`     datetime null,
    `updated_at`     datetime null,
    `created_by`     varchar(255) null,
    `updated_by`     varchar(255) null
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS s2_data_set
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    domain_id   BIGINT,
    `name`      VARCHAR(255),
    biz_name    VARCHAR(255),
    `description` VARCHAR(255),
    `status`      INT,
    alias       VARCHAR(255),
    data_set_detail text,
    created_at  datetime,
    created_by  VARCHAR(255),
    updated_at  datetime,
    updated_by  VARCHAR(255),
    query_config VARCHAR(3000),
    `admin` varchar(3000) DEFAULT NULL,
    `admin_org` varchar(3000) DEFAULT NULL
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS s2_tag(
   `id` INT NOT NULL  AUTO_INCREMENT,
   `item_id` INT  NOT NULL ,
   `type` varchar(255)  NOT NULL ,
   `created_at` datetime NOT NULL ,
   `created_by` varchar(100) NOT NULL ,
   `updated_at` datetime DEFAULT NULL ,
   `updated_by` varchar(100) DEFAULT NULL ,
   `ext` text DEFAULT NULL  ,
   PRIMARY KEY (`id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `s2_tag_object`
(
    `id`                bigint(20)   NOT NULL AUTO_INCREMENT,
    `domain_id`         bigint(20)   DEFAULT NULL,
    `name`              varchar(255) NOT NULL COMMENT '名称',
    `biz_name`          varchar(255) NOT NULL COMMENT '英文名称',
    `description`       varchar(500) DEFAULT NULL COMMENT '描述',
    `status`            tinyint NOT NULL DEFAULT '1' COMMENT '状态',
    `sensitive_level`   tinyint NOT NULL DEFAULT '0' COMMENT '敏感级别',
    `created_at`        datetime     NOT NULL COMMENT '创建时间',
    `created_by`        varchar(100) NOT NULL COMMENT '创建人',
    `updated_at`        datetime      NULL COMMENT '更新时间',
    `updated_by`        varchar(100)  NULL COMMENT '更新人',
    `ext`               text DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
DEFAULT CHARSET = utf8 COMMENT ='标签对象表';

CREATE TABLE IF NOT EXISTS `s2_query_rule` (
    `id` bigint(20)   NOT NULL AUTO_INCREMENT,
    `data_set_id` bigint(20) ,
    `priority` int(10) NOT NULL DEFAULT '1' ,
    `rule_type` varchar(255)  NOT NULL ,
    `name` varchar(255)  NOT NULL ,
    `biz_name` varchar(255)  NOT NULL ,
    `description` varchar(500) DEFAULT NULL ,
    `rule` text DEFAULT NULL  ,
    `action` text DEFAULT NULL  ,
    `status` INT  NOT NULL DEFAULT '1' ,
    `created_at` datetime NOT NULL ,
    `created_by` varchar(100) NOT NULL ,
    `updated_at` datetime DEFAULT NULL ,
    `updated_by` varchar(100) DEFAULT NULL ,
    `ext` text DEFAULT NULL  ,
    PRIMARY KEY (`id`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8 COMMENT ='查询规则表';

CREATE TABLE IF NOT EXISTS `s2_term` (
    `id` bigint(20) NOT NULL  AUTO_INCREMENT,
    `domain_id` bigint(20),
    `name` varchar(255)  NOT NULL ,
    `description` varchar(500) DEFAULT NULL ,
    `alias` varchar(1000)  NOT NULL ,
    `related_metrics` varchar(1000)  DEFAULT NULL ,
    `related_dimensions` varchar(1000)  DEFAULT NULL,
    `created_at` datetime NOT NULL ,
    `created_by` varchar(100) NOT NULL ,
    `updated_at` datetime DEFAULT NULL ,
    `updated_by` varchar(100) DEFAULT NULL ,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8 COMMENT ='术语表';

CREATE TABLE IF NOT EXISTS `s2_user_token` (
     `id` bigint NOT NULL AUTO_INCREMENT,
     `name` VARCHAR(255) NOT NULL,
     `user_name` VARCHAR(255)  NOT NULL,
     `expire_time` BIGINT(20) NOT NULL,
     `token` text NOT NULL,
     `salt` VARCHAR(255)  default NULL,
     `create_time` DATETIME NOT NULL,
     `create_by` VARCHAR(255) NOT NULL,
     `update_time` DATETIME default NULL,
     `update_by` VARCHAR(255) NOT NULL,
     `expire_date_time` DATETIME NOT NULL,
     unique key name_username (`name`, `user_name`),
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin comment='用户令牌信息表';
