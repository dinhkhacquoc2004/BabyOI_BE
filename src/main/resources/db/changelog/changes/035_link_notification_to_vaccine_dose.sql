-- liquibase formatted sql

-- changeset codex:112
UPDATE notification n
SET data_json = jsonb_set(
    jsonb_set(
        jsonb_set(COALESCE(n.data_json, '{}')::jsonb, '{route}', '"/lichtiem/chitietbenh"'::jsonb, true),
        '{recordId}', to_jsonb(vr.id), true
    ),
    '{diseaseId}', to_jsonb(vr.disease_id), true
)::TEXT
FROM vaccine_record vr
WHERE n.type = 'VACCINE_REMINDER'
  AND n.source_id = vr.id
  AND vr.disease_id IS NOT NULL;
