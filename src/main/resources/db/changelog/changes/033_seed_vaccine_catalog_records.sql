-- liquibase formatted sql

-- changeset codex:113
-- validCheckSum: ANY
INSERT INTO "locations" ("id", "name", "region", "address", "status", "created_at")
VALUES
    (1, 'VNVC Trường Chinh', 'Miền Bắc', '180 Trường Chinh, Đống Đa, Hà Nội', 2, CURRENT_TIMESTAMP),
    (2, 'VNVC Nguyễn Duy Trinh', 'Miền Nam', 'Nguyễn Duy Trinh, TP. Thủ Đức, TP.HCM', 2, CURRENT_TIMESTAMP),
    (3, 'Trung tâm Y tế Dự phòng Hà Nội', 'Miền Bắc', '70 Nguyễn Chí Thanh, Đống Đa, Hà Nội', 2, CURRENT_TIMESTAMP)
ON CONFLICT ("id") DO UPDATE SET
    "name" = EXCLUDED."name",
    "region" = EXCLUDED."region",
    "address" = EXCLUDED."address",
    "status" = EXCLUDED."status",
    "updated_at" = CURRENT_TIMESTAMP;

-- changeset codex:114
-- validCheckSum: ANY
INSERT INTO "vaccines" ("id", "name", "manufacturer", "origin", "description", "status", "created_at")
VALUES
    (1, 'Hexaxim', 'Sanofi', 'Pháp', 'Vắc-xin 6 trong 1 phòng bạch hầu, ho gà, uốn ván, bại liệt, viêm gan B và Hib.', 2, CURRENT_TIMESTAMP),
    (2, 'Rotarix', 'GSK', 'Bỉ', 'Vắc-xin uống phòng tiêu chảy do Rotavirus.', 2, CURRENT_TIMESTAMP),
    (3, 'Varilrix', 'GSK', 'Bỉ', 'Vắc-xin phòng bệnh thủy đậu.', 2, CURRENT_TIMESTAMP),
    (4, 'Vaxigrip Tetra', 'Sanofi', 'Pháp', 'Vắc-xin phòng cúm mùa.', 2, CURRENT_TIMESTAMP),
    (5, 'MMR II', 'MSD', 'Mỹ', 'Vắc-xin phòng sởi, quai bị và rubella.', 2, CURRENT_TIMESTAMP)
ON CONFLICT ("id") DO UPDATE SET
    "name" = EXCLUDED."name",
    "manufacturer" = EXCLUDED."manufacturer",
    "origin" = EXCLUDED."origin",
    "description" = EXCLUDED."description",
    "status" = EXCLUDED."status",
    "updated_at" = CURRENT_TIMESTAMP;

-- changeset codex:115
-- validCheckSum: ANY
INSERT INTO "vaccine_packages" ("id", "code", "name", "description", "status", "created_at")
VALUES
    (1, 'GOI_01', 'Gói 1: Hexaxim - Rotarix - Varilrix', 'Gói vắc-xin cơ bản cho trẻ nhỏ theo các mốc tháng đầu đời.', 2, CURRENT_TIMESTAMP)
ON CONFLICT ("id") DO UPDATE SET
    "code" = EXCLUDED."code",
    "name" = EXCLUDED."name",
    "description" = EXCLUDED."description",
    "status" = EXCLUDED."status",
    "updated_at" = CURRENT_TIMESTAMP;

INSERT INTO "package_structures" (
    "id",
    "package_id",
    "duration_months",
    "vaccine_id",
    "recommended_age_months",
    "dosage_order",
    "dose_label",
    "note"
)
VALUES
    (1, 1, 7, 1, 2, 1, 'Mũi 1', NULL),
    (2, 1, 7, 1, 3, 2, 'Mũi 2', NULL),
    (3, 1, 7, 2, 2, 1, 'Liều 1', 'Uống'),
    (4, 1, 7, 2, 3, 2, 'Liều 2', 'Uống'),
    (5, 1, 24, 3, 12, 1, 'Mũi 1', NULL),
    (6, 1, 24, 5, 12, 1, 'Mũi 1', NULL)
ON CONFLICT ("id") DO UPDATE SET
    "package_id" = EXCLUDED."package_id",
    "duration_months" = EXCLUDED."duration_months",
    "vaccine_id" = EXCLUDED."vaccine_id",
    "recommended_age_months" = EXCLUDED."recommended_age_months",
    "dosage_order" = EXCLUDED."dosage_order",
    "dose_label" = EXCLUDED."dose_label",
    "note" = EXCLUDED."note";

