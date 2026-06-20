-- liquibase formatted sql

-- changeset codex:039-remove-mother-routine-notifications
UPDATE baby_routine_entry bre
SET
    status = -4,
    updated_at = CURRENT_TIMESTAMP
FROM profile p
WHERE bre.profile_id = p.id
  AND UPPER(COALESCE(p.profile_type, '')) <> 'CHILD'
  AND bre.status <> -4;

-- changeset codex:039-archive-mother-routine-notifications
-- validCheckSum: 9:b644be4d15c0c0e8f27504cf02d5ef87
WITH routine_notification_profile AS (
    SELECT
        n.id AS notification_id,
        COALESCE(
            bre.profile_id,
            NULLIF(substring(n.data_json FROM '"profileId"[[:space:]]*:[[:space:]]*([0-9]+)'), '')::BIGINT
        ) AS profile_id
    FROM notification n
    LEFT JOIN baby_routine_entry bre
        ON n.source_type = 'BABY_ROUTINE_ENTRY'
       AND n.source_id = bre.id
    WHERE n.type = 'ROUTINE_REMINDER'
)
UPDATE notification n
SET
    status = -4,
    read_at = COALESCE(n.read_at, CURRENT_TIMESTAMP),
    archived_at = COALESCE(n.archived_at, CURRENT_TIMESTAMP)
FROM routine_notification_profile rnp
JOIN profile p ON p.id = rnp.profile_id
WHERE n.id = rnp.notification_id
  AND UPPER(COALESCE(p.profile_type, '')) <> 'CHILD'
  AND n.status <> -4;
