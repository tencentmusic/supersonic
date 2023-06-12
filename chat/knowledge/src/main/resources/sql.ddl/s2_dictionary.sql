CREATE TABLE IF NOT EXISTS `s2_dictionary` (
`id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
`item_id` bigint(20) DEFAULT NULL COMMENT '对应维度id、指标id等',
`type` varchar(50) DEFAULT NULL COMMENT '对应维度、指标等',
`black_list` mediumtext COMMENT '字典黑名单',
`white_list` mediumtext COMMENT '字典白名单',
`rule_list` mediumtext COMMENT '字典规则',
`is_dict_Info` tinyint(1) NOT NULL DEFAULT '0' COMMENT '1-开启写入字典，0-不开启',
`created_at` datetime  NOT NULL COMMENT '创建时间',
`updated_at` datetime  NOT NULL COMMENT '更新时间',
`created_by` varchar(100) NOT NULL COMMENT '创建人',
`updated_by` varchar(100) DEFAULT NULL COMMENT '更新人',
`is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '1-删除,0-可用',
  PRIMARY KEY (`id`)
) COMMENT='字典配置信息表'
