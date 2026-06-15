-- liquibase formatted sql

-- changeset codex:032-create-baby-routine-entry
CREATE TABLE IF NOT EXISTS baby_routine_entry (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    routine_date DATE NOT NULL,
    entry_type VARCHAR(30) NOT NULL,
    planned_time TIME NOT NULL,
    actual_time TIME,
    activity VARCHAR(255) NOT NULL,
    note TEXT,
    icon VARCHAR(40),
    color VARCHAR(20),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    source VARCHAR(30),
    status BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_baby_routine_entry_profile
        FOREIGN KEY (profile_id) REFERENCES profile (id)
);

CREATE INDEX IF NOT EXISTS idx_baby_routine_profile_date
    ON baby_routine_entry (profile_id, routine_date);
