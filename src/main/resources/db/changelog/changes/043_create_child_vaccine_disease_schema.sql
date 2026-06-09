--liquibase formatted sql

--changeset codex:043-create-child-vaccine-disease-schema
--validCheckSum: ANY
CREATE TABLE IF NOT EXISTS child_vaccine_diseases (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_required BOOLEAN DEFAULT TRUE,
    display_order INT,
    status BIGINT DEFAULT 2,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vaccine_disease_coverage (
    id BIGSERIAL PRIMARY KEY,
    vaccine_id BIGINT NOT NULL,
    disease_id BIGINT NOT NULL,
    product_family_code VARCHAR(100),
    interchange_rule BIGINT NOT NULL DEFAULT 2,
    status BIGINT DEFAULT 2,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_vaccine_disease_coverage_vaccine FOREIGN KEY (vaccine_id) REFERENCES vaccines(id),
    CONSTRAINT fk_vaccine_disease_coverage_disease FOREIGN KEY (disease_id) REFERENCES child_vaccine_diseases(id),
    CONSTRAINT uk_vaccine_disease_coverage UNIQUE (vaccine_id, disease_id)
);

CREATE TABLE IF NOT EXISTS child_disease_dose_schedules (
    id BIGSERIAL PRIMARY KEY,
    disease_id BIGINT NOT NULL,
    dose_order INT NOT NULL,
    recommended_age_months INT,
    interval_days INT,
    dose_label VARCHAR(255),
    note TEXT,
    status BIGINT DEFAULT 2,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_child_disease_dose_disease FOREIGN KEY (disease_id) REFERENCES child_vaccine_diseases(id),
    CONSTRAINT uk_child_disease_dose UNIQUE (disease_id, dose_order)
);

CREATE INDEX IF NOT EXISTS idx_vaccine_disease_coverage_vaccine_id ON vaccine_disease_coverage(vaccine_id);
CREATE INDEX IF NOT EXISTS idx_vaccine_disease_coverage_disease_id ON vaccine_disease_coverage(disease_id);
CREATE INDEX IF NOT EXISTS idx_child_disease_dose_disease_id ON child_disease_dose_schedules(disease_id);

INSERT INTO child_vaccine_diseases (code, name, description, is_required, display_order, status, created_at)
VALUES
    ('TUBERCULOSIS', 'Lao', 'Bệnh lao ở trẻ em.', TRUE, 1, 2, CURRENT_TIMESTAMP),
    ('HEPATITIS_B', 'Viêm gan B', 'Bệnh viêm gan B, gồm mũi sơ sinh và các mũi nhắc qua vắc-xin đơn hoặc phối hợp.', TRUE, 2, 2, CURRENT_TIMESTAMP),
    ('DIPHTHERIA', 'Bạch hầu', 'Bệnh bạch hầu, thường được phòng bằng vắc-xin phối hợp 5 trong 1 hoặc 6 trong 1.', TRUE, 3, 2, CURRENT_TIMESTAMP),
    ('PERTUSSIS', 'Ho gà', 'Bệnh ho gà, thường được phòng bằng vắc-xin phối hợp 5 trong 1 hoặc 6 trong 1.', TRUE, 4, 2, CURRENT_TIMESTAMP),
    ('TETANUS', 'Uốn ván', 'Bệnh uốn ván, thường được phòng bằng vắc-xin phối hợp 5 trong 1 hoặc 6 trong 1.', TRUE, 5, 2, CURRENT_TIMESTAMP),
    ('POLIO', 'Bại liệt', 'Bệnh bại liệt, có thể được phòng trong các vắc-xin phối hợp.', TRUE, 6, 2, CURRENT_TIMESTAMP),
    ('HIB', 'Hib', 'Các bệnh do Haemophilus influenzae type b.', TRUE, 7, 2, CURRENT_TIMESTAMP),
    ('ROTA', 'Tiêu chảy do Rota', 'Tiêu chảy cấp do Rotavirus.', TRUE, 8, 2, CURRENT_TIMESTAMP),
    ('PNEUMOCOCCAL', 'Phế cầu', 'Các bệnh do phế cầu khuẩn.', TRUE, 9, 2, CURRENT_TIMESTAMP),
    ('MENINGOCOCCAL_B', 'Não mô cầu nhóm B', 'Bệnh do não mô cầu nhóm B.', TRUE, 10, 2, CURRENT_TIMESTAMP),
    ('MENINGOCOCCAL_ACYW', 'Não mô cầu A, C, Y, W-135', 'Bệnh do não mô cầu các nhóm A, C, Y, W-135.', TRUE, 11, 2, CURRENT_TIMESTAMP),
    ('INFLUENZA', 'Cúm', 'Cúm mùa.', TRUE, 12, 2, CURRENT_TIMESTAMP),
    ('MEASLES', 'Sởi', 'Bệnh sởi.', TRUE, 13, 2, CURRENT_TIMESTAMP),
    ('MUMPS', 'Quai bị', 'Bệnh quai bị.', TRUE, 14, 2, CURRENT_TIMESTAMP),
    ('RUBELLA', 'Rubella', 'Bệnh rubella.', TRUE, 15, 2, CURRENT_TIMESTAMP),
    ('VARICELLA', 'Thủy đậu', 'Bệnh thủy đậu.', TRUE, 16, 2, CURRENT_TIMESTAMP),
    ('JAPANESE_ENCEPHALITIS', 'Viêm não Nhật Bản', 'Bệnh viêm não Nhật Bản.', TRUE, 17, 2, CURRENT_TIMESTAMP),
    ('HEPATITIS_A', 'Viêm gan A', 'Bệnh viêm gan A.', TRUE, 18, 2, CURRENT_TIMESTAMP),
    ('TYPHOID', 'Thương hàn', 'Bệnh thương hàn.', FALSE, 19, 2, CURRENT_TIMESTAMP),
    ('CHOLERA', 'Tả', 'Bệnh tả.', FALSE, 20, 2, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    is_required = EXCLUDED.is_required,
    display_order = EXCLUDED.display_order,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

CREATE TEMP TABLE child_disease_seed_doses (
    disease_code VARCHAR(100),
    dose_order INT,
    recommended_age_months INT,
    interval_days INT,
    dose_label VARCHAR(255),
    note TEXT
) ON COMMIT DROP;

INSERT INTO child_disease_seed_doses (disease_code, dose_order, recommended_age_months, interval_days, dose_label, note)
VALUES
    ('TUBERCULOSIS', 1, 0, NULL, 'Mũi 1', 'Tiêm càng sớm càng tốt sau sinh.'),
    ('HEPATITIS_B', 1, 0, NULL, 'Mũi sơ sinh', 'Tiêm trong 24 giờ đầu sau sinh nếu đủ điều kiện.'),
    ('HEPATITIS_B', 2, 2, 30, 'Mũi 2', 'Có thể được phủ bởi vắc-xin 6 trong 1.'),
    ('HEPATITIS_B', 3, 3, 30, 'Mũi 3', 'Có thể được phủ bởi vắc-xin 5 trong 1 hoặc 6 trong 1 phù hợp.'),
    ('HEPATITIS_B', 4, 4, 30, 'Mũi 4', 'Có thể được phủ bởi vắc-xin 6 trong 1.'),
    ('DIPHTHERIA', 1, 2, NULL, 'Mũi 1', NULL),
    ('DIPHTHERIA', 2, 3, 30, 'Mũi 2', NULL),
    ('DIPHTHERIA', 3, 4, 30, 'Mũi 3', NULL),
    ('DIPHTHERIA', 4, 18, 365, 'Mũi nhắc', NULL),
    ('PERTUSSIS', 1, 2, NULL, 'Mũi 1', NULL),
    ('PERTUSSIS', 2, 3, 30, 'Mũi 2', NULL),
    ('PERTUSSIS', 3, 4, 30, 'Mũi 3', NULL),
    ('PERTUSSIS', 4, 18, 365, 'Mũi nhắc', NULL),
    ('TETANUS', 1, 2, NULL, 'Mũi 1', NULL),
    ('TETANUS', 2, 3, 30, 'Mũi 2', NULL),
    ('TETANUS', 3, 4, 30, 'Mũi 3', NULL),
    ('TETANUS', 4, 18, 365, 'Mũi nhắc', NULL),
    ('POLIO', 1, 2, NULL, 'Mũi 1', NULL),
    ('POLIO', 2, 3, 30, 'Mũi 2', NULL),
    ('POLIO', 3, 4, 30, 'Mũi 3', NULL),
    ('POLIO', 4, 18, 365, 'Mũi nhắc', NULL),
    ('HIB', 1, 2, NULL, 'Mũi 1', NULL),
    ('HIB', 2, 3, 30, 'Mũi 2', NULL),
    ('HIB', 3, 4, 30, 'Mũi 3', NULL),
    ('HIB', 4, 18, 365, 'Mũi nhắc', NULL),
    ('ROTA', 1, 2, NULL, 'Liều 1', 'Hoàn thành sớm theo giới hạn tuổi của sản phẩm.'),
    ('ROTA', 2, 3, 30, 'Liều 2', NULL),
    ('ROTA', 3, 4, 30, 'Liều 3', 'Áp dụng khi dùng phác đồ 3 liều.'),
    ('PNEUMOCOCCAL', 1, 2, NULL, 'Mũi 1', NULL),
    ('PNEUMOCOCCAL', 2, 3, 30, 'Mũi 2', NULL),
    ('PNEUMOCOCCAL', 3, 4, 30, 'Mũi 3', NULL),
    ('PNEUMOCOCCAL', 4, 12, 240, 'Mũi nhắc', NULL),
    ('MENINGOCOCCAL_B', 1, 2, NULL, 'Mũi 1', NULL),
    ('MENINGOCOCCAL_B', 2, 4, 60, 'Mũi 2', NULL),
    ('MENINGOCOCCAL_B', 3, 12, 180, 'Mũi nhắc', NULL),
    ('MENINGOCOCCAL_ACYW', 1, 9, NULL, 'Mũi 1', NULL),
    ('MENINGOCOCCAL_ACYW', 2, 12, 90, 'Mũi 2', NULL),
    ('INFLUENZA', 1, 6, NULL, 'Mũi 1', 'Tiêm nhắc hằng năm.'),
    ('INFLUENZA', 2, 7, 30, 'Mũi 2 mùa đầu', 'Áp dụng với trẻ tiêm cúm lần đầu theo chỉ định.'),
    ('MEASLES', 1, 9, NULL, 'Mũi 1', NULL),
    ('MEASLES', 2, 18, 270, 'Mũi 2', 'Có thể được phủ bởi MMR hoặc MMRV.'),
    ('MUMPS', 1, 12, NULL, 'Mũi 1', 'Thường được phủ bởi MMR hoặc MMRV.'),
    ('MUMPS', 2, 18, 180, 'Mũi 2', NULL),
    ('RUBELLA', 1, 12, NULL, 'Mũi 1', 'Thường được phủ bởi MMR hoặc MMRV.'),
    ('RUBELLA', 2, 18, 180, 'Mũi 2', NULL),
    ('VARICELLA', 1, 12, NULL, 'Mũi 1', NULL),
    ('VARICELLA', 2, 48, 90, 'Mũi 2', NULL),
    ('JAPANESE_ENCEPHALITIS', 1, 9, NULL, 'Mũi 1', 'Không tự ý hoán đổi Jevax và Imojev.'),
    ('JAPANESE_ENCEPHALITIS', 2, 12, 365, 'Mũi 2', NULL),
    ('HEPATITIS_A', 1, 12, NULL, 'Mũi 1', NULL),
    ('HEPATITIS_A', 2, 18, 180, 'Mũi 2', NULL),
    ('TYPHOID', 1, 24, NULL, 'Mũi 1', NULL),
    ('CHOLERA', 1, 24, NULL, 'Liều 1', NULL),
    ('CHOLERA', 2, 24, 14, 'Liều 2', NULL);

INSERT INTO child_disease_dose_schedules (disease_id, dose_order, recommended_age_months, interval_days, dose_label, note, status, created_at)
SELECT d.id, s.dose_order, s.recommended_age_months, s.interval_days, s.dose_label, s.note, 2, CURRENT_TIMESTAMP
FROM child_disease_seed_doses s
JOIN child_vaccine_diseases d ON d.code = s.disease_code
ON CONFLICT (disease_id, dose_order) DO UPDATE SET
    recommended_age_months = EXCLUDED.recommended_age_months,
    interval_days = EXCLUDED.interval_days,
    dose_label = EXCLUDED.dose_label,
    note = EXCLUDED.note,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

CREATE TEMP TABLE vaccine_disease_seed_coverage (
    vaccine_id BIGINT,
    disease_code VARCHAR(100),
    product_family_code VARCHAR(100),
    interchange_rule BIGINT
) ON COMMIT DROP;

INSERT INTO vaccine_disease_seed_coverage (vaccine_id, disease_code, product_family_code, interchange_rule)
VALUES
    (1, 'DIPHTHERIA', 'HEXAVALENT', 1), (1, 'PERTUSSIS', 'HEXAVALENT', 1), (1, 'TETANUS', 'HEXAVALENT', 1),
    (1, 'POLIO', 'HEXAVALENT', 1), (1, 'HIB', 'HEXAVALENT', 1), (1, 'HEPATITIS_B', 'HEXAVALENT', 1),
    (7, 'DIPHTHERIA', 'HEXAVALENT', 1), (7, 'PERTUSSIS', 'HEXAVALENT', 1), (7, 'TETANUS', 'HEXAVALENT', 1),
    (7, 'POLIO', 'HEXAVALENT', 1), (7, 'HIB', 'HEXAVALENT', 1), (7, 'HEPATITIS_B', 'HEXAVALENT', 1),
    (22, 'HEPATITIS_B', 'HEPB_BIRTH', 2),
    (15, 'HEPATITIS_A', 'HEPA_HEPB_COMBO', 2), (15, 'HEPATITIS_B', 'HEPA_HEPB_COMBO', 2),
    (2, 'ROTA', 'RV1_ROTARIX', 2), (6, 'ROTA', 'RV5_ROTATEQ', 3),
    (8, 'PNEUMOCOCCAL', 'PCV20', 1), (23, 'PNEUMOCOCCAL', 'PCV10', 1), (24, 'PNEUMOCOCCAL', 'PCV13', 1),
    (25, 'PNEUMOCOCCAL', 'PCV15', 1), (29, 'PNEUMOCOCCAL', 'PPSV23', 2),
    (9, 'MENINGOCOCCAL_B', 'MENB_BEXSERO', 2), (26, 'MENINGOCOCCAL_B', 'MENBC', 2),
    (10, 'MENINGOCOCCAL_ACYW', 'MENACYW', 1), (11, 'MENINGOCOCCAL_ACYW', 'MENACYW', 1), (27, 'MENINGOCOCCAL_ACYW', 'MENACYW', 1),
    (4, 'INFLUENZA', 'FLU_TETRA', 6),
    (12, 'MEASLES', 'MEASLES_SINGLE', 2),
    (5, 'MEASLES', 'MMR', 1), (5, 'MUMPS', 'MMR', 1), (5, 'RUBELLA', 'MMR', 1),
    (14, 'MEASLES', 'MMR', 1), (14, 'MUMPS', 'MMR', 1), (14, 'RUBELLA', 'MMR', 1),
    (19, 'MEASLES', 'MMRV_COMBO', 1), (19, 'MUMPS', 'MMRV_COMBO', 1), (19, 'RUBELLA', 'MMRV_COMBO', 1), (19, 'VARICELLA', 'MMRV_COMBO', 1),
    (3, 'VARICELLA', 'VARICELLA', 1), (18, 'VARICELLA', 'VARICELLA', 1),
    (13, 'JAPANESE_ENCEPHALITIS', 'LIVE_IMOJEV', 4), (28, 'JAPANESE_ENCEPHALITIS', 'INACTIVATED_JEVAX', 4),
    (20, 'HEPATITIS_A', 'HEPATITIS_A', 1),
    (16, 'TYPHOID', 'TYPHOID_VI', 2),
    (17, 'CHOLERA', 'ORAL_CHOLERA', 2),
    (21, 'TUBERCULOSIS', 'BCG', 2);

INSERT INTO vaccine_disease_coverage (vaccine_id, disease_id, product_family_code, interchange_rule, status, created_at)
SELECT s.vaccine_id, d.id, s.product_family_code, s.interchange_rule, 2, CURRENT_TIMESTAMP
FROM vaccine_disease_seed_coverage s
JOIN vaccines v ON v.id = s.vaccine_id
JOIN child_vaccine_diseases d ON d.code = s.disease_code
ON CONFLICT (vaccine_id, disease_id) DO UPDATE SET
    product_family_code = EXCLUDED.product_family_code,
    interchange_rule = EXCLUDED.interchange_rule,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;
