--liquibase formatted sql

--changeset codex:046-create-profile-vaccine-disease-status
CREATE TABLE IF NOT EXISTS profile_vaccine_disease_status (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    disease_id BIGINT NOT NULL,
    status BIGINT NOT NULL DEFAULT 2,
    stopped_at TIMESTAMP,
    stopped_dose_order INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_profile_vaccine_disease_status_profile FOREIGN KEY (profile_id) REFERENCES profile(id),
    CONSTRAINT fk_profile_vaccine_disease_status_disease FOREIGN KEY (disease_id) REFERENCES child_vaccine_diseases(id),
    CONSTRAINT uk_profile_vaccine_disease_status UNIQUE (profile_id, disease_id)
);

CREATE INDEX IF NOT EXISTS idx_profile_vaccine_disease_status_profile_id ON profile_vaccine_disease_status(profile_id);
CREATE INDEX IF NOT EXISTS idx_profile_vaccine_disease_status_disease_id ON profile_vaccine_disease_status(disease_id);
CREATE INDEX IF NOT EXISTS idx_profile_vaccine_disease_status_status ON profile_vaccine_disease_status(status);
