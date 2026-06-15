-- liquibase formatted sql

-- changeset codex:108
ALTER TABLE "notification"
    ADD COLUMN IF NOT EXISTS "archived_at" TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS "reminder_key" VARCHAR(160);

-- changeset codex:109
CREATE UNIQUE INDEX IF NOT EXISTS "uk_notification_reminder_key"
    ON "notification" ("reminder_key")
    WHERE "reminder_key" IS NOT NULL;

-- changeset codex:110
CREATE INDEX IF NOT EXISTS "idx_notification_user_archive_created"
    ON "notification" ("user_id", "archived_at", "created_at" DESC);
