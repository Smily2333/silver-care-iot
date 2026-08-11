SELECT 'future_total', COUNT(*), MIN(located_at), MAX(located_at)
FROM location_records
WHERE located_at > DATE_ADD(UTC_TIMESTAMP(), INTERVAL 30 DAY);

SELECT 'year_2036', COUNT(*), MIN(located_at), MAX(located_at)
FROM location_records
WHERE located_at >= '2036-01-01' AND located_at < '2037-01-01';

SELECT id, device_id, source_command, located_at, created_at
FROM location_records
WHERE located_at >= '2036-01-01' AND located_at < '2037-01-01'
ORDER BY id
LIMIT 100;

SELECT 'linked_fall_alerts', COUNT(*)
FROM fall_alerts
WHERE location_record_id IN (
    SELECT id FROM location_records
    WHERE located_at >= '2036-01-01' AND located_at < '2037-01-01'
);

SELECT YEAR(located_at) AS located_year, COUNT(*)
FROM location_records
GROUP BY YEAR(located_at)
ORDER BY located_year;

SELECT id, device_id, source_command, located_at, created_at
FROM location_records
ORDER BY located_at DESC
LIMIT 30;

SELECT TABLE_NAME, COLUMN_NAME, CONSTRAINT_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_SCHEMA = DATABASE()
  AND REFERENCED_TABLE_NAME = 'location_records';
