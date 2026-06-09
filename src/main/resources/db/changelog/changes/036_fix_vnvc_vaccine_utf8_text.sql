-- liquibase formatted sql

-- changeset codex:129
-- reruns VNVC vaccine demo seed with verified UTF-8 Vietnamese text
-- validCheckSum: ANY
UPDATE "profile"
SET
    "date_of_birth" = CASE
        WHEN "id" = 1 THEN DATE '2025-06-09'
        WHEN "id" = 2 THEN DATE '2026-02-09'
        ELSE "date_of_birth"
    END,
    "updated_at" = CURRENT_TIMESTAMP,
    "updated_by" = 'SYSTEM'
WHERE "id" IN (1, 2);

-- changeset codex:130
-- validCheckSum: ANY
INSERT INTO "locations" ("id", "name", "region", "address", "status", "created_at")
VALUES
    (1, 'VNVC Tr??ng Chinh', 'Mi?n B?c', '180 Tr??ng Chinh, ??ng ?a, H? N?i', 2, CURRENT_TIMESTAMP),
    (2, 'VNVC Nguy?n Duy Trinh', 'Mi?n Nam', 'Nguy?n Duy Trinh, TP Th? ??c, TP HCM', 2, CURRENT_TIMESTAMP)
ON CONFLICT ("id") DO UPDATE SET
    "name" = EXCLUDED."name",
    "region" = EXCLUDED."region",
    "address" = EXCLUDED."address",
    "status" = EXCLUDED."status",
    "updated_at" = CURRENT_TIMESTAMP;

