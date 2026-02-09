-- Add delivery timing and failure tracking columns for observability

-- Add delivery time tracking to records
ALTER TABLE s2_report_delivery_record
ADD COLUMN delivery_time_ms BIGINT;

COMMENT ON COLUMN s2_report_delivery_record.delivery_time_ms IS '推送耗时(毫秒)';

-- Add consecutive failure tracking to configs
ALTER TABLE s2_report_delivery_config
ADD COLUMN consecutive_failures INT DEFAULT 0,
ADD COLUMN max_consecutive_failures INT DEFAULT 5;

COMMENT ON COLUMN s2_report_delivery_config.consecutive_failures IS '连续失败次数';
COMMENT ON COLUMN s2_report_delivery_config.max_consecutive_failures IS '最大连续失败次数(超过后自动禁用)';

-- Create index for statistics queries
CREATE INDEX IF NOT EXISTS idx_delivery_record_created_at ON s2_report_delivery_record(created_at);
CREATE INDEX IF NOT EXISTS idx_delivery_record_status_type ON s2_report_delivery_record(status, delivery_type);
