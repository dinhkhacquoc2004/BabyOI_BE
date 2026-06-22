WITH ranked_active_plans AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY profile_id
               ORDER BY created_at DESC NULLS LAST, id DESC
           ) AS row_number
    FROM nutrition_plan
    WHERE status = 2
)
UPDATE nutrition_plan
SET status = 3,
    updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    SELECT id
    FROM ranked_active_plans
    WHERE row_number > 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_nutrition_plan_one_active_per_profile
    ON nutrition_plan(profile_id)
    WHERE status = 2;
