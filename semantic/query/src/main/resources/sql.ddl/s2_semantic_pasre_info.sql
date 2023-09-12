CREATE TABLE IF NOT EXISTS `s2_semantic_pasre_info` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `trace_id` varchar(200) NOT NULL COMMENT '查询标识' ,
    `domain_id` bigint(20)  NOT NULL COMMENT '主题域ID',
    `dimensions` mediumtext COMMENT '查询相关的维度信息',
    `metrics` mediumtext COMMENT '查询相关的指标信息',
    `orders` mediumtext COMMENT '查询相关的排序信息',
    `filters` mediumtext COMMENT '查询相关的过滤信息',
    `date_info` mediumtext COMMENT '查询相关的日期信息',
    `limit` bigint(20) NOT NULL COMMENT'查询相关的limit信息',
    `native_query` tinyint(1) NOT NULL DEFAULT '0' COMMENT '1-明细查询,0-聚合查询',
    `sql` mediumtext COMMENT '解析后的sql',
    `created_at` datetime  NOT NULL COMMENT '创建时间',
    `created_by` varchar(100) NOT NULL COMMENT '创建人',
    `status` int(10) NOT NULL COMMENT '运行状态',
    `elapsed_ms` bigint(10) DEFAULT NULL COMMENT 'sql解析耗时',
  PRIMARY KEY (`id`)
)COMMENT='语义层sql解析信息表'