INSERT INTO "vaccines" ("id", "name", "manufacturer", "origin", "description", "status", "created_at")
VALUES
    (1, 'Hexaxim', 'Sanofi', 'Ph?p', '6 trong 1: b?ch h?u, ho g?, u?n v?n, b?i li?t, Hib, vi?m gan B.', 2, CURRENT_TIMESTAMP),
    (2, 'Rotarix', 'GSK', 'B?', 'Ph?ng ti?u ch?y c?p do Rotavirus.', 2, CURRENT_TIMESTAMP),
    (3, 'Varilrix', 'GSK', 'B?', 'Ph?ng th?y ??u.', 2, CURRENT_TIMESTAMP),
    (4, 'Vaxigrip Tetra', 'Sanofi', 'Ph?p', 'Ph?ng c?m m?a.', 2, CURRENT_TIMESTAMP),
    (5, 'MMR II', 'MSD', 'M?', 'Ph?ng s?i, quai b?, rubella.', 2, CURRENT_TIMESTAMP),
    (6, 'Rotateq', 'MSD', 'M?', 'Ph?ng ti?u ch?y c?p do Rotavirus.', 2, CURRENT_TIMESTAMP),
    (7, 'Infanrix Hexa', 'GSK', 'B?', '6 trong 1: b?ch h?u, ho g?, u?n v?n, b?i li?t, Hib, vi?m gan B.', 2, CURRENT_TIMESTAMP),
    (8, 'Prevenar 20', 'Pfizer', 'B?', 'Ph?ng c?c b?nh do ph? c?u khu?n.', 2, CURRENT_TIMESTAMP),
    (9, 'Bexsero', 'GSK', '?', 'Ph?ng n?o m? c?u nh?m B.', 2, CURRENT_TIMESTAMP),
    (10, 'MenQuadfi', 'Sanofi', 'M?', 'Phong nao mo cau A, C, Y, W-135.', 2, CURRENT_TIMESTAMP),
    (11, 'Nimenrix', 'Pfizer', 'B?', 'Phong nao mo cau A, C, Y, W-135.', 2, CURRENT_TIMESTAMP),
    (12, 'MVVac', 'Polyvac', 'Vi?t Nam', 'Ph?ng s?i.', 2, CURRENT_TIMESTAMP),
    (13, 'Imojev', 'Sanofi', 'Thai Lan', 'Ph?ng vi?m n?o Nh?t B?n.', 2, CURRENT_TIMESTAMP),
    (14, 'Priorix', 'GSK', 'B?', 'Ph?ng s?i, quai b?, rubella.', 2, CURRENT_TIMESTAMP),
    (15, 'Twinrix', 'GSK', 'B?', 'Ph?ng vi?m gan A v? vi?m gan B.', 2, CURRENT_TIMESTAMP),
    (16, 'Typhoid Vi', 'Davac', 'Vi?t Nam', 'Ph?ng th??ng h?n.', 2, CURRENT_TIMESTAMP),
    (17, 'mOrcvax', 'Vabiotech', 'Vi?t Nam', 'Ph?ng b?nh t?.', 2, CURRENT_TIMESTAMP),
    (18, 'Varivax', 'MSD', 'M?', 'Ph?ng th?y ??u.', 2, CURRENT_TIMESTAMP),
    (19, 'ProQuad', 'MSD', 'M?', 'Ph?ng s?i, quai b?, rubella, th?y ??u.', 2, CURRENT_TIMESTAMP),
    (20, 'Avaxim 80U/0.5ml', 'Sanofi', 'Ph?p', 'Ph?ng vi?m gan A cho tr? em.', 2, CURRENT_TIMESTAMP),
    (21, 'BCG', 'Vi?t Nam', 'Vi?t Nam', 'Ph?ng lao cho tr? s? sinh.', 2, CURRENT_TIMESTAMP),
    (22, 'V?c xin vi?m gan B s? sinh', 'Nhi?u h?ng', 'Nhi?u n??c', 'Ph?ng vi?m gan B trong 24 gi? ??u sau sinh.', 2, CURRENT_TIMESTAMP),
    (23, 'Synflorix', 'GSK', 'B?', 'Ph?ng c?c b?nh do ph? c?u khu?n.', 2, CURRENT_TIMESTAMP),
    (24, 'Prevenar 13', 'Pfizer', 'B?', 'Ph?ng c?c b?nh do ph? c?u khu?n.', 2, CURRENT_TIMESTAMP),
    (25, 'Vaxneuvance', 'MSD', 'M?', 'Ph?ng c?c b?nh do ph? c?u khu?n.', 2, CURRENT_TIMESTAMP),
    (26, 'VA-MENGOC-BC', 'Finlay', 'Cuba', 'Ph?ng n?o m? c?u nh?m B v? C.', 2, CURRENT_TIMESTAMP),
    (27, 'Menactra', 'Sanofi', 'M?', 'Phong nao mo cau A, C, Y, W-135.', 2, CURRENT_TIMESTAMP),
    (28, 'Jevax', 'Vabiotech', 'Vi?t Nam', 'Ph?ng vi?m n?o Nh?t B?n.', 2, CURRENT_TIMESTAMP),
    (29, 'Pneumovax 23', 'MSD', 'M?', 'Ph?ng c?c b?nh do ph? c?u khu?n.', 2, CURRENT_TIMESTAMP)
ON CONFLICT ("id") DO UPDATE SET
    "name" = EXCLUDED."name",
    "manufacturer" = EXCLUDED."manufacturer",
    "origin" = EXCLUDED."origin",
    "description" = EXCLUDED."description",
    "status" = EXCLUDED."status",
    "updated_at" = CURRENT_TIMESTAMP;

-- Gi? l? ?? 0 v? c?c trang VNVC tham chi?u ch?a c?ng b? gi? l? theo t?ng trung t?m.
DELETE FROM "location_vaccine_prices"
WHERE "location_id" IN (1, 2);

INSERT INTO "location_vaccine_prices" ("id", "location_id", "vaccine_id", "retail_price", "stock_status", "status", "created_at")
SELECT
    seed."id",
    seed."location_id",
    seed."vaccine_id",
    0.00,
    true,
    2,
    CURRENT_TIMESTAMP
