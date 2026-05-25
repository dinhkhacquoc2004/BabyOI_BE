-- liquibase formatted sql

-- changeset codex:042
-- validCheckSum: ANY
ALTER TABLE "favorite_food"
    ADD COLUMN IF NOT EXISTS "profile_id" BIGINT;

-- changeset codex:043
-- validCheckSum: ANY
ALTER TABLE "restricted_food"
    ADD COLUMN IF NOT EXISTS "profile_id" BIGINT;

-- changeset codex:044
-- validCheckSum: ANY
UPDATE "favorite_food" ff
SET "profile_id" = p."id"
FROM (
    SELECT DISTINCT ON ("user_id") "id", "user_id"
    FROM "profile"
    WHERE "status" = 2
    ORDER BY "user_id", "id"
) p
WHERE ff."profile_id" IS NULL
  AND ff."user_id" = p."user_id";

-- changeset codex:045
-- validCheckSum: ANY
UPDATE "restricted_food" rf
SET "profile_id" = p."id"
FROM (
    SELECT DISTINCT ON ("user_id") "id", "user_id"
    FROM "profile"
    WHERE "status" = 2
    ORDER BY "user_id", "id"
) p
WHERE rf."profile_id" IS NULL
  AND rf."user_id" = p."user_id";

-- changeset codex:046
-- validCheckSum: ANY
ALTER TABLE "favorite_food"
    DROP CONSTRAINT IF EXISTS "fk_favorite_food_profile";

-- changeset codex:047
-- validCheckSum: ANY
ALTER TABLE "favorite_food"
    ADD CONSTRAINT "fk_favorite_food_profile"
    FOREIGN KEY ("profile_id") REFERENCES "profile" ("id");

-- changeset codex:048
-- validCheckSum: ANY
ALTER TABLE "restricted_food"
    DROP CONSTRAINT IF EXISTS "fk_restricted_food_profile";

-- changeset codex:049
-- validCheckSum: ANY
ALTER TABLE "restricted_food"
    ADD CONSTRAINT "fk_restricted_food_profile"
    FOREIGN KEY ("profile_id") REFERENCES "profile" ("id");

-- changeset codex:050
-- validCheckSum: ANY
DROP INDEX IF EXISTS "uk_favorite_food_user_food";

-- changeset codex:051
-- validCheckSum: ANY
DROP INDEX IF EXISTS "uk_restricted_food_user_food";

-- changeset codex:052
-- validCheckSum: ANY
CREATE UNIQUE INDEX IF NOT EXISTS "uk_favorite_food_profile_food"
    ON "favorite_food" ("profile_id", "food_libarary_id")
    WHERE "profile_id" IS NOT NULL;

-- changeset codex:053
-- validCheckSum: ANY
CREATE UNIQUE INDEX IF NOT EXISTS "uk_restricted_food_profile_food"
    ON "restricted_food" ("profile_id", "food_libarary_id")
    WHERE "profile_id" IS NOT NULL;
