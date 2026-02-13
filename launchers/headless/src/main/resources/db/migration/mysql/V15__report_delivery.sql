-- Report delivery configuration table
CREATE TABLE IF NOT EXISTS s2_report_delivery_config (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(200) NOT NULL COMMENT '配置名称',
    `delivery_type` VARCHAR(50) NOT NULL COMMENT '推送类型: EMAIL/WEBHOOK/FEISHU/DINGTALK',
    `delivery_config` TEXT COMMENT 'JSON配置(收件人、Webhook URL等)',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `description` VARCHAR(500) COMMENT '描述',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` VARCHAR(100),
    `updated_by` VARCHAR(100),
    INDEX idx_tenant_id (`tenant_id`),
    INDEX idx_delivery_type (`delivery_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表推送渠道配置';

-- Report delivery record table (for tracking and idempotency)
CREATE TABLE IF NOT EXISTS s2_report_delivery_record (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `delivery_key` VARCHAR(200) NOT NULL COMMENT '幂等键: schedule_id + execution_time + channel_id',
    `schedule_id` BIGINT NOT NULL COMMENT '关联调度ID',
    `execution_id` BIGINT COMMENT '关联执行记录ID',
    `config_id` BIGINT NOT NULL COMMENT '关联推送配置ID',
    `delivery_type` VARCHAR(50) NOT NULL COMMENT '推送类型',
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENDING/SUCCESS/FAILED',
    `file_location` VARCHAR(500) COMMENT '推送文件路径',
    `error_message` TEXT COMMENT '错误信息',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `started_at` DATETIME COMMENT '开始时间',
    `completed_at` DATETIME COMMENT '完成时间',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_delivery_key (`delivery_key`),
    INDEX idx_schedule_id (`schedule_id`),
    INDEX idx_execution_id (`execution_id`),
    INDEX idx_config_id (`config_id`),
    INDEX idx_status (`status`),
    INDEX idx_tenant_id (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表推送记录';
