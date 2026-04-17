-- Allow platform-scoped roles to be tenant-less.
-- Fresh schema already uses tenant_id NULL for PLATFORM roles; this aligns existing databases.

ALTER TABLE s2_role
    MODIFY COLUMN tenant_id bigint(20) DEFAULT NULL COMMENT '租户ID (NULL表示平台级角色)';

UPDATE s2_role
SET tenant_id = NULL
WHERE scope = 'PLATFORM';

-- MySQL does not support partial (filtered) unique indexes. Use a generated column
-- that holds the code only for PLATFORM roles and NULL otherwise, then unique-index it.
-- Multiple NULLs are allowed in MySQL unique indexes, so TENANT rows (NULL sentinel)
-- don't conflict; only two PLATFORM rows with the same code are blocked.
ALTER TABLE s2_role
    ADD COLUMN platform_code_sentinel VARCHAR(50)
        AS (IF(scope = 'PLATFORM', code, NULL)) STORED COMMENT '平台级角色唯一性哨兵列';
ALTER TABLE s2_role
    ADD UNIQUE INDEX uk_platform_role_code (platform_code_sentinel);