FROM (
    SELECT
        (loc."id" - 1) * 100 + vaccine."id" AS "id",
        loc."id" AS "location_id",
        vaccine."id" AS "vaccine_id"
    FROM "locations" loc
    CROSS JOIN "vaccines" vaccine
    WHERE loc."id" IN (1, 2)
      AND vaccine."id" BETWEEN 1 AND 29
) seed
ON CONFLICT ("id") DO UPDATE SET
    "location_id" = EXCLUDED."location_id",
    "vaccine_id" = EXCLUDED."vaccine_id",
    "retail_price" = EXCLUDED."retail_price",
    "stock_status" = EXCLUDED."stock_status",
    "status" = EXCLUDED."status",
    "updated_at" = CURRENT_TIMESTAMP;

-- changeset codex:131
-- validCheckSum: ANY
INSERT INTO "vaccine_packages" ("id", "code", "name", "description", "status", "created_at")
VALUES
    (1, 'GOI_01', 'G?i 1: Hexaxim - Rotarix - Varilrix - MenQuadfi', 'G?i v?c xin VNVC d?nh cho tr? 0-2 tu?i, ngu?n t? trang g?i v?c xin.', 2, CURRENT_TIMESTAMP),
    (2, 'GOI_02', 'G?i 2: Hexaxim - Rotateq - Varilrix - MenQuadfi', 'G?i v?c xin VNVC d?nh cho tr? 0-2 tu?i, ngu?n t? trang g?i v?c xin.', 2, CURRENT_TIMESTAMP),
    (3, 'GOI_03', 'G?i 3: Infanrix Hexa - Rotarix - Varilrix - MenQuadfi', 'G?i v?c xin VNVC d?nh cho tr? 0-2 tu?i, ngu?n t? trang g?i v?c xin.', 2, CURRENT_TIMESTAMP),
    (4, 'GOI_04', 'G?i 4: Infanrix Hexa - Rotateq - Varilrix - MenQuadfi', 'G?i v?c xin VNVC d?nh cho tr? 0-2 tu?i, ngu?n t? trang g?i v?c xin.', 2, CURRENT_TIMESTAMP),
    (5, 'GOI_05', 'G?i 5: Hexaxim - Rotarix - Varivax - MenQuadfi', 'T?n g?i l?y t? m?c l?c trang g?i v?c xin VNVC.', 2, CURRENT_TIMESTAMP),
    (6, 'GOI_06', 'G?i 6: Hexaxim - Rotateq - Varivax - MenQuadfi', 'T?n g?i l?y t? m?c l?c trang g?i v?c xin VNVC.', 2, CURRENT_TIMESTAMP),
    (7, 'GOI_07', 'G?i 7: Infanrix Hexa - Rotarix - Varivax - MenQuadfi', 'T?n g?i l?y t? m?c l?c trang g?i v?c xin VNVC.', 2, CURRENT_TIMESTAMP),
    (8, 'GOI_08', 'G?i 8: Infanrix Hexa - Rotateq - Varivax - MenQuadfi', 'T?n g?i l?y t? m?c l?c trang g?i v?c xin VNVC.', 2, CURRENT_TIMESTAMP),
    (9, 'GOI_09', 'G?i 9: Infanrix Hexa - Rotateq - Proquad', 'T?n g?i l?y t? m?c l?c trang g?i v?c xin VNVC.', 2, CURRENT_TIMESTAMP),
    (10, 'GOI_10', 'G?i 10: Infanrix Hexa - Rotarix - Proquad', 'T?n g?i l?y t? m?c l?c trang g?i v?c xin VNVC.', 2, CURRENT_TIMESTAMP),
    (11, 'GOI_11', 'G?i 11: Hexaxim - Rotateq - Proquad', 'T?n g?i l?y t? m?c l?c trang g?i v?c xin VNVC.', 2, CURRENT_TIMESTAMP),
    (12, 'GOI_12', 'G?i 12: Hexaxim - Rotarix - Proquad', 'T?n g?i l?y t? m?c l?c trang g?i v?c xin VNVC.', 2, CURRENT_TIMESTAMP),
    (13, 'GOI_13', 'G?i 13: Infanrix Hexa - Rotateq - Varivax', 'T?n g?i l?y t? m?c l?c trang g?i v?c xin VNVC.', 2, CURRENT_TIMESTAMP),
    (14, 'GOI_14', 'G?i 14: Infanrix Hexa - Rotarix - Varivax', 'T?n g?i l?y t? m?c l?c trang g?i v?c xin VNVC.', 2, CURRENT_TIMESTAMP),
    (15, 'GOI_15', 'G?i 15: Hexaxim - Rotateq - Varivax', 'T?n g?i l?y t? m?c l?c trang g?i v?c xin VNVC.', 2, CURRENT_TIMESTAMP),
    (16, 'GOI_16', 'G?i 16: Hexaxim - Rotarix - Varivax', 'T?n g?i l?y t? m?c l?c trang g?i v?c xin VNVC.', 2, CURRENT_TIMESTAMP),
    (17, 'GOI_17', 'G?i 17: Infanrix Hexa - Rotateq - Varilrix', 'T?n g?i l?y t? m?c l?c trang g?i v?c xin VNVC.', 2, CURRENT_TIMESTAMP),
    (18, 'GOI_18', 'G?i 18: Infanrix Hexa - Rotarix - Varilrix', 'T?n g?i l?y t? m?c l?c trang g?i v?c xin VNVC.', 2, CURRENT_TIMESTAMP),
    (19, 'GOI_19', 'G?i 19: Hexaxim - Rotateq - Varilrix', 'T?n g?i l?y t? m?c l?c trang g?i v?c xin VNVC.', 2, CURRENT_TIMESTAMP),
    (20, 'GOI_20', 'G?i 20: Hexaxim - Rotarix - Varilrix', 'G?i v?c xin VNVC d?nh cho tr? 0-2 tu?i, ngu?n t? trang g?i v?c xin.', 2, CURRENT_TIMESTAMP)
