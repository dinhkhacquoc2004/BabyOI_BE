-- liquibase formatted sql

-- changeset codex:030
ALTER TABLE "users"
    ADD COLUMN IF NOT EXISTS "social_provider" VARCHAR(50),
    ADD COLUMN IF NOT EXISTS "social_provider_id" VARCHAR(255);

-- changeset codex:031
CREATE INDEX IF NOT EXISTS "idx_users_social_provider_id"
    ON "users" ("social_provider", "social_provider_id");
