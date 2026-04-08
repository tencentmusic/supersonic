-- V28__business_topic.sql

CREATE TABLE IF NOT EXISTS s2_business_topic (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL,
    description TEXT,
    priority    INT           DEFAULT 0,
    owner_id    BIGINT,
    default_delivery_config_ids VARCHAR(500),
    enabled     SMALLINT      DEFAULT 1,
    tenant_id   BIGINT        DEFAULT 1,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_business_topic_tenant ON s2_business_topic (tenant_id);
CREATE INDEX IF NOT EXISTS idx_business_topic_priority ON s2_business_topic (priority);

CREATE TABLE IF NOT EXISTS s2_business_topic_item (
    id        BIGSERIAL PRIMARY KEY,
    topic_id  BIGINT      NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    item_id   BIGINT      NOT NULL,
    tenant_id BIGINT      DEFAULT 1,
    UNIQUE (topic_id, item_type, item_id)
);

CREATE INDEX IF NOT EXISTS idx_business_topic_item_type ON s2_business_topic_item (item_type, item_id);
