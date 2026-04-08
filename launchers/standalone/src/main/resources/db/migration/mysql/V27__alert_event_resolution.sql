-- V27__alert_event_resolution.sql
-- Add exception handling workflow columns to alert events

ALTER TABLE s2_alert_event
    ADD COLUMN resolution_status VARCHAR(20)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CONFIRMED/ASSIGNED/RESOLVED/CLOSED',
    ADD COLUMN acknowledged_by   VARCHAR(100) NULL     COMMENT 'User who confirmed the event',
    ADD COLUMN acknowledged_at   DATETIME     NULL     COMMENT 'When the event was confirmed',
    ADD COLUMN assignee_id       BIGINT       NULL     COMMENT 'User ID of assigned handler',
    ADD COLUMN assigned_at       DATETIME     NULL     COMMENT 'When the event was assigned',
    ADD COLUMN resolved_by       VARCHAR(100) NULL     COMMENT 'User who resolved the event',
    ADD COLUMN resolved_at       DATETIME     NULL     COMMENT 'When the event was resolved',
    ADD COLUMN closed_at         DATETIME     NULL     COMMENT 'When the event was closed',
    ADD COLUMN notes             TEXT         NULL     COMMENT 'Resolution notes / timeline entries';

ALTER TABLE s2_alert_event
    ADD INDEX idx_alert_event_resolution (resolution_status);
