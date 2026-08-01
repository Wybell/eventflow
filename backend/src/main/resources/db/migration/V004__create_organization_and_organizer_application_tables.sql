CREATE TABLE ef_organization (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    name VARCHAR(120) NOT NULL COMMENT 'Organization name',
    description VARCHAR(500) NULL COMMENT 'Organization description',
    contact_name VARCHAR(64) NOT NULL COMMENT 'Primary contact name',
    contact_mobile VARCHAR(20) NULL COMMENT 'Primary contact mobile',
    contact_email VARCHAR(255) NULL COMMENT 'Primary contact email',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Organization status',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ef_organization_name (name),
    KEY idx_ef_organization_status_id (status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Event organizer organizations';

CREATE TABLE ef_organizer_application (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    user_id BIGINT UNSIGNED NOT NULL COMMENT 'Applicant user identifier',
    organization_name VARCHAR(120) NOT NULL COMMENT 'Requested organization name',
    organization_description VARCHAR(500) NULL COMMENT 'Requested organization description',
    contact_name VARCHAR(64) NOT NULL COMMENT 'Primary contact name',
    contact_mobile VARCHAR(20) NULL COMMENT 'Primary contact mobile',
    contact_email VARCHAR(255) NULL COMMENT 'Primary contact email',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING APPROVED REJECTED',
    review_note VARCHAR(500) NULL COMMENT 'Administrator review note',
    review_user_id BIGINT UNSIGNED NULL COMMENT 'Administrator user identifier',
    review_time DATETIME(3) NULL COMMENT 'Review time',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
    PRIMARY KEY (id),
    KEY idx_ef_organizer_application_user_id (user_id, id),
    KEY idx_ef_organizer_application_status_id (status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Organizer role applications';
