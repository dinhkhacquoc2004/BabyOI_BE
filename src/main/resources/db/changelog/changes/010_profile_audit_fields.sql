-- liquibase formatted sql

-- changeset codex:032
-- validCheckSum: ANY
ALTER TABLE "profile"
    ADD COLUMN IF NOT EXISTS "updated_by" VARCHAR(255);

-- changeset codex:033
-- validCheckSum: ANY
ALTER TABLE "profile"
    ADD COLUMN IF NOT EXISTS "updated_at" TIMESTAMP(6);

-- changeset codex:034 splitStatements:false
-- validCheckSum: ANY
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'profile'
          AND column_name = 'update_by'
    ) THEN
        UPDATE "profile"
        SET "updated_by" = "update_by"::VARCHAR(255)
        WHERE "updated_by" IS NULL
          AND "update_by" IS NOT NULL;
    END IF;
END $$;

-- changeset codex:035
-- validCheckSum: ANY
ALTER TABLE "profile"
    ALTER COLUMN "created_by" TYPE VARCHAR(255) USING "created_by"::VARCHAR(255);

-- changeset codex:036
-- validCheckSum: ANY
UPDATE "profile" p
SET "created_by" = COALESCE(u."user_name", u."email", p."created_by")
FROM "users" u
WHERE p."created_by" ~ '^[0-9]+$'
  AND u."id" = p."created_by"::BIGINT;

-- changeset codex:037
-- validCheckSum: ANY
UPDATE "profile" p
SET "updated_by" = COALESCE(u."user_name", u."email", p."updated_by")
FROM "users" u
WHERE p."updated_by" ~ '^[0-9]+$'
  AND u."id" = p."updated_by"::BIGINT;

-- changeset codex:038
-- validCheckSum: ANY
UPDATE "profile" p
SET "created_by" = COALESCE(u."user_name", u."email", 'SYSTEM')
FROM "users" u
WHERE (p."created_by" IS NULL OR BTRIM(p."created_by") = '')
  AND u."id" = p."user_id";

-- changeset codex:039
-- validCheckSum: ANY
UPDATE "profile"
SET "updated_by" = COALESCE(NULLIF(BTRIM("updated_by"), ''), "created_by", 'SYSTEM'),
    "updated_at" = COALESCE("updated_at", "created_at", NOW())
WHERE "updated_by" IS NULL
   OR BTRIM("updated_by") = ''
   OR "updated_at" IS NULL;

-- changeset codex:040
-- validCheckSum: ANY
ALTER TABLE "profile"
    DROP COLUMN IF EXISTS "update_by";
