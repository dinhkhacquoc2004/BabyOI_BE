-- liquibase formatted sql

-- changeset codex:089
-- validCheckSum: ANY
INSERT INTO "profile" (
    "id",
    "user_id",
    "name",
    "date_of_birth",
    "sex",
    "profile_type",
    "created_at",
    "created_by",
    "status",
    "profile_code"
)
SELECT
    seed."id",
    2,
    seed."name",
    seed."date_of_birth"::DATE,
    seed."sex",
    seed."profile_type",
    CURRENT_TIMESTAMP,
    'SYSTEM',
    2,
    seed."profile_code"
FROM (
    VALUES
        (1::BIGINT, 'B? Th? Nh?t', '2025-06-09', 'FEMALE', 'CHILD', 'PRF001'),
        (2::BIGINT, 'B? Th? Hai', '2026-02-09', 'FEMALE', 'CHILD', 'PRF002')
) AS seed (
    "id",
    "name",
    "date_of_birth",
    "sex",
    "profile_type",
    "profile_code"
)
WHERE EXISTS (
    SELECT 1 FROM "users" WHERE "id" = 2
)
ON CONFLICT ("id") DO UPDATE SET
    "user_id" = EXCLUDED."user_id",
    "name" = EXCLUDED."name",
    "date_of_birth" = EXCLUDED."date_of_birth",
    "sex" = EXCLUDED."sex",
    "profile_type" = EXCLUDED."profile_type",
    "status" = EXCLUDED."status",
    "profile_code" = EXCLUDED."profile_code",
    "updated_at" = CURRENT_TIMESTAMP,
    "updated_by" = 'SYSTEM';

-- changeset codex:090
-- validCheckSum: ANY
SELECT setval(
    pg_get_serial_sequence('"profile"', 'id'),
    COALESCE((SELECT MAX("id") FROM "profile"), 1),
    true
);
