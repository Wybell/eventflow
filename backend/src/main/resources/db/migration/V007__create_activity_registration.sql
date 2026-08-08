CREATE TABLE ef_registration (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    activity_id BIGINT UNSIGNED NOT NULL COMMENT 'Activity identifier',
    session_id BIGINT UNSIGNED NOT NULL COMMENT 'Activity session identifier',
    user_id BIGINT UNSIGNED NOT NULL COMMENT 'Participant user identifier',
    status VARCHAR(32) NOT NULL COMMENT 'CONFIRMED CANCELLED',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ef_registration_activity_user (activity_id, user_id),
    KEY idx_ef_registration_user_status (user_id, status, update_time, id),
    KEY idx_ef_registration_session_status (session_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User activity registrations';
