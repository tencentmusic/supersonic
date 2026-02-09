-- Add active_lock column for deployment concurrency control
-- Non-null (templateId_tenantId) when PENDING/RUNNING, NULL when SUCCESS/FAILED
-- Unique constraint ensures only one active deployment per template+tenant
ALTER TABLE s2_semantic_deployment
    ADD COLUMN active_lock VARCHAR(100) DEFAULT NULL;

COMMENT ON COLUMN s2_semantic_deployment.active_lock IS '部署并发锁: PENDING/RUNNING时为templateId_tenantId, 否则为NULL';

ALTER TABLE s2_semantic_deployment
    ADD CONSTRAINT uk_deployment_active_lock UNIQUE (active_lock);

-- Add current_step column for deployment progress tracking
ALTER TABLE s2_semantic_deployment
    ADD COLUMN current_step VARCHAR(50) DEFAULT NULL;

COMMENT ON COLUMN s2_semantic_deployment.current_step IS '当前执行步骤';
