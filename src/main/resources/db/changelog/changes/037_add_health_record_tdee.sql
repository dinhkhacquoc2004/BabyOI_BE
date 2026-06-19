-- liquibase formatted sql

-- changeset codex:080
ALTER TABLE IF EXISTS "health_record"
    ADD COLUMN IF NOT EXISTS "activity_level" VARCHAR(32),
    ADD COLUMN IF NOT EXISTS "bmr" DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS "tdee" DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS "tdee_formula" VARCHAR(64);
