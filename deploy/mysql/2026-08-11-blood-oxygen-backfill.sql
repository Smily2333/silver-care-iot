-- Backfill successfully parsed historical oxygen packets exactly once.
INSERT INTO health_records (
    device_id,
    oxygen_saturation,
    oxygen_status,
    invalid_reason,
    source_command,
    raw_packet_id,
    measured_at,
    created_at
)
SELECT
    d.id,
    CASE WHEN packet.oxygen_value BETWEEN 1 AND 100 THEN packet.oxygen_value ELSE NULL END,
    CASE
        WHEN packet.oxygen_value IS NULL OR packet.oxygen_value <= 0 THEN 'INVALID'
        WHEN packet.oxygen_value < 70 THEN 'TOO_LOW'
        WHEN packet.oxygen_value > 100 THEN 'TOO_HIGH'
        ELSE 'VALID'
    END,
    CASE WHEN packet.oxygen_value IS NULL OR packet.oxygen_value <= 0 THEN '血氧值缺失或为 0' ELSE NULL END,
    'oxygen',
    packet.id,
    packet.received_at,
    packet.received_at
FROM (
    SELECT
        id,
        device_no,
        received_at,
        CAST(NULLIF(TRIM(SUBSTRING_INDEX(content, ',', -1)), '') AS UNSIGNED) AS oxygen_value
    FROM raw_packet_logs
    WHERE command = 'oxygen' AND parse_status = 'SUCCESS'
) packet
JOIN devices d ON d.device_no = packet.device_no
WHERE NOT EXISTS (
    SELECT 1 FROM health_records h WHERE h.raw_packet_id = packet.id
);
