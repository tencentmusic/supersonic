-- Allow s2_user_id to be NULL for pending (unreviewed) mappings
ALTER TABLE s2_feishu_user_mapping ALTER COLUMN s2_user_id DROP NOT NULL;
ALTER TABLE s2_feishu_user_mapping ALTER COLUMN s2_user_id SET DEFAULT NULL;
