ALTER TABLE "vaccine_record"
    ADD COLUMN IF NOT EXISTS "source" BIGINT NOT NULL DEFAULT 1;

UPDATE "vaccine_record"
SET "source" = 1
WHERE "source" IS NULL;
