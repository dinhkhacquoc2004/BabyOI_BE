-- liquibase formatted sql

-- changeset codex:046-remove-routine-outside-profile-lifetime
WITH invalid_entries AS (
    SELECT bre.id
    FROM baby_routine_entry bre
    JOIN profile p ON p.id = bre.profile_id
    WHERE COALESCE(bre.source, '') <> 'CUSTOM'
      AND (
          bre.routine_date < GREATEST(
              p.date_of_birth,
              COALESCE(p.created_at::DATE, p.date_of_birth)
          )
          OR bre.routine_date > CURRENT_DATE + 1
      )
)
DELETE FROM notification n
USING invalid_entries invalid
WHERE n.source_type = 'BABY_ROUTINE_ENTRY'
  AND n.source_id = invalid.id;

-- changeset codex:046-remove-generated-routine-entries-outside-profile-lifetime
DELETE FROM baby_routine_entry bre
USING profile p
WHERE p.id = bre.profile_id
  AND COALESCE(bre.source, '') <> 'CUSTOM'
  AND (
      bre.routine_date < GREATEST(
          p.date_of_birth,
          COALESCE(p.created_at::DATE, p.date_of_birth)
      )
      OR bre.routine_date > CURRENT_DATE + 1
  );
