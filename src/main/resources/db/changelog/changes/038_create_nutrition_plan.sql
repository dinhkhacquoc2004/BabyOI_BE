CREATE TABLE IF NOT EXISTS nutrition_plan (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES profile(id),
    current_goal TEXT,
    future_goal TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    meals_per_day INTEGER NOT NULL,
    summary TEXT,
    warnings_json TEXT,
    status BIGINT,
    ai_model VARCHAR(120),
    ai_prompt_version VARCHAR(80),
    request_hash VARCHAR(128),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_nutrition_plan_profile_date
    ON nutrition_plan(profile_id, start_date, end_date);

CREATE INDEX IF NOT EXISTS idx_nutrition_plan_request_hash
    ON nutrition_plan(request_hash, status);

CREATE TABLE IF NOT EXISTS nutrition_plan_day (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES nutrition_plan(id) ON DELETE CASCADE,
    plan_date DATE NOT NULL,
    day_index INTEGER NOT NULL,
    note TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_nutrition_plan_day_plan
    ON nutrition_plan_day(plan_id, plan_date);

CREATE TABLE IF NOT EXISTS nutrition_plan_meal (
    id BIGSERIAL PRIMARY KEY,
    plan_day_id BIGINT NOT NULL REFERENCES nutrition_plan_day(id) ON DELETE CASCADE,
    meal_type VARCHAR(40) NOT NULL,
    food_library_id BIGINT NOT NULL REFERENCES food_library(id),
    food_name_snapshot VARCHAR(255) NOT NULL,
    portion VARCHAR(255),
    reason TEXT,
    warning TEXT,
    eaten_status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_nutrition_plan_meal_day
    ON nutrition_plan_meal(plan_day_id);
