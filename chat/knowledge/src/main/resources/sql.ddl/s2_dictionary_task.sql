CREATE TABLE IF NOT EXISTS `s2_dictionary_task` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL COMMENT '任务名称',
  `description` varchar(255) NOT NULL COMMENT '任务描述',
  `command` mediumtext  NOT NULL COMMENT '任务请求参数',
  `dimension_ids` mediumtext  NULL COMMENT '本次执行维度列表',
  `status` int(10) NOT NULL COMMENT '任务最终运行状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` varchar(100) NOT NULL COMMENT '创建人',
  `elapsed_ms` bigint(10) DEFAULT NULL COMMENT '任务耗时',
  `message` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '备注相关信息',
  PRIMARY KEY (`id`)
)COMMENT='字典任务信息表'
