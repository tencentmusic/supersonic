create table supersonic_sit.s2_auth_groups
(
    group_id int           not null
        primary key,
    config   varchar(2048) null
)
    collate = utf8mb4_unicode_ci;

create table supersonic_sit.s2_available_date_info
(
    id               int auto_increment
        primary key,
    item_id          int                                     not null,
    type             varchar(255)                            not null,
    date_format      varchar(64)                             not null,
    start_date       varchar(64)                             null,
    end_date         varchar(64)                             null,
    unavailable_date text                                    null,
    created_at       timestamp default CURRENT_TIMESTAMP     not null,
    created_by       varchar(100)                            not null,
    updated_at       timestamp default CURRENT_TIMESTAMP     not null on update CURRENT_TIMESTAMP,
    updated_by       varchar(100)                            not null,
    status           int       default 0                     null
)
    collate = utf8mb4_unicode_ci;

create table supersonic_sit.s2_chat
(
    chat_id       bigint(8) auto_increment
        primary key,
    chat_name     varchar(100)     null,
    create_time   datetime         null,
    last_time     datetime         null,
    creator       varchar(30)      null,
    last_question varchar(200)     null,
    is_delete     int(2) default 0 null comment 'is deleted',
    is_top        int(2) default 0 null comment 'is top'
)
    charset = utf8;

create table supersonic_sit.s2_chat_config
(
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `domain_id` bigint(20) DEFAULT NULL COMMENT '主题域id',
  `chat_detail_config` mediumtext COMMENT '明细模式配置信息',
  `chat_agg_config` mediumtext COMMENT '指标模式配置信息',
  `recommended_questions`  mediumtext COMMENT '推荐问题配置',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  `created_by` varchar(100) NOT NULL COMMENT '创建人',
  `updated_by` varchar(100) NOT NULL COMMENT '更新人',
  `status` int(10) NOT NULL COMMENT '主题域扩展信息状态, 0-删除，1-生效',
  PRIMARY KEY (`id`)
)
    comment '主题域扩展信息表' charset = utf8;

create table supersonic_sit.s2_chat_context
(
    chat_id        bigint                             not null comment 'context chat id'
        primary key,
    modified_at    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'row modify time',
    user           varchar(64)                        null comment 'row modify user',
    query_text     text                               null comment 'query text',
    semantic_parse text                               null comment 'parse data',
    ext_data       text                               null comment 'extend data'
)
    charset = utf8;

create table supersonic_sit.s2_chat_query
(
    question_id    bigint auto_increment
        primary key,
    create_time    timestamp     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    query_text     mediumtext                              null,
    user_name      varchar(150)                            null,
    query_state    int(1)                                  null,
    chat_id        bigint                                  not null,
    query_response mediumtext                              not null,
    score          int           default 0                 null,
    feedback       varchar(1024) default ''                null
)
    charset = utf8;

create table supersonic_sit.s2_database
(
    id          bigint auto_increment
        primary key,
    domain_id   bigint       not null comment '主题域ID',
    name        varchar(255) not null comment '名称',
    description varchar(500) null comment '描述',
    version varchar(64) null comment '版本',
    type        varchar(20)  not null comment '类型 mysql,clickhouse,tdw',
    config      text         not null comment '配置信息',
    created_at  datetime     not null comment '创建时间',
    created_by  varchar(100) not null comment '创建人',
    updated_at  datetime     not null comment '更新时间',
    updated_by  varchar(100) not null comment '更新人'
)
    comment '数据库实例表' charset = utf8;

create table supersonic_sit.s2_datasource
(
    id                bigint auto_increment
        primary key,
    domain_id         bigint       not null comment '主题域ID',
    name              varchar(255) not null comment '数据源名称',
    biz_name          varchar(255) not null comment '内部名称',
    description       varchar(500) null comment '数据源描述',
    database_id       bigint       not null comment '数据库实例ID',
    datasource_detail mediumtext   not null comment '数据源配置',
    created_at        datetime     not null comment '创建时间',
    created_by        varchar(100) not null comment '创建人',
    updated_at        datetime     not null comment '更新时间',
    updated_by        varchar(100) not null comment '更新人'
)
    charset = utf8;

create table supersonic_sit.s2_datasource_rela
(
    id              bigint auto_increment
        primary key,
    domain_id       bigint       null,
    datasource_from bigint       null,
    datasource_to   bigint       null,
    join_key        varchar(100) null,
    created_at      datetime     null,
    created_by      varchar(100) null,
    updated_at      datetime     null,
    updated_by      varchar(100) null
)
    charset = utf8;


