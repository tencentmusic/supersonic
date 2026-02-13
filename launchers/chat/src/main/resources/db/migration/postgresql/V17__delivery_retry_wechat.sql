-- Add retry scheduling fields and WeChatWork support

-- Add retry scheduling fields to delivery records
ALTER TABLE s2_report_delivery_record
ADD COLUMN max_retries INT DEFAULT 5,
ADD COLUMN next_retry_at TIMESTAMP;

COMMENT ON COLUMN s2_report_delivery_record.max_retries IS '最大重试次数';
COMMENT ON COLUMN s2_report_delivery_record.next_retry_at IS '下次重试时间';

-- Index for retry task queries
CREATE INDEX IF NOT EXISTS idx_delivery_record_retry ON s2_report_delivery_record(status, next_retry_at);
