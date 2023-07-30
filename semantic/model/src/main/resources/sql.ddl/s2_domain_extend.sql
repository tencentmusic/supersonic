CREATE TABLE IF NOT EXISTS `s2_chat_config`
(
    `id` bigint
(
    20
) unsigned NOT NULL AUTO_INCREMENT,
    `domain_id` bigint
(
    20
) DEFAULT NULL COMMENT '主题域id',
    `default_metrics` varchar
(
    655
) DEFAULT NULL COMMENT '默认指标',
    `visibility` mediumtext COMMENT '不可见的维度/指标信息',
    `entity_info` mediumtext COMMENT '实体信息',
    `dictionary_info` mediumtext COMMENT '字典相关的维度设置信息',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `updated_at` datetime NOT NULL COMMENT '更新时间',
    `created_by` varchar
(
    100
) NOT NULL COMMENT '创建人',
    `updated_by` varchar
(
    100
) NOT NULL COMMENT '更新人',
    `status` int
(
    10
) NOT NULL COMMENT '主题域扩展信息状态, 0-删除，1-生效',
    PRIMARY KEY
(
    `id`
)
    ) COMMENT='主题域扩展信息表'