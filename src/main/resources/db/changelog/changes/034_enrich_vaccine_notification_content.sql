-- liquibase formatted sql

-- changeset codex:111
WITH notification_profile AS (
    SELECT
        n.id AS notification_id,
        COALESCE(
            vr.profile_id,
            CASE
                WHEN n.data_json IS NOT NULL AND n.data_json ~ '"profileId"[[:space:]]*:[[:space:]]*[0-9]+'
                THEN (regexp_match(n.data_json, '"profileId"[[:space:]]*:[[:space:]]*([0-9]+)'))[1]::BIGINT
            END
        ) AS profile_id,
        n.source_id,
        n.created_at
    FROM notification n
    LEFT JOIN vaccine_record vr ON vr.id = n.source_id
    WHERE n.type = 'VACCINE_REMINDER'
), resolved_record AS (
    SELECT
        np.notification_id,
        candidate.id AS record_id,
        candidate.profile_id,
        candidate.injection_date,
        candidate.dose_order,
        p.name AS profile_name,
        d.name AS disease_name,
        v.name AS vaccine_name
    FROM notification_profile np
    JOIN profile p ON p.id = np.profile_id
    LEFT JOIN LATERAL (
        SELECT vr.*
        FROM vaccine_record vr
        WHERE vr.profile_id = np.profile_id
          AND (np.source_id IS NULL OR vr.id = np.source_id)
        ORDER BY
            CASE WHEN vr.injection_date = np.created_at::DATE THEN 0 ELSE 1 END,
            ABS(vr.injection_date - np.created_at::DATE),
            vr.id
        LIMIT 1
    ) candidate ON TRUE
    LEFT JOIN child_vaccine_diseases d ON d.id = candidate.disease_id
    LEFT JOIN vaccines v ON v.id = candidate.vaccine_id
)
UPDATE notification n
SET
    source_id = COALESCE(n.source_id, rr.record_id),
    title = 'Lịch tiêm của bé ' || rr.profile_name,
    body = 'Bé ' || rr.profile_name || ' tiêm '
        || COALESCE(
            CASE
                WHEN rr.disease_name IS NOT NULL AND rr.vaccine_name IS NOT NULL AND lower(rr.disease_name) <> lower(rr.vaccine_name)
                THEN rr.disease_name || ' (' || rr.vaccine_name || ')'
            END,
            rr.disease_name,
            rr.vaccine_name,
            'vaccine theo lịch'
        )
        || ', ' || CASE WHEN rr.dose_order IS NULL THEN 'mũi theo lịch' ELSE 'mũi ' || rr.dose_order END
        || CASE
            WHEN rr.injection_date = CURRENT_DATE THEN ' hôm nay.'
            WHEN rr.injection_date IS NOT NULL THEN ' vào ngày ' || to_char(rr.injection_date, 'DD/MM/YYYY') || '.'
            ELSE '.'
        END,
    data_json = jsonb_set(
        jsonb_set(COALESCE(n.data_json, '{}')::jsonb, '{route}', '"/lichtiem/listlichtiem"'::jsonb, true),
        '{recordId}', to_jsonb(rr.record_id), true
    )::TEXT
FROM resolved_record rr
WHERE n.id = rr.notification_id
  AND rr.record_id IS NOT NULL;
