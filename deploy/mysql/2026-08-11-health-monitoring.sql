-- Pass the target database explicitly with mysql --database=DATABASE_NAME.
-- No USE statement is allowed here, so a development migration cannot silently target production.

-- 执行前备份数据库。本阶段只增加已确认不依赖 terminalStatus 位定义的结构。
ALTER TABLE health_records
    ADD COLUMN heart_rate_status VARCHAR(16) NULL,
    ADD COLUMN blood_pressure_status VARCHAR(16) NULL,
    ADD COLUMN temperature_status VARCHAR(16) NULL,
    ADD COLUMN invalid_reason VARCHAR(255) NULL;

CREATE TABLE device_actions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_id BIGINT NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    command_name VARCHAR(32) NOT NULL,
    command_content VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_by VARCHAR(64) NULL,
    requested_at DATETIME(6) NOT NULL,
    sent_at DATETIME(6) NULL,
    acknowledged_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    deadline_at DATETIME(6) NULL,
    result_record_type VARCHAR(32) NULL,
    result_record_id BIGINT NULL,
    failure_reason VARCHAR(512) NULL,
    ack_missing BIT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_device_action_device_time (device_id, requested_at),
    INDEX idx_device_action_deadline (status, deadline_at)
);
