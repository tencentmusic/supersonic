CREATE TABLE IF NOT EXISTS `s2_report_schedule_confirmation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `confirm_token` VARCHAR(64) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `chat_id` INT NOT NULL,
    `action_type` VARCHAR(50) NOT NULL,
    `source_query_id` BIGINT DEFAULT NULL,
    `source_parse_id` INT DEFAULT NULL,
    `source_data_set_id` BIGINT DEFAULT NULL,
    `payload_json` TEXT DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `expire_at` TIMESTAMP NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_schedule_confirm_token` (`confirm_token`),
    KEY `idx_report_schedule_confirm_user_chat_status` (`user_id`, `chat_id`, `status`),
    KEY `idx_report_schedule_confirm_expire_at` (`expire_at`)
);
