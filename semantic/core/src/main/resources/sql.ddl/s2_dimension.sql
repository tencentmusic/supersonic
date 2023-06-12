CREATE TABLE IF NOT EXISTS `s2_dimension`
(
    `id` bigint
(
    20
) NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '维度ID',
    `domain_id` bigint
(
    20
) NOT NULL COMMENT '主题域id',
    `datasource_id` bigint
(
    20
) NOT NULL COMMENT '所属数据源id',
    `name` varchar
(
    255
) NOT NULL COMMENT '维度名称',
    `biz_name` varchar
(
    255
) NOT NULL COMMENT '字段名称',
    `description` varchar
(
    500
) NOT NULL COMMENT '描述',
    `status` int
(
    10
) NOT NULL COMMENT '维度状态,0正常,1下架,2删除',
    `sensitive_level` int
(
    10
) DEFAULT NULL COMMENT '敏感级别',
    `type` varchar
(
    50
) NOT NULL COMMENT '维度类型 categorical,time',
    `type_params` text NOT NULL COMMENT '类型参数',
    `expr` text NOT NULL COMMENT '表达式',
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
    `semantic_type` varchar
(
    20
) NOT NULL COMMENT '语义类型DATE, ID, CATEGORY'
    ) comment '维度表'
