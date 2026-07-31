CREATE TABLE ef_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    username VARCHAR(64) NOT NULL COMMENT 'Unique account name',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt password hash',
    display_name VARCHAR(64) NOT NULL COMMENT 'Display name',
    mobile VARCHAR(20) NULL COMMENT 'Mobile number',
    email VARCHAR(255) NULL COMMENT 'Email address',
    organization_id BIGINT UNSIGNED NULL COMMENT 'Organization identifier',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Account status',
    last_login_time DATETIME(3) NULL COMMENT 'Last successful login time',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ef_user_username (username),
    UNIQUE KEY uk_ef_user_mobile (mobile),
    UNIQUE KEY uk_ef_user_email (email),
    KEY idx_ef_user_organization_status (organization_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='EventFlow user accounts';

CREATE TABLE ef_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    role_code VARCHAR(32) NOT NULL COMMENT 'Stable role code',
    role_name VARCHAR(64) NOT NULL COMMENT 'Role display name',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Role status',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ef_role_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='EventFlow authorization roles';

CREATE TABLE ef_user_role_rel (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    user_id BIGINT UNSIGNED NOT NULL COMMENT 'User identifier',
    role_id BIGINT UNSIGNED NOT NULL COMMENT 'Role identifier',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ef_user_role_rel_user_role (user_id, role_id),
    KEY idx_ef_user_role_rel_role_user (role_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User role associations';

CREATE TABLE ef_refresh_token (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    user_id BIGINT UNSIGNED NOT NULL COMMENT 'User identifier',
    token_hash CHAR(64) NOT NULL COMMENT 'SHA-256 hash of opaque refresh token',
    expires_time DATETIME(3) NOT NULL COMMENT 'Expiration time',
    revoked_time DATETIME(3) NULL COMMENT 'Revocation time',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ef_refresh_token_hash (token_hash),
    KEY idx_ef_refresh_token_user_expiry (user_id, expires_time),
    KEY idx_ef_refresh_token_expiry (expires_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Rotating opaque refresh tokens';

INSERT INTO ef_role (role_code, role_name, status)
VALUES ('USER', '普通用户', 'ACTIVE'), ('ORGANIZER', '组织者', 'ACTIVE'), ('ADMIN', '管理员', 'ACTIVE')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = VALUES(status);
