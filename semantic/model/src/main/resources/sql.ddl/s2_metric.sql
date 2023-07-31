CREATE TABLE IF NOT EXISTS `s2_metric`
(
    `id` bigint
(
    20
) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `domain_id` bigint
(
    20
) NOT NULL COMMENT '主体域ID',
    `name` varchar
(
    255
) NOT NULL COMMENT '指标名称',
    `biz_name` varchar
(
    255
) NOT NULL COMMENT '字段名称',
    `description` varchar
(
    500
) DEFAULT NULL COMMENT '描述',
    `status` int
(
    10
) NOT NULL COMMENT '指标状态,0正常,1下架,2删除',
    `sensitive_level` int
(
    10
) NOT NULL COMMENT '敏感级别',
    `type` varchar
(
    50
) NOT NULL COMMENT '指标类型 proxy,expr',
    `type_params` text NOT NULL COMMENT '类型参数',
    `created_at` datetime NOT NULL COMMENT '创建时间',
    `created_by` varchar
(
    100
) NOT NULL COMMENT '创建人',
    `updated_at` datetime NOT NULL COMMENT '更新时间',
    `updated_by` varchar
(
    100
) NOT NULL COMMENT '更新人',
    `data_format_type` varchar
(
    50
) NOT NULL COMMENT '数值类型',
    `data_format` varchar
(
    500
) NOT NULL COMMENT '数值类型参数'
    ) comment '指标表'