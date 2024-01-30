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
    updated_by  VARCHAR(255)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

alter table s2_plugin change column model `view` varchar(100);
alter table s2_view_info rename to s2_canvas;

alter table s2_query_stat_info add column `view_id` bigint(20) DEFAULT NULL after `model_id`;