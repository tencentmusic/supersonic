alter table s2_domain add column `entity`varchar(500) DEFAULT NULL COMMENT '主题域实体信息';


--20230808
alter table s2_domain drop column entity;

create table s2_model
(
    id         bigint auto_increment
        primary key,
    name       varchar(100)                null,
    biz_name   varchar(100) null,
    domain_id  bigint                      null,
    viewer     varchar(500) null,
    view_org   varchar(500)  null,
    admin      varchar(500) null,
    admin_org  varchar(500)  null,
    is_open    int                         null,
    created_by varchar(100) null,
    created_at datetime                    null,
    updated_by varchar(100)  null,
    updated_at datetime                    null,
    entity     text         null
) collate = utf8_unicode_ci;

alter table s2_datasource change column domain_id model_id bigint;
alter table s2_dimension change column domain_id model_id bigint;
alter table s2_metric change column domain_id model_id bigint;
alter table s2_datasource_rela change column domain_id model_id bigint;
alter table s2_view_info change column domain_id model_id bigint;
alter table s2_domain_extend change column domain_id model_id bigint;
alter table s2_chat_config change column domain_id model_id bigint;
alter table s2_plugin change column domain model varchar(100);
alter table s2_query_stat_info change column domain_id model_id bigint;

update s2_plugin set config = replace(config, 'domain', 'model');

--20230823
alter table s2_chat_query add column agent_id int after question_id;
alter table s2_chat_query change column query_response query_result mediumtext;

--20230829
alter table s2_database add column admin varchar(500);
alter table s2_database add column viewer varchar(500);
alter table s2_database drop column domain_id;

--20230831
alter table s2_chat add column agent_id int after chat_id;

--20230907
ALTER TABLE s2_model add alias varchar(200) default null after domain_id;

--20230919
alter table s2_metric add tags varchar(500) null;

--20230920
alter table s2_user add is_admin int null;

--20230926
alter table s2_model add drill_down_dimensions varchar(500) null;
alter table s2_metric add relate_dimensions varchar(500) null;


--20231013
alter table s2_dimension add column data_type  varchar(50)  not null DEFAULT 'varchar' comment '维度数据类型 varchar、array';
alter table s2_query_stat_info add column `query_opt_mode` varchar(20) DEFAULT NULL COMMENT '优化模式';
alter table s2_datasource add column depends text COMMENT '上游依赖标识' after datasource_detail;

--20231018
UPDATE `s2_agent` SET `config` = replace (`config`,'DSL','LLM_S2QL') WHERE `config` LIKE '%DSL%';

--20231023
alter table s2_model add column status int null after alias;
alter table s2_model add column description varchar(500) null after status;
alter table s2_datasource add column status int null after database_id;
update s2_model set status = 1;
update s2_datasource set status = 1;
update s2_metric set status = 1;
update s2_dimension set status = 1;

--20231110
UPDATE `s2_agent` SET `config` = replace (`config`,'LLM_S2QL','LLM_S2SQL') WHERE `config` LIKE '%LLM_S2QL%';

--20231113
CREATE TABLE s2_sys_parameter
(
    id  int primary key AUTO_INCREMENT COMMENT '主键id',
    admin varchar(500) COMMENT '系统管理员',
    parameters text null COMMENT '配置项'
);

--20231114
alter table s2_chat_config add column `llm_examples` text COMMENT 'llm examples';

--20231116
alter table s2_datasource add column `filter_sql` varchar(1000) COMMENT 'filter_sql' after depends;

--20231120
alter table s2_dimension add column `is_tag` int(10) DEFAULT NULL;

--20231125
alter table s2_model add column `database_id` INT NOT NULL;
alter table s2_model add column `model_detail` text NOT  NULL;
alter table s2_model add column `depends` varchar(500) DEFAULT NULL;
alter table s2_model add column `filter_sql` varchar(1000) DEFAULT NULL;