create table supersonic_sit.s2_dimension
(
    id              bigint auto_increment comment '维度ID'
        primary key,
    domain_id       bigint                               not null comment '主题域id',
    datasource_id   bigint                               not null comment '所属数据源id',
    name            varchar(255)                         not null comment '维度名称',
    biz_name        varchar(255)                         not null comment '字段名称',
    description     varchar(500)                         not null comment '描述',
    status          int(10)                              not null comment '维度状态,0正常,1下架,2删除',
    sensitive_level int(10)                              null comment '敏感级别',
    type            varchar(50)                          not null comment '维度类型 categorical,time',
    type_params     text                                 null comment '类型参数',
    expr            text                                 not null comment '表达式',
    created_at      datetime                             not null comment '创建时间',
    created_by      varchar(100)                         not null comment '创建人',
    updated_at      datetime                             not null comment '更新时间',
    updated_by      varchar(100)                         not null comment '更新人',
    semantic_type   varchar(20)                          not null comment '语义类型DATE, ID, CATEGORY',
    alias           varchar(500) collate utf8_unicode_ci null,
    default_values varchar(500) DEFAULT NULL,
    dim_value_maps varchar(500) DEFAULT NULL
)
    comment '维度表' charset = utf8;

create table supersonic_sit.s2_domain
(
    id         bigint auto_increment comment '自增ID'
        primary key,
    name       varchar(255)     null comment '主题域名称',
    biz_name   varchar(255)     null comment '内部名称',
    parent_id  bigint default 0 null comment '父主题域ID',
    status     int(10)          not null comment '主题域状态',
    created_at datetime         null comment '创建时间',
    created_by varchar(100)     null comment '创建人',
    updated_at datetime         null comment '更新时间',
    updated_by varchar(100)     null comment '更新人',
    admin      varchar(3000)    null comment '主题域管理员',
    admin_org  varchar(3000)    null comment '主题域管理员组织',
    is_open    int              null comment '主题域是否公开',
    viewer     varchar(3000)    null comment '主题域可用用户',
    view_org   varchar(3000)    null comment '主题域可用组织',
    entity     varchar(500) DEFAULT NULL COMMENT '主题域实体信息'
)
    comment '主题域基础信息表' charset = utf8;


create table supersonic_sit.s2_domain_extend
(
    id              bigint unsigned auto_increment
        primary key,
    domain_id       bigint       null comment '主题域id',
    default_metrics varchar(655) null comment '默认指标',
    visibility      mediumtext   null comment '不可见的维度/指标信息',
    entity_info     mediumtext   null comment '实体信息',
    dictionary_info mediumtext   null comment '字典相关的维度设置信息',
    created_at      datetime     not null comment '创建时间',
    updated_at      datetime     not null comment '更新时间',
    created_by      varchar(100) not null comment '创建人',
    updated_by      varchar(100) not null comment '更新人',
    status          int(10)      not null comment '主题域扩展信息状态, 0-删除，1-生效'
)
    comment '主题域扩展信息表' collate = utf8mb4_unicode_ci;


create table supersonic_sit.s2_metric
(
    id               bigint auto_increment
        primary key,
    domain_id        bigint                               not null comment '主体域ID',
    name             varchar(255)                         not null comment '指标名称',
    biz_name         varchar(255)                         not null comment '字段名称',
    description      varchar(500)                         null comment '描述',
    status           int(10)                              not null comment '指标状态,0正常,1下架,2删除',
    sensitive_level  int(10)                              not null comment '敏感级别',
    type             varchar(50)                          not null comment '指标类型 proxy,expr',
    type_params      text                                 not null comment '类型参数',
    created_at       datetime                             not null comment '创建时间',
    created_by       varchar(100)                         not null comment '创建人',
    updated_at       datetime                             not null comment '更新时间',
    updated_by       varchar(100)                         not null comment '更新人',
    data_format_type varchar(50)                          null comment '数值类型',
    data_format      varchar(500)                         null comment '数值类型参数',
    alias            varchar(500) collate utf8_unicode_ci null
)
    comment '指标表' charset = utf8;

