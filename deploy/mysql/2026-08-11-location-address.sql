-- Pass the target database explicitly with mysql --database=DATABASE_NAME.
-- No USE statement is allowed here, so a development migration cannot silently target production.

-- 执行前请备份数据库。此脚本只执行一次；JPA ddl-auto=update 也会创建相同结构。
ALTER TABLE location_records
    ADD COLUMN approximate_address VARCHAR(255) NULL,
    ADD COLUMN address_status VARCHAR(16) NULL,
    ADD COLUMN address_resolved_at DATETIME(6) NULL;

CREATE TABLE location_address_cache (
    grid_key VARCHAR(64) NOT NULL,
    latitude DECIMAL(11, 7) NOT NULL,
    longitude DECIMAL(12, 7) NOT NULL,
    approximate_address VARCHAR(255) NOT NULL,
    road VARCHAR(128) NULL,
    neighbourhood VARCHAR(128) NULL,
    district VARCHAR(128) NULL,
    city VARCHAR(128) NULL,
    display_name VARCHAR(512) NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (grid_key)
);
