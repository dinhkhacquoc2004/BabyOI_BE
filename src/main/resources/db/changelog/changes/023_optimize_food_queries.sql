-- liquibase formatted sql

-- changeset codex:073
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS "idx_food_library_status_function_advance_id"
    ON "food_library" ("status", "function_code", "advance_for", "id");

CREATE INDEX IF NOT EXISTS "idx_food_library_name_trgm"
    ON "food_library"
    USING gin (lower("name") gin_trgm_ops);

CREATE INDEX IF NOT EXISTS "idx_food_library_ingredients_food_lib_id"
    ON "food_library_ingredients" ("food_lib_id");

CREATE INDEX IF NOT EXISTS "idx_food_library_ingredients_food_ing_id"
    ON "food_library_ingredients" ("food_ing_id");

CREATE UNIQUE INDEX IF NOT EXISTS "uk_food_nutrition_summary_food"
    ON "food_nutrition_summary" ("food_id");

CREATE UNIQUE INDEX IF NOT EXISTS "uk_food_recommendation_food"
    ON "food_recommendation" ("food_id");

CREATE UNIQUE INDEX IF NOT EXISTS "uk_ingredient_nutrition_food_ingredient"
    ON "ingredient_nutrition" ("food_ingredient_id");
