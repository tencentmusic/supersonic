-- Allow platform-scoped roles to be tenant-less.
-- Fresh schema already uses tenant_id NULL for PLATFORM roles; this aligns existing databases.

ALTER TABLE s2_role
    ALTER COLUMN tenant_id DROP NOT NULL;

ALTER TABLE s2_role
    ALTER COLUMN tenant_id DROP DEFAULT;

UPDATE s2_role
SET tenant_id = NULL
WHERE scope = 'PLATFORM';

-- PostgreSQL supports partial unique indexes natively.
-- Enforce one platform role per code where tenant_id IS NULL.
CREATE UNIQUE INDEX uk_platform_role_code
    ON s2_role (code)
    WHERE tenant_id IS NULL;
