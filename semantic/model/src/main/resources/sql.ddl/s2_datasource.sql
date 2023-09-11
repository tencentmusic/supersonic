CREATE TABLE IF NOT EXISTS `s2_datasource`
(
    `id` bigint
(
    20
) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `domain_id` bigint
(
    20
) NOT NULL COMMENT '主题域ID',
    `name` varchar
(
    255
) NOT NULL COMMENT '数据源名称',
    `biz_name` varchar
(
    255
) NOT NULL COMMENT '内部名称',
    `description` varchar
(
    500
) DEFAULT NULL COMMENT '数据源描述',
    `database_id` int
(
    10
) NOT NULL COMMENT '数据库实例ID',
    `datasource_detail` mediumtext NOT NULL COMMENT '数据源配置',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `created_by` varchar
(
    100
) NOT NULL COMMENT '创建人',
    `updated_at` datetime NOT NULL COMMENT '更新时间',
    `updated_by` varchar
(
    100
) NOT NULL COMMENT '更新人'
    ) comment '数据源表'