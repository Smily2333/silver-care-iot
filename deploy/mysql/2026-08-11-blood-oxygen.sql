-- Pass the target database explicitly with mysql --database=DATABASE_NAME.
ALTER TABLE health_records
    ADD COLUMN oxygen_saturation INT NULL,
    ADD COLUMN oxygen_status VARCHAR(16) NULL;
