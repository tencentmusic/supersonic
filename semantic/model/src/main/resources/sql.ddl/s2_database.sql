CREATE TABLE `s2_database`
(
    `id`          bigint(20) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `domain_id`   bigint(20) NOT NULL COMMENT '主题域ID',
    `name`        varchar(255) NOT NULL COMMENT '名称',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `version` varchar(64) DEFAULT NULL COMMENT '版本',
    `type`        varchar(20)  NOT NULL COMMENT '类型 mysql,clickhouse,tdw',
    `config`      text         NOT NULL COMMENT '配置信息',
    `created_at`  datetime     NOT NULL COMMENT '创建时间',
    `created_by`  varchar(100) NOT NULL COMMENT '创建人',
    `updated_at`  datetime     NOT NULL COMMENT '更新时间',
    `updated_by`  varchar(100) NOT NULL COMMENT '更新人'
) comment '数据库实例表'