CREATE TABLE s2_model_rela
(
    id             BIGINT AUTO_INCREMENT,
    domain_id       BIGINT,
    from_model_id    BIGINT,
    to_model_id      BIGINT,
    join_type       VARCHAR(255),
    join_condition  VARCHAR(255),
    PRIMARY KEY (`id`)
);

alter table s2_view_info change model_id domain_id bigint;
alter table s2_dimension drop column datasource_id;

-- 20231211
CREATE TABLE `s2_collect`
(
    `id`          bigint      NOT NULL primary key AUTO_INCREMENT,
    `type`        varchar(20) NOT NULL,
    `username`    varchar(20) NOT NULL,
    `collect_id`  bigint      NOT NULL,
    `create_time` datetime,
    `update_time` datetime
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

alter table s2_metric add column `ext` text DEFAULT NULL;

CREATE TABLE `s2_metric_query_default_config`
(
    `id`             bigint  NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `metric_id`      bigint,
    `user_name`      varchar(255)  NOT NULL,
    `default_config` varchar(1000) NOT NULL,
    `created_at`     datetime null,
    `updated_at`     datetime null,
    `created_by`     varchar(100) null,
    `updated_by`     varchar(100) null
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--20231214
alter table s2_chat_query add column `similar_queries` varchar(1024) DEFAULT '';
alter table s2_model add column `source_type` varchar(128) DEFAULT NULL;


CREATE TABLE `s2_app`
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


--20240115
alter table s2_metric add column `define_type` varchar(50)  DEFAULT NULL; -- MEASURE, FIELD, METRIC
update s2_metric set define_type = 'MEASURE';

--20240129
CREATE TABLE s2_view(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    domain_id   BIGINT,
    `name`      VARCHAR(255),
    biz_name    VARCHAR(255),
    `description` VARCHAR(255),
    `status`      INT,
    alias       VARCHAR(255),
    view_detail text,
    created_at  datetime,
    created_by  VARCHAR(255),
    updated_at  datetime,
    updated_by  VARCHAR(255),
    query_config VARCHAR(3000),
    `admin` varchar(3000) DEFAULT NULL,
    `admin_org` varchar(3000) DEFAULT NULL
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

alter table s2_plugin change column model `view` varchar(100);
alter table s2_view_info rename to s2_canvas;

alter table s2_query_stat_info add column `view_id` bigint(20) DEFAULT NULL after `model_id`;

--20240301
CREATE TABLE IF NOT EXISTS `s2_dictionary_conf` (
   `id` INT NOT NULL AUTO_INCREMENT,
   `description` varchar(255) ,
   `type` varchar(255)  NOT NULL ,
   `item_id` INT  NOT NULL ,
   `config` text  ,
   `status` varchar(255) NOT NULL ,
   `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP  ,
   `created_by` varchar(100) NOT NULL ,
   PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_dictionary_conf IS 'dictionary conf information table';

CREATE TABLE IF NOT EXISTS `s2_dictionary_task` (
   `id` INT NOT NULL AUTO_INCREMENT,
   `name` varchar(255) NOT NULL ,
   `description` varchar(255) ,
   `type` varchar(255)  NOT NULL ,
   `item_id` INT  NOT NULL ,
   `config` text  ,
   `status` varchar(255) NOT NULL ,
   `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP  ,
   `created_by` varchar(100) NOT NULL ,
   `elapsed_ms` bigINT DEFAULT NULL ,
   PRIMARY KEY (`id`)
);
COMMENT ON TABLE s2_dictionary_task IS 'dictionary task information table';


--20240229
alter table s2_view rename to s2_data_set;
alter table s2_query_stat_info change view_id data_set_id bigint;
alter table s2_plugin change `view` data_set varchar(200);
alter table s2_data_set change view_detail data_set_detail text;

--20240311
alter table s2_data_set add column query_type varchar(100) DEFAULT NULL;

--20240319
CREATE TABLE IF NOT EXISTS `s2_tag_object`
(
    `id`                bigint(20)   NOT NULL AUTO_INCREMENT,
    `domain_id`         bigint(20)   DEFAULT NULL,
    `name`              varchar(255) NOT NULL COMMENT '名称',
    `biz_name`          varchar(255) NOT NULL COMMENT '英文名称',
    `description`       varchar(500) DEFAULT NULL COMMENT '描述',
    `status`            int(10) NOT NULL DEFAULT '1' COMMENT '状态',
    `sensitive_level`   int(10) NOT NULL DEFAULT '0' COMMENT '敏感级别',
    `created_at`        datetime     NOT NULL COMMENT '创建时间',
    `created_by`        varchar(100) NOT NULL COMMENT '创建人',
    `updated_at`        datetime      NULL COMMENT '更新时间',
    `updated_by`        varchar(100)  NULL COMMENT '更新人',
    `ext`               text DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
DEFAULT CHARSET = utf8 COMMENT ='标签表对象';

alter table s2_model add column `tag_object_id` bigint(20) DEFAULT NULL after domain_id;

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

--20240321
CREATE TABLE IF NOT EXISTS `s2_query_rule` (
    `id` INT NOT NULL  AUTO_INCREMENT,
    `data_set_id` INT ,
    `priority` INT  NOT NULL DEFAULT '1' ,
    `rule_type` varchar(255)  NOT NULL ,
    `name` varchar(255)  NOT NULL ,
    `biz_name` varchar(255)  NOT NULL ,
    `description` varchar(500) DEFAULT NULL ,
    `rule` LONGVARCHAR DEFAULT NULL  ,
    `action` LONGVARCHAR DEFAULT NULL  ,
    `status` INT  NOT NULL DEFAULT '1' ,
    `created_at` TIMESTAMP NOT NULL ,
    `created_by` varchar(100) NOT NULL ,
    `updated_at` TIMESTAMP DEFAULT NULL ,
    `updated_by` varchar(100) DEFAULT NULL ,
    `ext` LONGVARCHAR DEFAULT NULL  ,
    PRIMARY KEY (`id`)
    );
COMMENT ON TABLE s2_query_rule IS 'tag query rule table';

--20240325
alter table s2_metric  change tags classifications varchar(500) null;
alter table s2_metric  add column `is_publish` int(10) DEFAULT NULL COMMENT '是否发布';
update s2_metric set is_publish = 1;

--20240402
alter table s2_dimension add column `ext` varchar(1000) DEFAULT NULL;

--20240510
CREATE TABLE IF NOT EXISTS `s2_term` (
    `id` bigint(20) NOT NULL  AUTO_INCREMENT,
    `domain_id` bigint(20),
    `name` varchar(255)  NOT NULL ,
    `description` varchar(500) DEFAULT NULL ,
    `alias` varchar(1000)  NOT NULL ,
    `created_at` datetime NOT NULL ,
    `created_by` varchar(100) NOT NULL ,
    `updated_at` datetime DEFAULT NULL ,
    `updated_by` varchar(100) DEFAULT NULL ,
    PRIMARY KEY (`id`)
);

--20240520
alter table s2_agent add column `llm_config` varchar(2000) COLLATE utf8_unicode_ci DEFAULT NULL;
alter table s2_agent add column `multi_turn_config` varchar(2000) COLLATE utf8_unicode_ci DEFAULT NULL;

alter table s2_model add column `ext` varchar(1000) DEFAULT NULL;

--20240601
alter table s2_sys_parameter rename to s2_system_config;

--20240603
alter table s2_chat_query add column `parse_time_cost` varchar(1024);

--20240609
alter table s2_user add column `salt` varchar(256) DEFAULT NULL COMMENT 'md5密码盐';

--20240621
alter table s2_agent add column `visual_config` varchar(2000)  COLLATE utf8_unicode_ci DEFAULT NULL COMMENT '可视化配置';

alter table s2_term add column `related_metrics` varchar(1000)  DEFAULT NULL  COMMENT '术语关联的指标';
alter table s2_term add column `related_dimensions` varchar(1000)  DEFAULT NULL  COMMENT '术语关联的维度';
