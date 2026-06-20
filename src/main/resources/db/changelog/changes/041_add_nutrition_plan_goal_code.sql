ALTER TABLE nutrition_plan
    ADD COLUMN IF NOT EXISTS goal_code VARCHAR(80);

CREATE INDEX IF NOT EXISTS idx_nutrition_plan_goal_code
    ON nutrition_plan(profile_id, goal_code);
