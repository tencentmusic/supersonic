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