ON CONFLICT ("id") DO UPDATE SET
    "code" = EXCLUDED."code",
    "name" = EXCLUDED."name",
    "description" = EXCLUDED."description",
    "status" = EXCLUDED."status",
    "updated_at" = CURRENT_TIMESTAMP;

DELETE FROM "vaccine_record"
WHERE "profile_id" IN (1, 2);

DELETE FROM "package_structures";

DELETE FROM "location_package_prices"
WHERE "location_id" IN (1, 2);

-- Trang VNVC c?ng khai kh?ng t?ch gi? theo trung t?m; d? li?u n?y ?p d?ng cho trung t?m demo 1 v? 2.
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
SELECT
    ((src."location_id" - 1) * 1000 + src."package_id" * 10 + src."duration_months") AS "id",
    src."location_id",
    src."package_id",
    src."duration_months",
    src."package_price",
    0.00,
    src."discount_amount",
    src."final_package_price",
    src."gift_description",
    CURRENT_TIMESTAMP
FROM (
    VALUES
        (1, 1, 7, 19965600.00, 1197936.00, 18767664.00, '3 voucher 50.000 ??ng / Balo tr? em / ?o m?a / M? b?o hi?m l?n'),
        (1, 1, 10, 21652800.00, 1515696.00, 20137104.00, '4 voucher 50.000 ??ng / Balo ch?ng g? / 3 voucher 50.000 ??ng v? n?n b?o hi?m tr? c? s?ng'),
        (1, 1, 12, 31550400.00, 2208528.00, 29341872.00, 'Balo h?c sinh / T?i th?i trang'),
        (1, 1, 24, 37197600.00, 2603832.00, 34593768.00, 'Balo h?c sinh / T?i th?i trang'),
        (1, 2, 7, 20132400.00, 1409268.00, 18723132.00, '4 voucher 50.000 ??ng / Balo ch?ng g? / 3 voucher 50.000 ??ng v? n?n b?o hi?m tr? c? s?ng'),
        (1, 2, 10, 21819600.00, 1527372.00, 20292228.00, '4 voucher 50.000 ??ng / Balo ch?ng g? / 3 voucher 50.000 ??ng v? n?n b?o hi?m tr? c? s?ng'),
        (1, 2, 12, 31717200.00, 2220204.00, 29496996.00, 'Balo h?c sinh / T?i th?i trang'),
        (1, 2, 24, 37364400.00, 2615508.00, 34748892.00, 'Balo h?c sinh / T?i th?i trang'),
        (1, 3, 7, 19965600.00, 1197936.00, 18767664.00, '3 voucher 50.000 ??ng / Balo tr? em / ?o m?a / M? b?o hi?m l?n'),
        (1, 3, 10, 21652800.00, 1515696.00, 20137104.00, '4 voucher 50.000 ??ng / Balo ch?ng g? / 3 voucher 50.000 ??ng v? n?n b?o hi?m tr? c? s?ng'),
        (1, 3, 12, 31550400.00, 2208528.00, 29341872.00, 'Balo h?c sinh / T?i th?i trang'),
        (1, 3, 24, 37197600.00, 2603832.00, 34593768.00, 'Balo h?c sinh / T?i th?i trang'),
        (1, 4, 7, 20132400.00, 1409268.00, 18723132.00, '4 voucher 50.000 ??ng / Balo ch?ng g? / 3 voucher 50.000 ??ng v? n?n b?o hi?m tr? c? s?ng'),
        (1, 4, 10, 21819600.00, 1527372.00, 20292228.00, '4 voucher 50.000 ??ng / Balo ch?ng g? / 3 voucher 50.000 ??ng v? n?n b?o hi?m tr? c? s?ng'),
        (1, 4, 12, 31717200.00, 2220204.00, 29496996.00, 'Balo h?c sinh / T?i th?i trang'),
        (1, 4, 24, 37364400.00, 2615508.00, 34748892.00, 'Balo h?c sinh / T?i th?i trang'),
        (1, 20, 7, 21825600.00, 1527792.00, 20297808.00, '4 voucher 50.000 ??ng / Balo ch?ng g? / 3 voucher 50.000 ??ng v? n?n b?o hi?m tr? c? s?ng'),
        (1, 20, 10, 23512800.00, 1645896.00, 21866904.00, '4 voucher 50.000 ??ng / Balo ch?ng g? / 3 voucher 50.000 ??ng v? n?n b?o hi?m tr? c? s?ng'),
        (1, 20, 12, 33170400.00, 2321928.00, 30848472.00, 'Balo h?c sinh / T?i th?i trang'),
        (1, 20, 24, 38817600.00, 2717232.00, 36100368.00, 'Balo h?c sinh / T?i th?i trang'),
        (2, 1, 7, 19965600.00, 1197936.00, 18767664.00, '3 voucher 50.000 ??ng / Balo tr? em / ?o m?a / M? b?o hi?m l?n'),
        (2, 1, 10, 21652800.00, 1515696.00, 20137104.00, '4 voucher 50.000 ??ng / Balo ch?ng g? / 3 voucher 50.000 ??ng v? n?n b?o hi?m tr? c? s?ng'),
        (2, 1, 12, 31550400.00, 2208528.00, 29341872.00, 'Balo h?c sinh / T?i th?i trang'),
        (2, 1, 24, 37197600.00, 2603832.00, 34593768.00, 'Balo h?c sinh / T?i th?i trang'),
        (2, 2, 7, 20132400.00, 1409268.00, 18723132.00, '4 voucher 50.000 ??ng / Balo ch?ng g? / 3 voucher 50.000 ??ng v? n?n b?o hi?m tr? c? s?ng'),
        (2, 2, 10, 21819600.00, 1527372.00, 20292228.00, '4 voucher 50.000 ??ng / Balo ch?ng g? / 3 voucher 50.000 ??ng v? n?n b?o hi?m tr? c? s?ng'),
        (2, 2, 12, 31717200.00, 2220204.00, 29496996.00, 'Balo h?c sinh / T?i th?i trang'),
        (2, 2, 24, 37364400.00, 2615508.00, 34748892.00, 'Balo h?c sinh / T?i th?i trang'),
        (2, 3, 7, 19965600.00, 1197936.00, 18767664.00, '3 voucher 50.000 ??ng / Balo tr? em / ?o m?a / M? b?o hi?m l?n'),
        (2, 3, 10, 21652800.00, 1515696.00, 20137104.00, '4 voucher 50.000 ??ng / Balo ch?ng g? / 3 voucher 50.000 ??ng v? n?n b?o hi?m tr? c? s?ng'),
        (2, 3, 12, 31550400.00, 2208528.00, 29341872.00, 'Balo h?c sinh / T?i th?i trang'),
        (2, 3, 24, 37197600.00, 2603832.00, 34593768.00, 'Balo h?c sinh / T?i th?i trang'),
        (2, 4, 7, 20132400.00, 1409268.00, 18723132.00, '4 voucher 50.000 ??ng / Balo ch?ng g? / 3 voucher 50.000 ??ng v? n?n b?o hi?m tr? c? s?ng'),
        (2, 4, 10, 21819600.00, 1527372.00, 20292228.00, '4 voucher 50.000 ??ng / Balo ch?ng g? / 3 voucher 50.000 ??ng v? n?n b?o hi?m tr? c? s?ng'),
        (2, 4, 12, 31717200.00, 2220204.00, 29496996.00, 'Balo h?c sinh / T?i th?i trang'),
        (2, 4, 24, 37364400.00, 2615508.00, 34748892.00, 'Balo h?c sinh / T?i th?i trang'),
        (2, 20, 7, 21825600.00, 1527792.00, 20297808.00, '4 voucher 50.000 ??ng / Balo ch?ng g? / 3 voucher 50.000 ??ng v? n?n b?o hi?m tr? c? s?ng'),
        (2, 20, 10, 23512800.00, 1645896.00, 21866904.00, '4 voucher 50.000 ??ng / Balo ch?ng g? / 3 voucher 50.000 ??ng v? n?n b?o hi?m tr? c? s?ng'),
        (2, 20, 12, 33170400.00, 2321928.00, 30848472.00, 'Balo h?c sinh / T?i th?i trang'),
        (2, 20, 24, 38817600.00, 2717232.00, 36100368.00, 'Balo h?c sinh / T?i th?i trang')
) AS src ("location_id", "package_id", "duration_months", "package_price", "discount_amount", "final_package_price", "gift_description")
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

