-- liquibase formatted sql

-- changeset codex:037-backfill-standard-child-vaccine-records
INSERT INTO "vaccine_record" (
    "profile_id",
    "disease_id",
    "dose_order",
    "source",
    "vaccine_id",
    "injection_date",
    "actual_injection_date",
    "price",
    "note",
    "created_at",
    "created_by",
    "status"
)
SELECT
    p."id",
    s."disease_id",
    s."dose_order",
    1,
    NULL,
    (p."date_of_birth" + make_interval(months => s."recommended_age_months"))::DATE,
    NULL,
    NULL,
    s."note",
    CURRENT_DATE,
    p."user_id",
    -1
FROM "profile" p
JOIN "child_disease_dose_schedules" s
    ON s."status" = 2
   AND s."recommended_age_months" BETWEEN 0 AND 24
WHERE p."status" = 2
  AND UPPER(COALESCE(p."profile_type", '')) = 'CHILD'
  AND p."date_of_birth" IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM "vaccine_record" vr
      WHERE vr."profile_id" = p."id"
        AND vr."source" = 1
        AND vr."disease_id" = s."disease_id"
        AND vr."dose_order" = s."dose_order"
  );

SELECT setval(pg_get_serial_sequence('"vaccine_record"', 'id'), COALESCE((SELECT MAX("id") FROM "vaccine_record"), 1), true);
