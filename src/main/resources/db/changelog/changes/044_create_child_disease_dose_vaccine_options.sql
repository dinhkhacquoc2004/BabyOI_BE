-- liquibase formatted sql

-- changeset codex:044-create-child-disease-dose-vaccine-options
CREATE TABLE IF NOT EXISTS child_disease_dose_vaccine_options (
    id BIGSERIAL PRIMARY KEY,
    disease_id BIGINT NOT NULL,
    dose_schedule_id BIGINT,
    dose_order INT NOT NULL,
    vaccine_id BIGINT NOT NULL,
    is_preferred BOOLEAN DEFAULT FALSE,
    display_order INT,
    note TEXT,
    status BIGINT DEFAULT 2,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_child_disease_dose_option_disease FOREIGN KEY (disease_id) REFERENCES child_vaccine_diseases(id),
    CONSTRAINT fk_child_disease_dose_option_schedule FOREIGN KEY (dose_schedule_id) REFERENCES child_disease_dose_schedules(id),
    CONSTRAINT fk_child_disease_dose_option_vaccine FOREIGN KEY (vaccine_id) REFERENCES vaccines(id),
    CONSTRAINT uk_child_disease_dose_vaccine_option UNIQUE (disease_id, dose_order, vaccine_id)
);

CREATE INDEX IF NOT EXISTS idx_child_disease_dose_option_disease_id
    ON child_disease_dose_vaccine_options(disease_id);

CREATE INDEX IF NOT EXISTS idx_child_disease_dose_option_vaccine_id
    ON child_disease_dose_vaccine_options(vaccine_id);

-- Add a common 5-in-1 product so DTP, polio and Hib schedules can be configured separately from hepatitis B.
INSERT INTO vaccines (id, name, manufacturer, origin, description, status, created_at)
VALUES
    (30, 'Pentaxim', 'Sanofi', 'Pháp', 'Vắc-xin 5 trong 1 phòng bạch hầu, ho gà, uốn ván, bại liệt và Hib.', 2, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    manufacturer = EXCLUDED.manufacturer,
    origin = EXCLUDED.origin,
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO vaccine_disease_coverage (vaccine_id, disease_id, product_family_code, interchange_rule, status, created_at)
SELECT 30, d.id, 'PENTAVALENT_DTP_IPV_HIB', 1, 2, CURRENT_TIMESTAMP
FROM child_vaccine_diseases d
WHERE d.code IN ('DIPHTHERIA', 'PERTUSSIS', 'TETANUS', 'POLIO', 'HIB')
ON CONFLICT (vaccine_id, disease_id) DO UPDATE SET
    product_family_code = EXCLUDED.product_family_code,
    interchange_rule = EXCLUDED.interchange_rule,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO location_vaccine_prices (id, location_id, vaccine_id, retail_price, stock_status, status, created_at)
SELECT (loc.id - 1) * 100 + 30, loc.id, 30, 0.00, TRUE, 2, CURRENT_TIMESTAMP
FROM locations loc
WHERE loc.id IN (1, 2)
ON CONFLICT (id) DO UPDATE SET
    location_id = EXCLUDED.location_id,
    vaccine_id = EXCLUDED.vaccine_id,
    retail_price = EXCLUDED.retail_price,
    stock_status = EXCLUDED.stock_status,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

-- Generic options: each active vaccine that covers a disease can be selected for each configured dose of that disease.
INSERT INTO child_disease_dose_vaccine_options (
    disease_id,
    dose_schedule_id,
    dose_order,
    vaccine_id,
    is_preferred,
    display_order,
    note,
    status,
    created_at
)
SELECT
    s.disease_id,
    s.id,
    s.dose_order,
    c.vaccine_id,
    FALSE,
    ROW_NUMBER() OVER (PARTITION BY s.disease_id, s.dose_order ORDER BY c.product_family_code, c.vaccine_id),
    NULL,
    2,
    CURRENT_TIMESTAMP
FROM child_disease_dose_schedules s
JOIN vaccine_disease_coverage c ON c.disease_id = s.disease_id AND c.status = 2
JOIN vaccines v ON v.id = c.vaccine_id AND v.status = 2
WHERE s.status = 2
ON CONFLICT (disease_id, dose_order, vaccine_id) DO UPDATE SET
    dose_schedule_id = EXCLUDED.dose_schedule_id,
    display_order = EXCLUDED.display_order,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

-- Hepatitis B has a special birth dose; keep it distinct from later combination-vaccine doses.
UPDATE child_disease_dose_vaccine_options o
SET status = 0, updated_at = CURRENT_TIMESTAMP
FROM child_vaccine_diseases d
WHERE o.disease_id = d.id
  AND d.code = 'HEPATITIS_B'
  AND o.dose_order = 1
  AND o.vaccine_id <> 22;

UPDATE child_disease_dose_vaccine_options o
SET is_preferred = TRUE,
    display_order = 1,
    note = 'Mũi viêm gan B sơ sinh trong 24 giờ đầu sau sinh nếu đủ điều kiện.',
    status = 2,
    updated_at = CURRENT_TIMESTAMP
FROM child_vaccine_diseases d
WHERE o.disease_id = d.id
  AND d.code = 'HEPATITIS_B'
  AND o.dose_order = 1
  AND o.vaccine_id = 22;

UPDATE child_disease_dose_vaccine_options o
SET is_preferred = TRUE,
    note = 'Có thể dùng vaccine 6 trong 1 cho các mũi sau sơ sinh theo lịch.',
    updated_at = CURRENT_TIMESTAMP
FROM child_vaccine_diseases d
WHERE o.disease_id = d.id
  AND d.code = 'HEPATITIS_B'
  AND o.dose_order IN (2, 3, 4)
  AND o.vaccine_id IN (1, 7);

UPDATE child_disease_dose_vaccine_options o
SET is_preferred = TRUE,
    note = 'Vaccine 5 trong 1 phù hợp cho bạch hầu, ho gà, uốn ván, bại liệt và Hib; không thay thế mũi viêm gan B.',
    updated_at = CURRENT_TIMESTAMP
FROM child_vaccine_diseases d
WHERE o.disease_id = d.id
  AND d.code IN ('DIPHTHERIA', 'PERTUSSIS', 'TETANUS', 'POLIO', 'HIB')
  AND o.vaccine_id = 30;

SELECT setval(pg_get_serial_sequence('vaccines', 'id'), COALESCE((SELECT MAX(id) FROM vaccines), 1), true);
SELECT setval(pg_get_serial_sequence('child_disease_dose_vaccine_options', 'id'), COALESCE((SELECT MAX(id) FROM child_disease_dose_vaccine_options), 1), true);
