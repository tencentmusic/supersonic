-- Allow platform-scoped roles to be tenant-less.
-- Fresh schema already uses tenant_id NULL for PLATFORM roles; this aligns existing databases.

ALTER TABLE s2_role
    MODIFY COLUMN tenant_id bigint(20) DEFAULT NULL COMMENT '租户ID (NULL表示平台级角色)';

UPDATE s2_role
SET tenant_id = NULL
WHERE scope = 'PLATFORM';
