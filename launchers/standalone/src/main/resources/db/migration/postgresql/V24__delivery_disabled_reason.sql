-- Add disabled_reason column to s2_report_delivery_config
-- Records why a delivery config was automatically disabled after consecutive failures
ALTER TABLE s2_report_delivery_config
    ADD COLUMN IF NOT EXISTS disabled_reason VARCHAR(512) DEFAULT NULL;

COMMENT ON COLUMN s2_report_delivery_config.disabled_reason IS '自动禁用原因';
