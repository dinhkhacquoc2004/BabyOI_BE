-- liquibase formatted sql

-- changeset codex:020
ALTER TABLE "food_library"
    ADD COLUMN IF NOT EXISTS "image_url" TEXT;

ALTER TABLE "food_ingredients"
    ADD COLUMN IF NOT EXISTS "image_url" TEXT;

-- changeset codex:021
UPDATE "food_library"
SET "image_url" = '/Food/Food_' || "id" || '.jpg'
WHERE "image_url" IS NULL OR "image_url" = '';

UPDATE "food_ingredients"
SET "image_url" = CASE
    WHEN "id" = 11 THEN '/Ingredient/ingredient_11.png'
    WHEN "id" = 45 THEN NULL
    ELSE '/Ingredient/Ingredient_' || "id" || '.jpg'
END
WHERE "id" BETWEEN 1 AND 123;
