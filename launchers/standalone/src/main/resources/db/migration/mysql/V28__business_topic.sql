-- V28__business_topic.sql

CREATE TABLE IF NOT EXISTS s2_business_topic (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL,
    description TEXT,
    priority    INT           DEFAULT 0 COMMENT 'Lower value = higher priority in dashboard',
    owner_id    BIGINT,
    default_delivery_config_ids VARCHAR(500) COMMENT 'CSV of delivery config IDs',
    enabled     TINYINT(1)    DEFAULT 1,
    tenant_id   BIGINT        DEFAULT 1,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_priority (priority)
);

CREATE TABLE IF NOT EXISTS s2_business_topic_item (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_id  BIGINT      NOT NULL,
    item_type VARCHAR(50) NOT NULL COMMENT 'FIXED_REPORT, ALERT_RULE, SCHEDULE',
    item_id   BIGINT      NOT NULL COMMENT 'datasetId for FIXED_REPORT, ruleId for ALERT_RULE, scheduleId for SCHEDULE',
    tenant_id BIGINT      DEFAULT 1,
    UNIQUE KEY uq_topic_item (topic_id, item_type, item_id),
    INDEX idx_item (item_type, item_id)
);
