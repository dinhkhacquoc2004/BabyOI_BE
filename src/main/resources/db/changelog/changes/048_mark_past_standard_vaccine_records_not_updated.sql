-- liquibase formatted sql

-- changeset codex:048-mark-past-standard-vaccine-records-not-updated
UPDATE "vaccine_record"
SET "status" = 0,
    "updated_at" = CURRENT_DATE
WHERE "source" = 1
  AND "status" = -1
  AND "vaccine_id" IS NULL
  AND "actual_injection_date" IS NULL
  AND "injection_date" < CURRENT_DATE;
