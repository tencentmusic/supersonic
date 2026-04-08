-- V26__report_subscription.sql
-- User subscription tracking for fixed reports (by dataset)

CREATE TABLE IF NOT EXISTS s2_report_subscription (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL COMMENT 'Subscriber user ID',
    dataset_id  BIGINT       NOT NULL COMMENT 'Subscribed dataset (= fixed report)',
    tenant_id   BIGINT       NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_dataset (user_id, dataset_id, tenant_id),
    KEY idx_dataset_id (dataset_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='User subscriptions to fixed reports';
