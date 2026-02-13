-- MySQL does not support ALTER TABLE ... ADD COLUMN IF NOT EXISTS
-- Use a stored procedure to conditionally add columns
DROP PROCEDURE IF EXISTS add_column_if_not_exists;

DELIMITER //
CREATE PROCEDURE add_column_if_not_exists()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 's2_feishu_query_session' AND column_name = 'dataset_id') THEN
        ALTER TABLE s2_feishu_query_session ADD COLUMN dataset_id BIGINT DEFAULT NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 's2_feishu_query_session' AND column_name = 'agent_id') THEN
        ALTER TABLE s2_feishu_query_session ADD COLUMN agent_id INT DEFAULT NULL;
    END IF;
END //
DELIMITER ;

CALL add_column_if_not_exists();
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
