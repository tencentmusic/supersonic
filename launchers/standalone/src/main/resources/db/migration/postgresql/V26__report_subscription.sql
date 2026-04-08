-- V26__report_subscription.sql
-- User subscription tracking for fixed reports (by dataset)

CREATE TABLE IF NOT EXISTS s2_report_subscription (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    dataset_id  BIGINT       NOT NULL,
    tenant_id   BIGINT       NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_dataset UNIQUE (user_id, dataset_id, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_report_sub_dataset ON s2_report_subscription (dataset_id);
CREATE INDEX IF NOT EXISTS idx_report_sub_tenant  ON s2_report_subscription (tenant_id);
