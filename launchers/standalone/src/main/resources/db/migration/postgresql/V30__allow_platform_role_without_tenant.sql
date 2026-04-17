-- Allow platform-scoped roles to be tenant-less.
-- Fresh schema already uses tenant_id NULL for PLATFORM roles; this aligns existing databases.

ALTER TABLE s2_role
    ALTER COLUMN tenant_id DROP NOT NULL;

ALTER TABLE s2_role
    ALTER COLUMN tenant_id DROP DEFAULT;

UPDATE s2_role
SET tenant_id = NULL
WHERE scope = 'PLATFORM';
