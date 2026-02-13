-- ============================================================
-- Phase 2: Report Schedule, Execution, Export Task, Data Sync
-- ============================================================

-- Report schedule configuration
CREATE TABLE IF NOT EXISTS `s2_report_schedule` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(200) NOT NULL COMMENT '调度任务名称',
    `dataset_id` bigint NOT NULL COMMENT '关联 DataSet',
    `query_config` text COMMENT 'JSON: QueryStructReq 模板',
    `output_format` varchar(20) DEFAULT 'EXCEL' COMMENT '输出格式: EXCEL/CSV/JSON',
    `cron_expression` varchar(100) NOT NULL COMMENT 'Cron 表达式',
    `enabled` tinyint DEFAULT 1 COMMENT '是否启用',
    `owner_id` bigint COMMENT '权限归属用户',
    `retry_count` int DEFAULT 3 COMMENT '最大重试次数',
    `retry_interval` int DEFAULT 30 COMMENT '重试间隔基数(秒)',
    `template_version` bigint COMMENT '绑定的模板版本',
    `delivery_config_ids` varchar(500) COMMENT '关联推送渠道配置ID(逗号分隔)',
    `quartz_job_key` varchar(200) COMMENT 'Quartz Job 标识',
    `last_execution_time` datetime DEFAULT NULL,
    `next_execution_time` datetime DEFAULT NULL,
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` varchar(100) DEFAULT NULL,
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_report_schedule_tenant` (`tenant_id`),
    KEY `idx_report_schedule_dataset` (`dataset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表调度配置';

-- Report execution records (audit log)
CREATE TABLE IF NOT EXISTS `s2_report_execution` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `schedule_id` bigint COMMENT '关联调度任务',
    `attempt` int DEFAULT 1 COMMENT '当前执行次数',
    `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    `start_time` datetime DEFAULT NULL,
    `end_time` datetime DEFAULT NULL,
    `result_location` varchar(500) COMMENT '结果文件路径',
    `error_message` varchar(2000) DEFAULT NULL,
    `row_count` bigint DEFAULT NULL COMMENT '返回数据量',
    `sql_hash` varchar(64) DEFAULT NULL COMMENT '执行 SQL 的 hash',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `execution_snapshot` text COMMENT 'JSON: 完整的 ReportExecutionContext 快照',
    `template_version` bigint COMMENT '执行时的模板版本号',
    `engine_version` varchar(50) COMMENT '执行时的系统版本号',
    `scan_rows` bigint COMMENT '预估/实际扫描行数',
    `execution_time_ms` bigint COMMENT '查询执行耗时(毫秒)',
    `io_bytes` bigint COMMENT 'IO 读取字节数',
    PRIMARY KEY (`id`),
    KEY `idx_report_execution_schedule` (`schedule_id`),
    KEY `idx_report_execution_tenant` (`tenant_id`),
    KEY `idx_report_execution_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表执行记录';

-- Export task (async export)
CREATE TABLE IF NOT EXISTS `s2_export_task` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `task_name` varchar(200) COMMENT '任务名称',
    `user_id` bigint NOT NULL COMMENT '发起用户',
    `dataset_id` bigint COMMENT '关联数据集',
    `query_config` text COMMENT 'JSON: 完整的 QueryStructReq',
    `output_format` varchar(20) DEFAULT 'EXCEL' COMMENT 'EXCEL/CSV',
    `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED/EXPIRED',
    `file_location` varchar(500) COMMENT '结果文件路径',
    `file_size` bigint COMMENT '文件大小(字节)',
    `row_count` bigint COMMENT '导出行数',
    `error_message` varchar(2000) DEFAULT NULL,
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `expire_time` datetime COMMENT '文件过期时间',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_export_task_user` (`user_id`),
    KEY `idx_export_task_tenant` (`tenant_id`),
    KEY `idx_export_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导出任务';

-- Data sync configuration
CREATE TABLE IF NOT EXISTS `s2_data_sync_config` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(200) NOT NULL COMMENT '任务名称',
    `source_database_id` bigint NOT NULL COMMENT '源数据源(关联 s2_database)',
    `target_database_id` bigint NOT NULL COMMENT '目标数据源(关联 s2_database)',
    `sync_config` text COMMENT 'JSON: 同步规则(tables, channel_count等)',
    `cron_expression` varchar(100) NOT NULL COMMENT 'Cron 表达式',
    `retry_count` int DEFAULT 3 COMMENT '最大重试次数',
    `enabled` tinyint DEFAULT 1 COMMENT '是否启用',
    `quartz_job_key` varchar(200) COMMENT 'Quartz Job 标识',
    `created_by` varchar(100) DEFAULT NULL,
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_data_sync_config_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据同步配置';

-- Data sync execution records
CREATE TABLE IF NOT EXISTS `s2_data_sync_execution` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `sync_config_id` bigint NOT NULL COMMENT '关联同步配置',
    `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    `start_time` datetime DEFAULT NULL,
    `end_time` datetime DEFAULT NULL,
    `rows_read` bigint DEFAULT NULL COMMENT '读取行数',
    `rows_written` bigint DEFAULT NULL COMMENT '写入行数',
    `watermark_value` varchar(200) COMMENT '本次同步水位线值',
    `error_message` varchar(2000) DEFAULT NULL,
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_data_sync_execution_config` (`sync_config_id`),
    KEY `idx_data_sync_execution_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据同步执行记录';
