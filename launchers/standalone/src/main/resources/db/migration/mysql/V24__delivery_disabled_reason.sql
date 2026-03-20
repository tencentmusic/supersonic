-- Add disabled_reason column to s2_report_delivery_config
-- Records why a delivery config was automatically disabled after consecutive failures
ALTER TABLE s2_report_delivery_config
    ADD COLUMN `disabled_reason` VARCHAR(512) DEFAULT NULL COMMENT '自动禁用原因';