create table supersonic_sit.s2_query_stat_info
(
    id               bigint unsigned auto_increment
        primary key,
    trace_id         varchar(200)                         null comment '查询标识',
    domain_id        bigint                               null comment '主题域ID',
    user             varchar(200)                         null comment '执行sql的用户',
    created_at       datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    query_type       varchar(200)                         null comment '查询对应的场景',
    query_type_back  int(10)    default 0                 null comment '查询类型, 0-正常查询, 1-预刷类型',
    query_sql_cmd    mediumtext                           null comment '对应查询的struct',
    sql_cmd_md5      varchar(200)                         null comment 'sql md5值',
    query_struct_cmd mediumtext                           null comment '对应查询的struct',
    struct_cmd_md5   varchar(200)                         null comment 'sql md5值',
    `sql`            mediumtext                           null comment '对应查询的sql',
    sql_md5          varchar(200)                         null comment 'sql md5值',
    query_engine     varchar(20)                          null comment '查询引擎',
    elapsed_ms       bigint(10)                           null comment '查询耗时',
    query_state      varchar(20)                          null comment '查询最终状态',
    native_query     int(10)                              null comment '1-明细查询,0-聚合查询',
    start_date       varchar(50)                          null comment 'sql开始日期',
    end_date         varchar(50)                          null comment 'sql结束日期',
    dimensions       mediumtext                           null comment 'sql 涉及的维度',
    metrics          mediumtext                           null comment 'sql 涉及的指标',
    select_cols      mediumtext                           null comment 'sql select部分涉及的标签',
    agg_cols         mediumtext                           null comment 'sql agg部分涉及的标签',
    filter_cols      mediumtext                           null comment 'sql where部分涉及的标签',
    group_by_cols    mediumtext                           null comment 'sql grouy by部分涉及的标签',
    order_by_cols    mediumtext                           null comment 'sql order by部分涉及的标签',
    use_result_cache tinyint(1) default -1                null comment '是否命中sql缓存',
    use_sql_cache    tinyint(1) default -1                null comment '是否命中sql缓存',
    sql_cache_key    mediumtext                           null comment '缓存的key',
    result_cache_key mediumtext                           null comment '缓存的key'
)
 comment '查询统计信息表' collate = utf8mb4_unicode_ci;

create index domain_index
    on supersonic_sit.s2_query_stat_info (domain_id);

create table supersonic_sit.s2_semantic_pasre_info
(
    id           bigint unsigned auto_increment
        primary key,
    trace_id     varchar(200)         not null comment '查询标识',
    domain_id    bigint               not null comment '主体域ID',
    dimensions   mediumtext           null comment '查询相关的维度信息',
    metrics      mediumtext           null comment '查询相关的指标信息',
    orders       mediumtext           null comment '查询相关的排序信息',
    filters      mediumtext           null comment '查询相关的过滤信息',
    date_info    mediumtext           null comment '查询相关的日期信息',
    `limit`      bigint               not null comment '查询相关的limit信息',
    native_query tinyint(1) default 0 not null comment '1-明细查询,0-聚合查询',
    `sql`        mediumtext           null comment '解析后的sql',
    created_at   datetime             not null comment '创建时间',
    created_by   varchar(100)         not null comment '创建人',
    status       int(10)              not null comment '运行状态',
    elapsed_ms   bigint(10)           null comment 'sql解析耗时'
) comment '语义层sql解析信息表' charset = utf8;

create table supersonic_sit.s2_view_info
(
    id         bigint auto_increment
        primary key,
    domain_id  bigint       null,
    type       varchar(20)  null comment 'datasource、dimension、metric',
    config     text         null comment 'config detail',
    created_at datetime     null,
    created_by varchar(100) null,
    updated_at datetime     null,
    updated_by varchar(100) not null
) charset = utf8;


CREATE TABLE `s2_user` (
                           `id` int(11) NOT NULL AUTO_INCREMENT,
                           `name` varchar(100) NOT NULL,
                           `display_name` varchar(100) DEFAULT NULL,
                           `password` varchar(100) DEFAULT NULL,
                           `email` varchar(100) DEFAULT NULL,
                           PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;



CREATE TABLE s2_chat_statistics (
        `question_id`             BIGINT  NOT NULL,
        `chat_id`           BIGINT NOT NULL ,
        `user_name`         varchar(150)  DEFAULT NULL COMMENT '',
        `query_text`          varchar(200),
        `interface_name`         varchar(100)  DEFAULT NULL COMMENT '',
        `cost` INT(6) DEFAULT 0 ,
        `type` INT DEFAULT NULL ,
        `create_time`       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
         index `commonIndex` (`question_id`)
);

CREATE TABLE `s2_chat_parse` (
       `question_id` bigint(20) NOT NULL,
       `chat_id` bigint(20) NOT NULL,
       `parse_id` int(11) NOT NULL,
       `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       `query_text` varchar(500)  DEFAULT NULL,
       `user_name` varchar(150)  DEFAULT NULL,
       `parse_info` mediumtext NOT NULL,
       `is_candidate` int DEFAULT 1 COMMENT '1是candidate,0是selected',
        index `commonIndex` (`question_id`)
)
