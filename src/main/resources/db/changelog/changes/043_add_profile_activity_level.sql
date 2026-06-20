-- liquibase formatted sql

-- changeset codex:043-profile-activity-level
ALTER TABLE "profile"
    ADD COLUMN IF NOT EXISTS "activity_level" VARCHAR(32);

UPDATE "profile"
SET "activity_level" = 'LIGHT'
WHERE UPPER(COALESCE("profile_type", '')) = 'MOTHER'
  AND ("activity_level" IS NULL OR BTRIM("activity_level") = '');

-- changeset codex:043-activity-level-type-code
INSERT INTO "type_code" ("code", "name", "description", "status", "created_by")
VALUES ('ACTIVITY_LEVEL', 'Activity level', 'Activity levels used to estimate TDEE for mother profiles.', 2, 'SYSTEM')
ON CONFLICT ("code") DO UPDATE SET
    "name" = EXCLUDED."name",
    "description" = EXCLUDED."description",
    "status" = EXCLUDED."status",
    "updated_at" = CURRENT_TIMESTAMP,
    "updated_by" = 'SYSTEM';

-- changeset codex:043-activity-level-values
INSERT INTO "type_value" (
    "type_code_id",
    "value_code",
    "value_name",
    "value_text",
    "value_number",
    "description",
    "sort_order",
    "status",
    "created_by"
)
SELECT
    tc."id",
    seed."value_code",
    seed."value_name",
    seed."value_code",
    seed."factor",
    seed."description",
    seed."sort_order",
    2,
    'SYSTEM'
FROM (
    VALUES
        ('SEDENTARY', 'It van dong', 1.2, 'Little or no exercise.', 10),
        ('LIGHT', 'Van dong nhe', 1.375, 'Light exercise 1-3 days per week.', 20),
        ('MODERATE', 'Van dong vua', 1.55, 'Moderate exercise 3-5 days per week.', 30),
        ('ACTIVE', 'Van dong nhieu', 1.725, 'Hard exercise 6-7 days per week.', 40),
        ('VERY_ACTIVE', 'Van dong rat nhieu', 1.9, 'Very hard exercise or physical work.', 50)
) AS seed ("value_code", "value_name", "factor", "description", "sort_order")
JOIN "type_code" tc ON tc."code" = 'ACTIVITY_LEVEL'
ON CONFLICT ("type_code_id", "value_code") DO UPDATE SET
    "value_name" = EXCLUDED."value_name",
    "value_text" = EXCLUDED."value_text",
    "value_number" = EXCLUDED."value_number",
    "description" = EXCLUDED."description",
    "sort_order" = EXCLUDED."sort_order",
    "status" = EXCLUDED."status",
    "updated_at" = CURRENT_TIMESTAMP,
    "updated_by" = 'SYSTEM';
