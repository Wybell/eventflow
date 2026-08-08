ALTER TABLE ef_user
    ADD COLUMN avatar_url VARCHAR(500) NULL COMMENT 'User avatar URL' AFTER display_name;
