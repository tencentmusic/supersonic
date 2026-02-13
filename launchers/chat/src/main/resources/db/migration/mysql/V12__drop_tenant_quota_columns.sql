-- Remove redundant quota columns from s2_tenant.
-- Subscription plan quotas are now managed entirely in s2_subscription_plan
-- and accessed via s2_tenant_subscription.

-- 使用存储过程安全删除列和索引（兼容 MySQL 5.7+）
DROP PROCEDURE IF EXISTS drop_tenant_quota_columns;

DELIMITER //
CREATE PROCEDURE drop_tenant_quota_columns()
BEGIN
    -- Drop index idx_tenant_plan if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 's2_tenant'
          AND index_name = 'idx_tenant_plan'
    ) THEN
        ALTER TABLE `s2_tenant` DROP INDEX `idx_tenant_plan`;
    END IF;

    -- Drop columns if they exist
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 's2_tenant'
          AND column_name = 'plan_id'
    ) THEN
        ALTER TABLE `s2_tenant`
            DROP COLUMN `plan_id`,
            DROP COLUMN `max_users`,
            DROP COLUMN `max_datasets`,
            DROP COLUMN `max_models`,
            DROP COLUMN `max_agents`,
            DROP COLUMN `max_api_calls_per_day`,
            DROP COLUMN `max_tokens_per_month`;
    END IF;
END //
DELIMITER ;

CALL drop_tenant_quota_columns();
DROP PROCEDURE IF EXISTS drop_tenant_quota_columns;
