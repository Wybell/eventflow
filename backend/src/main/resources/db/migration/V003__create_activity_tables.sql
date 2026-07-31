CREATE TABLE ef_activity (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    organization_id BIGINT UNSIGNED NOT NULL COMMENT 'Owning organization',
    title VARCHAR(120) NOT NULL COMMENT 'Activity title',
    summary VARCHAR(500) NULL COMMENT 'Activity summary',
    cover_url VARCHAR(500) NULL COMMENT 'Cover image URL',
    venue_name VARCHAR(120) NULL COMMENT 'Venue name',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT PUBLISHED OFFLINE',
    registration_start_time DATETIME(3) NOT NULL COMMENT 'Registration opening time',
    registration_end_time DATETIME(3) NOT NULL COMMENT 'Registration closing time',
    create_user_id BIGINT UNSIGNED NOT NULL COMMENT 'Creator user identifier',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
    PRIMARY KEY (id),
    KEY idx_ef_activity_organization_status (organization_id, status, id),
    KEY idx_ef_activity_registration_window (status, registration_start_time, registration_end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Event activities';

CREATE TABLE ef_activity_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    activity_id BIGINT UNSIGNED NOT NULL COMMENT 'Activity identifier',
    title VARCHAR(120) NOT NULL COMMENT 'Session title',
    start_time DATETIME(3) NOT NULL COMMENT 'Session start time',
    end_time DATETIME(3) NOT NULL COMMENT 'Session end time',
    total_quota INT UNSIGNED NOT NULL COMMENT 'Total capacity',
    available_quota INT UNSIGNED NOT NULL COMMENT 'Available capacity',
    reserved_quota INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Temporary reserved capacity',
    confirmed_quota INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Confirmed capacity',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Session status',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
    PRIMARY KEY (id),
    KEY idx_ef_activity_session_activity_start (activity_id, start_time, id),
    CONSTRAINT chk_ef_activity_session_quota CHECK (total_quota = available_quota + reserved_quota + confirmed_quota),
    CONSTRAINT chk_ef_activity_session_time CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Activity sessions and quota facts';
