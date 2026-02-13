-- Add pool_config column to s2_database table for connection pool isolation settings
-- This migration is idempotent - it checks if the column exists before adding

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 's2_database' AND column_name = 'pool_config'
    ) THEN
        ALTER TABLE s2_database ADD COLUMN pool_config TEXT;
        COMMENT ON COLUMN s2_database.pool_config IS 'JSON configuration for connection pool settings per pool type';
    END IF;
END $$;
