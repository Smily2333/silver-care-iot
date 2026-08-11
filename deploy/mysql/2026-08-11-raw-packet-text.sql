-- Pass the target database explicitly with mysql --database=DATABASE_NAME.
-- Device location frames may contain base-station and Wi-Fi details and exceed 255 characters.

ALTER TABLE raw_packet_logs
    MODIFY COLUMN content LONGTEXT NULL,
    MODIFY COLUMN raw_packet LONGTEXT NOT NULL,
    MODIFY COLUMN error_message VARCHAR(1024) NULL;
