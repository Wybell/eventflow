CREATE TABLE ef_outbox_event (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    event_id VARCHAR(36) NOT NULL COMMENT 'Globally unique event identifier',
    event_type VARCHAR(100) NOT NULL COMMENT 'Event type',
    aggregate_type VARCHAR(64) NOT NULL COMMENT 'Aggregate type',
    aggregate_id BIGINT UNSIGNED NOT NULL COMMENT 'Aggregate identifier',
    payload JSON NOT NULL COMMENT 'Event payload',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'Delivery status',
    retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Delivery retry count',
    next_attempt_time DATETIME(3) NULL COMMENT 'Next delivery attempt time',
    published_time DATETIME(3) NULL COMMENT 'Successful publication time',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ef_outbox_event_event_id (event_id),
    KEY idx_ef_outbox_event_delivery (status, next_attempt_time, id),
    KEY idx_ef_outbox_event_aggregate (aggregate_type, aggregate_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Reliable event outbox';

CREATE TABLE ef_processed_event (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    consumer_name VARCHAR(100) NOT NULL COMMENT 'Consumer identity',
    event_id VARCHAR(36) NOT NULL COMMENT 'Consumed event identifier',
    processed_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Processing completion time',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ef_processed_event_consumer_event (consumer_name, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Idempotent message consumption records';

CREATE TABLE ef_operation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    request_id VARCHAR(64) NOT NULL COMMENT 'Request trace identifier',
    actor_id BIGINT UNSIGNED NULL COMMENT 'Operator user identifier',
    organization_id BIGINT UNSIGNED NULL COMMENT 'Organization identifier',
    action VARCHAR(100) NOT NULL COMMENT 'Operation action',
    target_type VARCHAR(64) NOT NULL COMMENT 'Target aggregate type',
    target_id BIGINT UNSIGNED NULL COMMENT 'Target aggregate identifier',
    detail JSON NULL COMMENT 'Desensitized operation detail',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
    PRIMARY KEY (id),
    KEY idx_ef_operation_log_request (request_id),
    KEY idx_ef_operation_log_actor_time (actor_id, create_time),
    KEY idx_ef_operation_log_target (target_type, target_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Auditable platform operations';
