-- V27__alert_event_resolution.sql
-- Add exception handling workflow columns to alert events

ALTER TABLE s2_alert_event
    ADD COLUMN IF NOT EXISTS resolution_status VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    ADD COLUMN IF NOT EXISTS acknowledged_by   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS acknowledged_at   TIMESTAMP,
    ADD COLUMN IF NOT EXISTS assignee_id       BIGINT,
    ADD COLUMN IF NOT EXISTS assigned_at       TIMESTAMP,
    ADD COLUMN IF NOT EXISTS resolved_by       VARCHAR(100),
    ADD COLUMN IF NOT EXISTS resolved_at       TIMESTAMP,
    ADD COLUMN IF NOT EXISTS closed_at         TIMESTAMP,
    ADD COLUMN IF NOT EXISTS notes             TEXT;

CREATE INDEX IF NOT EXISTS idx_alert_event_resolution ON s2_alert_event (resolution_status);