-- changeset codex:116
-- validCheckSum: ANY
INSERT INTO "location_vaccine_prices" ("id", "location_id", "vaccine_id", "retail_price", "stock_status", "status", "created_at")
VALUES
    (1, 1, 1, 1050000.00, true, 2, CURRENT_TIMESTAMP),
    (2, 1, 2, 750000.00, true, 2, CURRENT_TIMESTAMP),
    (3, 1, 3, 950000.00, true, 2, CURRENT_TIMESTAMP),
    (4, 1, 4, 350000.00, true, 2, CURRENT_TIMESTAMP),
    (5, 1, 5, 450000.00, true, 2, CURRENT_TIMESTAMP),
    (6, 2, 1, 1080000.00, true, 2, CURRENT_TIMESTAMP),
    (7, 2, 2, 760000.00, false, 2, CURRENT_TIMESTAMP)
ON CONFLICT ("id") DO UPDATE SET
    "location_id" = EXCLUDED."location_id",
    "vaccine_id" = EXCLUDED."vaccine_id",
    "retail_price" = EXCLUDED."retail_price",
    "stock_status" = EXCLUDED."stock_status",
    "status" = EXCLUDED."status",
    "updated_at" = CURRENT_TIMESTAMP;

INSERT INTO "location_package_prices" (
    "id",
    "location_id",
    "package_id",
    "duration_months",
    "base_vaccine_sum",
    "service_fee",
    "discount_amount",
    "final_package_price",
    "gift_description",
    "created_at"
)
VALUES
    (1, 1, 1, 7, 3600000.00, 0.00, 250000.00, 3350000.00, 'Balo BabyOi', CURRENT_TIMESTAMP),
    (2, 1, 1, 24, 5400000.00, 0.00, 500000.00, 4900000.00, 'Balo và áo mưa BabyOi', CURRENT_TIMESTAMP),
    (3, 2, 1, 7, 3680000.00, 0.00, 200000.00, 3480000.00, 'Balo BabyOi', CURRENT_TIMESTAMP)
ON CONFLICT ("id") DO UPDATE SET
    "location_id" = EXCLUDED."location_id",
    "package_id" = EXCLUDED."package_id",
    "duration_months" = EXCLUDED."duration_months",
    "base_vaccine_sum" = EXCLUDED."base_vaccine_sum",
    "service_fee" = EXCLUDED."service_fee",
    "discount_amount" = EXCLUDED."discount_amount",
    "final_package_price" = EXCLUDED."final_package_price",
    "gift_description" = EXCLUDED."gift_description",
    "updated_at" = CURRENT_TIMESTAMP;

-- changeset codex:117
-- validCheckSum: ANY
INSERT INTO "vaccine_record" (
    "id",
    "profile_id",
    "location_id",
    "vaccine_id",
    "package_id",
    "package_structure_id",
    "injection_date",
    "actual_injection_date",
    "price",
    "note",
    "created_at",
    "created_by",
    "status"
)
VALUES
    (1, 1, 1, 1, 1, 1, '2026-06-20', NULL, 1050000.00, 'Mũi 1 - Hexaxim', CURRENT_DATE, 1, -1),
    (2, 1, 1, 2, 1, 3, '2026-07-05', NULL, 750000.00, 'Liều uống Rotarix', CURRENT_DATE, 1, -1),
    (3, 1, 1, 4, NULL, NULL, '2026-05-20', '2026-05-20', 350000.00, 'Đã tiêm cúm mùa', CURRENT_DATE, 1, 3)
ON CONFLICT ("id") DO UPDATE SET
    "profile_id" = EXCLUDED."profile_id",
    "location_id" = EXCLUDED."location_id",
    "vaccine_id" = EXCLUDED."vaccine_id",
    "package_id" = EXCLUDED."package_id",
    "package_structure_id" = EXCLUDED."package_structure_id",
    "injection_date" = EXCLUDED."injection_date",
    "actual_injection_date" = EXCLUDED."actual_injection_date",
    "price" = EXCLUDED."price",
    "note" = EXCLUDED."note",
    "status" = EXCLUDED."status",
    "updated_at" = CURRENT_DATE,
    "updated_by" = 1;

-- changeset codex:118
-- validCheckSum: ANY
SELECT setval(pg_get_serial_sequence('"locations"', 'id'), COALESCE((SELECT MAX("id") FROM "locations"), 1), true);
SELECT setval(pg_get_serial_sequence('"vaccines"', 'id'), COALESCE((SELECT MAX("id") FROM "vaccines"), 1), true);
SELECT setval(pg_get_serial_sequence('"vaccine_packages"', 'id'), COALESCE((SELECT MAX("id") FROM "vaccine_packages"), 1), true);
SELECT setval(pg_get_serial_sequence('"package_structures"', 'id'), COALESCE((SELECT MAX("id") FROM "package_structures"), 1), true);
SELECT setval(pg_get_serial_sequence('"location_vaccine_prices"', 'id'), COALESCE((SELECT MAX("id") FROM "location_vaccine_prices"), 1), true);
SELECT setval(pg_get_serial_sequence('"location_package_prices"', 'id'), COALESCE((SELECT MAX("id") FROM "location_package_prices"), 1), true);
SELECT setval(pg_get_serial_sequence('"vaccine_record"', 'id'), COALESCE((SELECT MAX("id") FROM "vaccine_record"), 1), true);
