SELECT 'database' AS item, DATABASE() AS value;

SELECT TABLE_NAME, TABLE_ROWS
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME;

SELECT 'devices' AS item, COUNT(*) AS value FROM devices
UNION ALL SELECT 'health_records', COUNT(*) FROM health_records
UNION ALL SELECT 'location_records', COUNT(*) FROM location_records
UNION ALL SELECT 'fall_alerts', COUNT(*) FROM fall_alerts
UNION ALL SELECT 'raw_packet_logs', COUNT(*) FROM raw_packet_logs
UNION ALL SELECT 'miniapp_users', COUNT(*) FROM miniapp_users
UNION ALL SELECT 'miniapp_sessions', COUNT(*) FROM miniapp_sessions
UNION ALL SELECT 'device_bindings', COUNT(*) FROM device_bindings;

SELECT TABLE_NAME, COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
    (TABLE_NAME = 'health_records' AND COLUMN_NAME IN (
      'heart_rate_status', 'blood_pressure_status', 'temperature_status', 'invalid_reason'
    ))
    OR (TABLE_NAME = 'location_records' AND COLUMN_NAME IN (
      'approximate_address', 'address_status', 'address_resolved_at'
    ))
  )
ORDER BY TABLE_NAME, COLUMN_NAME;

SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('device_actions', 'location_address_cache')
ORDER BY TABLE_NAME;

SELECT id, device_id, source_command, located_at, created_at
FROM location_records_backup_20260811_invalid
ORDER BY id;
