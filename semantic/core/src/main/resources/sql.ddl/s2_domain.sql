CREATE TABLE IF NOT EXISTS `s2_domain`
(
    `id` bigint
(
    20
) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `name` varchar
(
    255
) DEFAULT NULL COMMENT '主题域名称',
    `biz_name` varchar
(
    255
) DEFAULT NULL COMMENT '内部名称',
    `parent_id` bigint
(
    20
) DEFAULT '0' COMMENT '父主题域ID',
    `status` int
(
    10
) NOT NULL COMMENT '主题域状态',
    `created_at` datetime DEFAULT NULL COMMENT '创建时间',
    `created_by` varchar
(
    100
) DEFAULT NULL COMMENT '创建人',
    `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
    `updated_by` varchar
(
    100
) DEFAULT NULL COMMENT '更新人',
    `is_unique` int
(
    10
) DEFAULT NULL COMMENT '0为非唯一,1为唯一',
    `admin` varchar
(
    3000
) DEFAULT NULL COMMENT '主题域管理员',
    `admin_org` varchar
(
    3000
) DEFAULT NULL COMMENT '主题域管理员组织',
    `is_open` tinyint
(
    1
) DEFAULT NULL COMMENT '主题域是否公开',
    `viewer` varchar
(
    3000
) DEFAULT NULL COMMENT '主题域可用用户',
    `view_org` varchar
(
    3000
) DEFAULT NULL COMMENT '主题域可用组织',
    PRIMARY KEY
(
    `id`
)
    ) COMMENT='主题域基础信息表'