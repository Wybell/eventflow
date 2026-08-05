ALTER TABLE ef_activity
    MODIFY COLUMN organization_id BIGINT UNSIGNED NULL COMMENT 'Legacy owning organization',
    ADD COLUMN organizer_name VARCHAR(120) NULL COMMENT 'Organizer name snapshot' AFTER organization_id,
    ADD COLUMN contact_name VARCHAR(64) NULL COMMENT 'Activity contact name' AFTER organizer_name,
    ADD COLUMN contact_mobile VARCHAR(20) NULL COMMENT 'Activity contact mobile' AFTER contact_name,
    ADD COLUMN contact_email VARCHAR(255) NULL COMMENT 'Activity contact email' AFTER contact_mobile,
    ADD COLUMN review_note VARCHAR(500) NULL COMMENT 'Latest review note' AFTER status,
    ADD COLUMN review_user_id BIGINT UNSIGNED NULL COMMENT 'Latest reviewer user identifier' AFTER review_note,
    ADD COLUMN review_time DATETIME(3) NULL COMMENT 'Latest review time' AFTER review_user_id,
    ADD COLUMN published_time DATETIME(3) NULL COMMENT 'Public publication time' AFTER review_time,
    ADD KEY idx_ef_activity_creator_status_id (create_user_id, status, id),
    ADD KEY idx_ef_activity_review_status_id (status, id);

CREATE TABLE ef_activity_review_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    activity_id BIGINT UNSIGNED NOT NULL COMMENT 'Activity identifier',
    reviewer_user_id BIGINT UNSIGNED NOT NULL COMMENT 'Reviewer user identifier',
    action VARCHAR(32) NOT NULL COMMENT 'APPROVED REJECTED OFFLINE',
    review_note VARCHAR(500) NULL COMMENT 'Review note',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    PRIMARY KEY (id),
    KEY idx_ef_activity_review_record_activity_id (activity_id, id),
    KEY idx_ef_activity_review_record_reviewer_id (reviewer_user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Activity review history';
