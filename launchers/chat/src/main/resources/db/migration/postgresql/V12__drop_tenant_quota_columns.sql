-- Remove redundant quota columns from s2_tenant.
-- Subscription plan quotas are now managed entirely in s2_subscription_plan
-- and accessed via s2_tenant_subscription.

ALTER TABLE s2_tenant
    DROP COLUMN IF EXISTS plan_id,
    DROP COLUMN IF EXISTS max_users,
    DROP COLUMN IF EXISTS max_datasets,
    DROP COLUMN IF EXISTS max_models,
    DROP COLUMN IF EXISTS max_agents,
    DROP COLUMN IF EXISTS max_api_calls_per_day,
    DROP COLUMN IF EXISTS max_tokens_per_month;