-- changeset codex:132
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
    (1001, 1, 1, 1, NULL, NULL, DATE '2025-08-09', DATE '2025-08-09', 0.00, 'H? s? 1 - b? 12 th?ng: Hexaxim m?i 1, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1002, 1, 1, 1, NULL, NULL, DATE '2025-09-09', DATE '2025-09-09', 0.00, 'H? s? 1 - b? 12 th?ng: Hexaxim m?i 2, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1003, 1, 1, 1, NULL, NULL, DATE '2025-10-09', DATE '2025-10-09', 0.00, 'H? s? 1 - b? 12 th?ng: Hexaxim m?i 3, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1004, 1, 1, 2, NULL, NULL, DATE '2025-07-23', DATE '2025-07-23', 0.00, 'H? s? 1 - b? 12 th?ng: Rotarix li?u 1, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1005, 1, 1, 2, NULL, NULL, DATE '2025-09-09', DATE '2025-09-09', 0.00, 'H? s? 1 - b? 12 th?ng: Rotarix li?u 2, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1006, 1, 1, 8, NULL, NULL, DATE '2025-07-23', DATE '2025-07-23', 0.00, 'H? s? 1 - b? 12 th?ng: Prevenar 20 m?i 1, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1007, 1, 1, 8, NULL, NULL, DATE '2025-09-09', DATE '2025-09-09', 0.00, 'H? s? 1 - b? 12 th?ng: Prevenar 20 m?i 2, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1008, 1, 1, 8, NULL, NULL, DATE '2025-10-09', DATE '2025-10-09', 0.00, 'H? s? 1 - b? 12 th?ng: Prevenar 20 m?i 3, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1009, 1, 1, 9, NULL, NULL, DATE '2025-08-09', DATE '2025-08-09', 0.00, 'H? s? 1 - b? 12 th?ng: Bexsero m?i 1, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1010, 1, 1, 9, NULL, NULL, DATE '2025-10-09', DATE '2025-10-09', 0.00, 'H? s? 1 - b? 12 th?ng: Bexsero m?i 2, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1011, 1, 1, 10, NULL, NULL, DATE '2025-07-23', DATE '2025-07-23', 0.00, 'H? s? 1 - b? 12 th?ng: MenQuadfi m?i 1, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1012, 1, 1, 10, NULL, NULL, DATE '2025-10-09', DATE '2025-10-09', 0.00, 'H? s? 1 - b? 12 th?ng: MenQuadfi m?i 2, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1013, 1, 1, 4, NULL, NULL, DATE '2025-12-09', DATE '2025-12-09', 0.00, 'H? s? 1 - b? 12 th?ng: Vaxigrip Tetra m?i 1, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1014, 1, 1, 12, NULL, NULL, DATE '2026-03-09', DATE '2026-03-09', 0.00, 'H? s? 1 - b? 12 th?ng: MVVac l?c 9 th?ng, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1015, 1, 1, 13, NULL, NULL, DATE '2026-03-09', DATE '2026-03-09', 0.00, 'H? s? 1 - b? 12 th?ng: Imojev m?i 1, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1016, 1, 1, 3, NULL, NULL, DATE '2026-03-09', DATE '2026-03-09', 0.00, 'H? s? 1 - b? 12 th?ng: Varilrix m?i 1, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1017, 1, 1, 14, NULL, NULL, DATE '2026-03-09', DATE '2026-03-09', 0.00, 'H? s? 1 - b? 12 th?ng: Priorix m?i 1, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (1018, 1, 1, 8, NULL, NULL, DATE '2026-06-09', NULL, 0.00, 'H? s? 1 - b? 12 th?ng: Prevenar 20 m?i nh?c ??n h?n h?m nay, d? li?u demo m?i l?', CURRENT_DATE, 1, -1),
    (1019, 1, 1, 9, NULL, NULL, DATE '2026-06-09', NULL, 0.00, 'H? s? 1 - b? 12 th?ng: Bexsero m?i 3 ??n h?n h?m nay, d? li?u demo m?i l?', CURRENT_DATE, 1, -1),
    (1020, 1, 1, 3, NULL, NULL, DATE '2026-06-09', NULL, 0.00, 'H? s? 1 - b? 12 th?ng: Varilrix m?i 2 ??n h?n h?m nay, d? li?u demo m?i l?', CURRENT_DATE, 1, -1),
    (1021, 1, 1, 15, NULL, NULL, DATE '2026-06-09', NULL, 0.00, 'H? s? 1 - b? 12 th?ng: Twinrix m?i 1 ??n h?n h?m nay, d? li?u demo m?i l?', CURRENT_DATE, 1, -1),
    (1022, 1, 1, 20, NULL, NULL, DATE '2026-06-09', NULL, 0.00, 'H? s? 1 - b? 12 th?ng: Avaxim m?i 1 ??n h?n h?m nay, d? li?u demo m?i l?', CURRENT_DATE, 1, -1),
    (1023, 1, 1, 1, NULL, NULL, DATE '2026-12-09', NULL, 0.00, 'H? s? 1 - m?c 18 th?ng: Hexaxim m?i nh?c s?p t?i, d? li?u demo m?i l?', CURRENT_DATE, 1, -1),
    (2001, 2, 1, 2, NULL, NULL, DATE '2026-03-23', DATE '2026-03-23', 0.00, 'H? s? 2 - b? 4 th?ng: Rotarix li?u 1, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (2002, 2, 1, 2, NULL, NULL, DATE '2026-05-09', DATE '2026-05-09', 0.00, 'H? s? 2 - b? 4 th?ng: Rotarix li?u 2, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (2003, 2, 1, 1, NULL, NULL, DATE '2026-04-09', DATE '2026-04-09', 0.00, 'H? s? 2 - b? 4 th?ng: Hexaxim m?i 1, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (2004, 2, 1, 1, NULL, NULL, DATE '2026-05-09', DATE '2026-05-09', 0.00, 'H? s? 2 - b? 4 th?ng: Hexaxim m?i 2, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (2005, 2, 1, 8, NULL, NULL, DATE '2026-03-23', DATE '2026-03-23', 0.00, 'H? s? 2 - b? 4 th?ng: Prevenar 20 m?i 1, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (2006, 2, 1, 8, NULL, NULL, DATE '2026-05-09', DATE '2026-05-09', 0.00, 'H? s? 2 - b? 4 th?ng: Prevenar 20 m?i 2, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (2007, 2, 1, 9, NULL, NULL, DATE '2026-04-09', DATE '2026-04-09', 0.00, 'H? s? 2 - b? 4 th?ng: Bexsero m?i 1, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (2008, 2, 1, 10, NULL, NULL, DATE '2026-03-23', DATE '2026-03-23', 0.00, 'H? s? 2 - b? 4 th?ng: MenQuadfi m?i 1, d? li?u demo m?i l?', CURRENT_DATE, 1, 3),
    (2009, 2, 1, 1, NULL, NULL, DATE '2026-06-09', NULL, 0.00, 'H? s? 2 - b? 4 th?ng: Hexaxim m?i 3 ??n h?n h?m nay, d? li?u demo m?i l?', CURRENT_DATE, 1, -1),
    (2010, 2, 1, 8, NULL, NULL, DATE '2026-06-09', NULL, 0.00, 'H? s? 2 - b? 4 th?ng: Prevenar 20 m?i 3 ??n h?n h?m nay, d? li?u demo m?i l?', CURRENT_DATE, 1, -1),
    (2011, 2, 1, 9, NULL, NULL, DATE '2026-06-09', NULL, 0.00, 'H? s? 2 - b? 4 th?ng: Bexsero m?i 2 ??n h?n h?m nay, d? li?u demo m?i l?', CURRENT_DATE, 1, -1),
    (2012, 2, 1, 10, NULL, NULL, DATE '2026-06-09', NULL, 0.00, 'H? s? 2 - b? 4 th?ng: MenQuadfi m?i 2 ??n h?n h?m nay, d? li?u demo m?i l?', CURRENT_DATE, 1, -1),
    (2013, 2, 1, 4, NULL, NULL, DATE '2026-08-09', NULL, 0.00, 'H? s? 2 - m?c 6 th?ng: Vaxigrip Tetra s?p t?i, d? li?u demo m?i l?', CURRENT_DATE, 1, -1)
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

-- changeset codex:133
-- validCheckSum: ANY
SELECT setval(pg_get_serial_sequence('"locations"', 'id'), COALESCE((SELECT MAX("id") FROM "locations"), 1), true);
SELECT setval(pg_get_serial_sequence('"vaccines"', 'id'), COALESCE((SELECT MAX("id") FROM "vaccines"), 1), true);
SELECT setval(pg_get_serial_sequence('"vaccine_packages"', 'id'), COALESCE((SELECT MAX("id") FROM "vaccine_packages"), 1), true);
SELECT setval(pg_get_serial_sequence('"package_structures"', 'id'), COALESCE((SELECT MAX("id") FROM "package_structures"), 1), true);
SELECT setval(pg_get_serial_sequence('"location_vaccine_prices"', 'id'), COALESCE((SELECT MAX("id") FROM "location_vaccine_prices"), 1), true);
SELECT setval(pg_get_serial_sequence('"location_package_prices"', 'id'), COALESCE((SELECT MAX("id") FROM "location_package_prices"), 1), true);
SELECT setval(pg_get_serial_sequence('"vaccine_record"', 'id'), COALESCE((SELECT MAX("id") FROM "vaccine_record"), 1), true);
