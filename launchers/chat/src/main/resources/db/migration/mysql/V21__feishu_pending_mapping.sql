-- Allow s2_user_id to be NULL for pending (unreviewed) mappings
ALTER TABLE s2_feishu_user_mapping MODIFY COLUMN s2_user_id BIGINT DEFAULT NULL;
