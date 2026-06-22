-- liquibase formatted sql

-- changeset codex:047-split-notification-channel-settings
ALTER TABLE "notification_setting"
    ADD COLUMN IF NOT EXISTS "vaccine_in_app_enabled" BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS "vaccine_push_enabled" BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS "appointment_in_app_enabled" BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS "appointment_push_enabled" BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS "chat_in_app_enabled" BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS "chat_push_enabled" BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS "promotion_in_app_enabled" BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS "promotion_push_enabled" BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS "system_in_app_enabled" BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS "system_push_enabled" BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE "notification_setting"
SET
    "vaccine_in_app_enabled" = "vaccine_enabled",
    "vaccine_push_enabled" = "vaccine_enabled",
    "appointment_in_app_enabled" = "appointment_enabled",
    "appointment_push_enabled" = "appointment_enabled",
    "chat_in_app_enabled" = "chat_enabled",
    "chat_push_enabled" = "chat_enabled",
    "promotion_in_app_enabled" = "promotion_enabled",
    "promotion_push_enabled" = "promotion_enabled",
    "system_in_app_enabled" = "system_enabled",
    "system_push_enabled" = "system_enabled";
