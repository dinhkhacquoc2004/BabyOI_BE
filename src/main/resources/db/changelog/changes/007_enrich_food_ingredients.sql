-- liquibase formatted sql

-- changeset codex:025
WITH extra_ingredient_seed(name, image_url, base_amount, base_unit, calories, protein, carbs, fat, fiber, sugar, sodium) AS (
    VALUES
        ('Nước dùng nhạt', '/Ingredient/ingredient_nuoc-dung-nhat.jpg', 100, 'ml', 8, 0.5, 1.0, 0.2, 0, 0.2, 80),
        ('Nước lọc', '/Ingredient/ingredient_nuoc-loc.jpg', 100, 'ml', 0, 0, 0, 0, 0, 0, 0),
        ('Gừng', '/Ingredient/ingredient_gung.jpg', 100, 'g', 80, 1.8, 17.8, 0.8, 2, 1.7, 13),
        ('Hành tím', '/Ingredient/ingredient_hanh-tim.jpg', 100, 'g', 72, 2.5, 16.8, 0.1, 3.2, 7.9, 12),
        ('Tỏi', '/Ingredient/ingredient_toi.jpg', 100, 'g', 149, 6.4, 33.1, 0.5, 2.1, 1.0, 17),
        ('Hành lá', '/Ingredient/ingredient_hanh-la.jpg', 100, 'g', 32, 1.8, 7.3, 0.2, 2.6, 2.3, 16),
        ('Rau mùi', '/Ingredient/ingredient_rau-mui.jpg', 100, 'g', 23, 2.1, 3.7, 0.5, 2.8, 0.9, 46),
        ('Dầu ô liu', '/Ingredient/ingredient_dau-o-liu.jpg', 100, 'g', 884, 0, 0, 100, 0, 0, 2),
        ('Dầu mè', '/Ingredient/ingredient_dau-me.jpg', 100, 'g', 884, 0, 0, 100, 0, 0, 0),
        ('Dầu ăn dặm', '/Ingredient/ingredient_dau-an-dam.jpg', 100, 'g', 884, 0, 0, 100, 0, 0, 0),
        ('Nước cốt chanh', '/Ingredient/ingredient_nuoc-cot-chanh.jpg', 100, 'ml', 22, 0.4, 6.9, 0.2, 0.3, 2.5, 1)
)
INSERT INTO "food_ingredients" ("name_ingredients", "image_url", "created_at", "created_by", "updated_by", "updated_at", "status")
SELECT s.name, s.image_url, now(), 'system', 'system', now(), 2
FROM extra_ingredient_seed s
WHERE NOT EXISTS (
    SELECT 1 FROM "food_ingredients" fi WHERE fi."name_ingredients" = s.name
);

-- changeset codex:026
WITH extra_ingredient_seed(name, base_amount, base_unit, calories, protein, carbs, fat, fiber, sugar, sodium) AS (
    VALUES
        ('Nước dùng nhạt', 100, 'ml', 8, 0.5, 1.0, 0.2, 0, 0.2, 80),
        ('Nước lọc', 100, 'ml', 0, 0, 0, 0, 0, 0, 0),
        ('Gừng', 100, 'g', 80, 1.8, 17.8, 0.8, 2, 1.7, 13),
        ('Hành tím', 100, 'g', 72, 2.5, 16.8, 0.1, 3.2, 7.9, 12),
        ('Tỏi', 100, 'g', 149, 6.4, 33.1, 0.5, 2.1, 1.0, 17),
        ('Hành lá', 100, 'g', 32, 1.8, 7.3, 0.2, 2.6, 2.3, 16),
        ('Rau mùi', 100, 'g', 23, 2.1, 3.7, 0.5, 2.8, 0.9, 46),
        ('Dầu ô liu', 100, 'g', 884, 0, 0, 100, 0, 0, 2),
        ('Dầu mè', 100, 'g', 884, 0, 0, 100, 0, 0, 0),
        ('Dầu ăn dặm', 100, 'g', 884, 0, 0, 100, 0, 0, 0),
        ('Nước cốt chanh', 100, 'ml', 22, 0.4, 6.9, 0.2, 0.3, 2.5, 1)
)
INSERT INTO "ingredient_nutrition" (
    "food_ingredient_id", "base_amount", "base_unit",
    "calories", "calories_unit",
    "protein", "protein_unit",
    "carbs", "carbs_unit",
    "fat", "fat_unit",
    "fiber", "fiber_unit",
    "sugar", "sugar_unit",
    "sodium", "sodium_unit",
    "status"
)
SELECT
    fi."id", s.base_amount, s.base_unit,
    s.calories, 'kcal',
    s.protein, 'g',
    s.carbs, 'g',
    s.fat, 'g',
    s.fiber, 'g',
    s.sugar, 'g',
    s.sodium, 'mg',
    2
FROM extra_ingredient_seed s
JOIN "food_ingredients" fi ON fi."name_ingredients" = s.name
WHERE NOT EXISTS (
    SELECT 1 FROM "ingredient_nutrition" inu WHERE inu."food_ingredient_id" = fi."id"
);

-- changeset codex:027
WITH main_ingredient_rank AS (
    SELECT
        fli."food_lib_id",
        fl."function_code",
        fl."name" AS food_name,
        fi."name_ingredients",
        ROW_NUMBER() OVER (PARTITION BY fli."food_lib_id" ORDER BY fli."id") AS ingredient_order
    FROM "food_library_ingredients" fli
    JOIN "food_library" fl ON fl."id" = fli."food_lib_id"
    JOIN "food_ingredients" fi ON fi."id" = fli."food_ing_id"
    WHERE (fli."status" <> -4 OR fli."status" IS NULL)
      AND fi."name_ingredients" NOT IN (
          'Nước dùng nhạt', 'Nước lọc', 'Gừng', 'Hành tím', 'Tỏi', 'Hành lá',
          'Rau mùi', 'Dầu ô liu', 'Dầu mè', 'Dầu ăn dặm', 'Nước cốt chanh'
      )
),
food_profile AS (
    SELECT
        "food_lib_id",
        MAX("function_code") AS function_code,
        MAX(food_name) AS food_name,
        MAX(CASE WHEN ingredient_order = 1 THEN "name_ingredients" END) AS protein_name,
        MAX(CASE WHEN ingredient_order = 2 THEN "name_ingredients" END) AS carb_name,
        MAX(CASE WHEN ingredient_order = 3 THEN "name_ingredients" END) AS veg_name
    FROM main_ingredient_rank
    WHERE ingredient_order <= 3
    GROUP BY "food_lib_id"
),
extra_for_food AS (
    SELECT fp."food_lib_id", x.name, x.amount, x.unit, x.description
    FROM food_profile fp
    CROSS JOIN LATERAL (
        VALUES
            (
                CASE
                    WHEN fp.food_name ILIKE '%salad%' OR fp.food_name ILIKE '%bowl%' OR fp.food_name ILIKE '%trộn%' THEN 'Nước cốt chanh'
                    WHEN fp.function_code = 2 THEN 'Nước lọc'
                    ELSE 'Nước dùng nhạt'
                END,
                CASE
                    WHEN fp.food_name ILIKE '%salad%' OR fp.food_name ILIKE '%bowl%' OR fp.food_name ILIKE '%trộn%' THEN 5::double precision
                    WHEN fp.function_code = 2 THEN 50::double precision
                    ELSE 90::double precision
                END,
                CASE
                    WHEN fp.food_name ILIKE '%salad%' OR fp.food_name ILIKE '%bowl%' OR fp.food_name ILIKE '%trộn%' THEN 'ml'
                    WHEN fp.function_code = 2 THEN 'ml'
                    ELSE 'ml'
                END,
                CASE
                    WHEN fp.food_name ILIKE '%salad%' OR fp.food_name ILIKE '%bowl%' OR fp.food_name ILIKE '%trộn%' THEN 'Tạo vị chua nhẹ để món đỡ ngán, giúp phần đạm và rau củ tươi vị hơn mà không cần dùng sốt ngọt hoặc nhiều dầu.'
                    WHEN fp.function_code = 2 THEN 'Tạo độ loãng và độ mềm phù hợp cho bé; dùng để chỉnh kết cấu sau khi nghiền/xay, không làm món quá đặc hoặc khó nuốt.'
                    ELSE 'Tạo độ ẩm và vị nền nhẹ cho món; nên dùng loại nhạt để giữ vị tự nhiên của nguyên liệu chính và tránh làm món quá mặn.'
                END
            ),
            (
                CASE
                    WHEN fp.function_code = 2 THEN 'Dầu ăn dặm'
                    WHEN fp.food_name ILIKE '%mè%' THEN 'Dầu mè'
                    ELSE 'Dầu ô liu'
                END,
                CASE WHEN fp.function_code = 2 THEN 3::double precision ELSE 5::double precision END,
                'g',
                CASE
                    WHEN fp.function_code = 2 THEN 'Bổ sung một lượng chất béo nhỏ để món mềm và thơm hơn; chỉ cho vào cuối khi món còn ấm, không đun quá lâu.'
                    ELSE 'Giúp áp chảo/xào nhẹ hoặc trộn món mượt hơn; dùng lượng nhỏ để món không bị nặng bụng và không che mất vị chính.'
                END
            ),
            (
                CASE
                    WHEN fp.protein_name ILIKE '%cá%' OR fp.protein_name ILIKE '%tôm%' THEN 'Gừng'
                    WHEN fp.protein_name ILIKE '%gà%' OR fp.protein_name ILIKE '%bò%' OR fp.protein_name ILIKE '%heo%' OR fp.protein_name ILIKE '%thịt%' THEN 'Hành tím'
                    WHEN fp.protein_name ILIKE '%trứng%' OR fp.protein_name ILIKE '%đậu hũ%' THEN 'Hành lá'
                    ELSE 'Rau mùi'
                END,
                CASE WHEN fp.function_code = 2 THEN 1::double precision ELSE 3::double precision END,
                'g',
                CASE
                    WHEN fp.protein_name ILIKE '%cá%' OR fp.protein_name ILIKE '%tôm%' THEN 'Giúp giảm mùi tanh nhẹ của hải sản; với bé chỉ dùng rất ít và vớt bỏ lát gừng trước khi xay hoặc nghiền.'
                    WHEN fp.protein_name ILIKE '%gà%' OR fp.protein_name ILIKE '%bò%' OR fp.protein_name ILIKE '%heo%' OR fp.protein_name ILIKE '%thịt%' THEN 'Tạo mùi thơm nền cho thịt; băm nhỏ hoặc phi rất nhẹ, không để cháy vì dễ làm món đắng.'
                    WHEN fp.protein_name ILIKE '%trứng%' OR fp.protein_name ILIKE '%đậu hũ%' THEN 'Làm món thơm và bớt đơn điệu; cho vào cuối để giữ màu và không làm rau bị nồng quá.'
                    ELSE 'Tạo mùi thơm tươi ở cuối món; dùng ít để không lấn át vị của nguyên liệu chính.'
                END
            )
    ) AS x(name, amount, unit, description)
)
INSERT INTO "food_library_ingredients" ("food_lib_id", "food_ing_id", "amount_per_serving", "unit", "status", "description")
SELECT eff."food_lib_id", fi."id", eff.amount, eff.unit, 2, eff.description
FROM extra_for_food eff
JOIN "food_ingredients" fi ON fi."name_ingredients" = eff.name
WHERE NOT EXISTS (
    SELECT 1
    FROM "food_library_ingredients" fli
    WHERE fli."food_lib_id" = eff."food_lib_id"
      AND fli."food_ing_id" = fi."id"
);

-- changeset codex:028
WITH ingredient_rank AS (
    SELECT
        fli."id",
        fl."function_code",
        fl."name" AS food_name,
        fi."name_ingredients",
        fli."amount_per_serving",
        COALESCE(NULLIF(fli."unit", ''), 'g') AS "unit",
        fli."description",
        ROW_NUMBER() OVER (PARTITION BY fli."food_lib_id" ORDER BY fli."id") AS ingredient_order
    FROM "food_library_ingredients" fli
    JOIN "food_library" fl ON fl."id" = fli."food_lib_id"
    JOIN "food_ingredients" fi ON fi."id" = fli."food_ing_id"
    WHERE fli."status" <> -4 OR fli."status" IS NULL
)
UPDATE "food_library_ingredients" fli
SET "description" = CASE
    WHEN ir."name_ingredients" IN ('Nước dùng nhạt', 'Nước lọc', 'Nước cốt chanh') THEN
        ir."description"
    WHEN ir."name_ingredients" IN ('Dầu ô liu', 'Dầu mè', 'Dầu ăn dặm') THEN
        ir."description"
    WHEN ir."name_ingredients" IN ('Gừng', 'Hành tím', 'Tỏi', 'Hành lá', 'Rau mùi') THEN
        ir."description"
    WHEN ir.ingredient_order = 1 THEN
        ir."name_ingredients" || ' là phần đạm chính tạo vị và độ chắc cho ' || ir.food_name || '. Lượng dùng khoảng ' ||
        TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM ir."amount_per_serving"::TEXT)) || ' ' || ir."unit" ||
        CASE WHEN ir."function_code" = 2
            THEN '; cần làm chín kỹ, tách bỏ xương/vỏ/gân/da dai nếu có, sau đó xé nhỏ, băm hoặc nghiền theo độ tuổi để bé dễ nuốt.'
            ELSE '; nên sơ chế sạch, nấu vừa chín để giữ độ mềm, hạn chế chiên ngập dầu để món không quá nặng.'
        END
    WHEN ir.ingredient_order = 2 THEN
        ir."name_ingredients" || ' là phần nền giúp ' || ir.food_name || ' có độ no, độ sánh hoặc độ mềm. Lượng dùng khoảng ' ||
        TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM ir."amount_per_serving"::TEXT)) || ' ' || ir."unit" ||
        CASE WHEN ir."function_code" = 2
            THEN '; cần nấu thật mềm rồi nghiền/xay/cắt nhỏ để không bị vón cục hoặc làm bé khó nuốt.'
            ELSE '; có thể tăng giảm theo mục tiêu ăn uống, đặc biệt khi mẹ muốn ăn nhẹ hơn hoặc cần bữa chắc bụng hơn.'
        END
    WHEN ir.ingredient_order = 3 THEN
        ir."name_ingredients" || ' là phần rau, củ, nấm hoặc trái cây giúp món có màu, mùi và vị tự nhiên hơn. Lượng dùng khoảng ' ||
        TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM ir."amount_per_serving"::TEXT)) || ' ' || ir."unit" ||
        CASE WHEN ir."function_code" = 2
            THEN '; cần rửa kỹ, bỏ vỏ/hạt/xơ cứng nếu có, nấu mềm và tránh để miếng tròn, dai hoặc cứng.'
            ELSE '; nên cho vào đúng thời điểm để giữ độ mềm vừa phải, tránh nấu quá lâu làm mất màu và làm món bị nhũn.'
        END
    ELSE
        COALESCE(ir."description", ir."name_ingredients" || ' là thành phần phụ giúp hoàn thiện mùi vị, độ mềm hoặc độ ẩm của món.')
END
FROM ingredient_rank ir
WHERE fli."id" = ir."id";

-- changeset codex:029
WITH extra_ingredient_seed(name, image_url, base_amount, base_unit, calories, protein, carbs, fat, fiber, sugar, sodium) AS (
    VALUES
        ('Nước luộc rau củ', '/Ingredient/ingredient_nuoc-luoc-rau-cu.jpg', 100, 'ml', 10, 0.3, 2.0, 0.1, 0.2, 0.8, 18),
        ('Sữa tươi không đường', '/Ingredient/ingredient_sua-tuoi-khong-duong.jpg', 100, 'ml', 61, 3.2, 4.8, 3.3, 0, 5.1, 43),
        ('Bột năng', '/Ingredient/ingredient_bot-nang.jpg', 100, 'g', 350, 0.2, 87, 0.1, 0.2, 0, 8),
        ('Mè rang', '/Ingredient/ingredient_me-rang.jpg', 100, 'g', 573, 17, 23, 50, 12, 0.3, 11),
        ('Tiêu xay', '/Ingredient/ingredient_tieu-xay.jpg', 100, 'g', 251, 10.4, 64, 3.3, 25, 0.6, 20),
        ('Muối i-ốt', '/Ingredient/ingredient_muoi-iot.jpg', 100, 'g', 0, 0, 0, 0, 0, 0, 38758)
)
INSERT INTO "food_ingredients" ("name_ingredients", "image_url", "created_at", "created_by", "updated_by", "updated_at", "status")
SELECT s.name, s.image_url, now(), 'system', 'system', now(), 2
FROM extra_ingredient_seed s
WHERE NOT EXISTS (
    SELECT 1 FROM "food_ingredients" fi WHERE fi."name_ingredients" = s.name
);

-- changeset codex:030
WITH extra_ingredient_seed(name, base_amount, base_unit, calories, protein, carbs, fat, fiber, sugar, sodium) AS (
    VALUES
        ('Nước luộc rau củ', 100, 'ml', 10, 0.3, 2.0, 0.1, 0.2, 0.8, 18),
        ('Sữa tươi không đường', 100, 'ml', 61, 3.2, 4.8, 3.3, 0, 5.1, 43),
        ('Bột năng', 100, 'g', 350, 0.2, 87, 0.1, 0.2, 0, 8),
        ('Mè rang', 100, 'g', 573, 17, 23, 50, 12, 0.3, 11),
        ('Tiêu xay', 100, 'g', 251, 10.4, 64, 3.3, 25, 0.6, 20),
        ('Muối i-ốt', 100, 'g', 0, 0, 0, 0, 0, 0, 38758)
)
INSERT INTO "ingredient_nutrition" (
    "food_ingredient_id", "base_amount", "base_unit",
    "calories", "calories_unit",
    "protein", "protein_unit",
    "carbs", "carbs_unit",
    "fat", "fat_unit",
    "fiber", "fiber_unit",
    "sugar", "sugar_unit",
    "sodium", "sodium_unit",
    "status"
)
SELECT
    fi."id", s.base_amount, s.base_unit,
    s.calories, 'kcal',
    s.protein, 'g',
    s.carbs, 'g',
    s.fat, 'g',
    s.fiber, 'g',
    s.sugar, 'g',
    s.sodium, 'mg',
    2
FROM extra_ingredient_seed s
JOIN "food_ingredients" fi ON fi."name_ingredients" = s.name
WHERE NOT EXISTS (
    SELECT 1 FROM "ingredient_nutrition" inu WHERE inu."food_ingredient_id" = fi."id"
);

-- changeset codex:031
WITH main_ingredient_rank AS (
    SELECT
        fli."food_lib_id",
        fl."function_code",
        fl."name" AS food_name,
        fi."name_ingredients",
        ROW_NUMBER() OVER (PARTITION BY fli."food_lib_id" ORDER BY fli."id") AS ingredient_order
    FROM "food_library_ingredients" fli
    JOIN "food_library" fl ON fl."id" = fli."food_lib_id"
    JOIN "food_ingredients" fi ON fi."id" = fli."food_ing_id"
    WHERE (fli."status" <> -4 OR fli."status" IS NULL)
      AND fi."name_ingredients" NOT IN (
          'Nước dùng nhạt', 'Nước lọc', 'Gừng', 'Hành tím', 'Tỏi', 'Hành lá',
          'Rau mùi', 'Dầu ô liu', 'Dầu mè', 'Dầu ăn dặm', 'Nước cốt chanh',
          'Nước luộc rau củ', 'Sữa tươi không đường', 'Bột năng', 'Mè rang',
          'Tiêu xay', 'Muối i-ốt'
      )
),
food_profile AS (
    SELECT
        "food_lib_id",
        MAX("function_code") AS function_code,
        MAX(food_name) AS food_name,
        MAX(CASE WHEN ingredient_order = 1 THEN "name_ingredients" END) AS protein_name,
        MAX(CASE WHEN ingredient_order = 2 THEN "name_ingredients" END) AS carb_name,
        MAX(CASE WHEN ingredient_order = 3 THEN "name_ingredients" END) AS veg_name
    FROM main_ingredient_rank
    WHERE ingredient_order <= 3
    GROUP BY "food_lib_id"
),
extra_for_food AS (
    SELECT fp."food_lib_id", x.name, x.amount, x.unit, x.description
    FROM food_profile fp
    CROSS JOIN LATERAL (
        VALUES
            (
                CASE
                    WHEN fp.function_code = 2 THEN 'Nước luộc rau củ'
                    WHEN fp.food_name ILIKE '%súp%' OR fp.food_name ILIKE '%cháo%' OR fp.food_name ILIKE '%canh%' OR fp.food_name ILIKE '%hầm%' THEN 'Sữa tươi không đường'
                    WHEN fp.food_name ILIKE '%salad%' OR fp.food_name ILIKE '%bowl%' OR fp.food_name ILIKE '%trộn%' THEN 'Mè rang'
                    ELSE 'Muối i-ốt'
                END,
                CASE
                    WHEN fp.function_code = 2 THEN 40::double precision
                    WHEN fp.food_name ILIKE '%súp%' OR fp.food_name ILIKE '%cháo%' OR fp.food_name ILIKE '%canh%' OR fp.food_name ILIKE '%hầm%' THEN 30::double precision
                    WHEN fp.food_name ILIKE '%salad%' OR fp.food_name ILIKE '%bowl%' OR fp.food_name ILIKE '%trộn%' THEN 3::double precision
                    ELSE 0.5::double precision
                END,
                CASE
                    WHEN fp.function_code = 2 THEN 'ml'
                    WHEN fp.food_name ILIKE '%súp%' OR fp.food_name ILIKE '%cháo%' OR fp.food_name ILIKE '%canh%' OR fp.food_name ILIKE '%hầm%' THEN 'ml'
                    WHEN fp.food_name ILIKE '%salad%' OR fp.food_name ILIKE '%bowl%' OR fp.food_name ILIKE '%trộn%' THEN 'g'
                    ELSE 'g'
                END,
                CASE
                    WHEN fp.function_code = 2 THEN 'Dùng để làm món mềm và có vị ngọt rau củ tự nhiên hơn nước lọc; thêm từng ít một sau khi nghiền để chỉnh độ đặc, không nêm mặn.'
                    WHEN fp.food_name ILIKE '%súp%' OR fp.food_name ILIKE '%cháo%' OR fp.food_name ILIKE '%canh%' OR fp.food_name ILIKE '%hầm%' THEN 'Tạo độ béo nhẹ và làm vị món tròn hơn; chỉ cho vào cuối, đun lửa nhỏ và không để sôi mạnh để tránh tách vị.'
                    WHEN fp.food_name ILIKE '%salad%' OR fp.food_name ILIKE '%bowl%' OR fp.food_name ILIKE '%trộn%' THEN 'Rắc sau cùng để tạo mùi thơm và độ bùi; dùng lượng nhỏ để không làm món quá béo, với bé nhỏ cần giã mịn hoặc bỏ qua nếu chưa phù hợp.'
                    ELSE 'Chỉ dùng rất ít để làm rõ vị nguyên liệu; không dùng cho phần ăn của bé dưới 12 tháng và không nên tăng lượng nếu mẹ cần ăn nhạt.'
                END
            ),
            (
                CASE
                    WHEN fp.function_code = 2 THEN 'Bột năng'
                    WHEN fp.food_name ILIKE '%súp%' OR fp.food_name ILIKE '%sốt%' THEN 'Bột năng'
                    ELSE 'Tiêu xay'
                END,
                CASE
                    WHEN fp.function_code = 2 THEN 1.5::double precision
                    WHEN fp.food_name ILIKE '%súp%' OR fp.food_name ILIKE '%sốt%' THEN 3::double precision
                    ELSE 0.2::double precision
                END,
                'g',
                CASE
                    WHEN fp.function_code = 2 THEN 'Tạo độ sánh rất nhẹ để món bám thìa hơn; hòa tan với nước nguội trước rồi khuấy vào nồi, chỉ dùng khi món cần đặc hơn.'
                    WHEN fp.food_name ILIKE '%súp%' OR fp.food_name ILIKE '%sốt%' THEN 'Giúp phần nước/sốt có độ sánh mượt; hòa với nước lạnh trước khi cho vào để tránh vón cục.'
                    ELSE 'Tạo mùi ấm nhẹ cho món của mẹ; cho sau cùng với lượng rất nhỏ, không dùng cho phần ăn của bé.'
                END
            )
    ) AS x(name, amount, unit, description)
)
INSERT INTO "food_library_ingredients" ("food_lib_id", "food_ing_id", "amount_per_serving", "unit", "status", "description")
SELECT eff."food_lib_id", fi."id", eff.amount, eff.unit, 2, eff.description
FROM extra_for_food eff
JOIN "food_ingredients" fi ON fi."name_ingredients" = eff.name
WHERE NOT EXISTS (
    SELECT 1
    FROM "food_library_ingredients" fli
    WHERE fli."food_lib_id" = eff."food_lib_id"
      AND fli."food_ing_id" = fi."id"
);

-- changeset codex:032
WITH ingredient_rank AS (
    SELECT
        fli."id",
        fl."function_code",
        fl."name" AS food_name,
        fi."name_ingredients",
        fli."amount_per_serving",
        COALESCE(NULLIF(fli."unit", ''), 'g') AS "unit",
        fli."description",
        ROW_NUMBER() OVER (PARTITION BY fli."food_lib_id" ORDER BY fli."id") AS ingredient_order
    FROM "food_library_ingredients" fli
    JOIN "food_library" fl ON fl."id" = fli."food_lib_id"
    JOIN "food_ingredients" fi ON fi."id" = fli."food_ing_id"
    WHERE fli."status" <> -4 OR fli."status" IS NULL
)
UPDATE "food_library_ingredients" fli
SET "description" = CASE
    WHEN ir."name_ingredients" IN (
        'Nước dùng nhạt', 'Nước lọc', 'Nước cốt chanh', 'Nước luộc rau củ',
        'Dầu ô liu', 'Dầu mè', 'Dầu ăn dặm',
        'Gừng', 'Hành tím', 'Tỏi', 'Hành lá', 'Rau mùi',
        'Sữa tươi không đường', 'Bột năng', 'Mè rang', 'Tiêu xay', 'Muối i-ốt'
    ) THEN
        ir."description" || ' Lượng gợi ý trong món này khoảng ' ||
        TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM ir."amount_per_serving"::TEXT)) || ' ' || ir."unit" ||
        '; đây là thành phần phụ nên không nên tăng tùy ý nếu món dành cho bé hoặc người cần ăn nhạt.'
    WHEN ir.ingredient_order = 1 THEN
        ir."name_ingredients" || ' là nguyên liệu tạo vị chính cho ' || ir.food_name || '. Lượng gợi ý khoảng ' ||
        TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM ir."amount_per_serving"::TEXT)) || ' ' || ir."unit" ||
        '. Khi chuẩn bị cần rửa sạch, kiểm tra phần không ăn được, cắt theo thớ mềm và làm chín bằng hấp, luộc, hầm hoặc áp chảo ít dầu. ' ||
        CASE WHEN ir."function_code" = 2
            THEN 'Với bé, phần này phải được làm mềm hoàn toàn, tách xương/vỏ/gân/da dai nếu có, sau đó băm, xé, nghiền hoặc xay đúng độ tuổi; không để miếng lớn vì bé dễ ọe hoặc hóc.'
            ELSE 'Với mẹ, phần này giúp bữa ăn có độ chắc và no hơn; tránh nấu quá lâu vì dễ khô, dai và làm món kém ngon.'
        END
    WHEN ir.ingredient_order = 2 THEN
        ir."name_ingredients" || ' là nền năng lượng của ' || ir.food_name || ', giúp món có độ no, độ sánh hoặc độ mềm. Lượng gợi ý khoảng ' ||
        TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM ir."amount_per_serving"::TEXT)) || ' ' || ir."unit" ||
        '. Thành phần này nên được nấu riêng đến khi thật mềm trước khi trộn với phần đạm và rau để tránh món còn lợn cợn. ' ||
        CASE WHEN ir."function_code" = 2
            THEN 'Với bé, cần kiểm tra độ mềm bằng cách miết giữa hai ngón tay; nếu còn cứng hoặc dai thì phải nấu thêm.'
            ELSE 'Với mẹ, có thể giảm lượng nếu đang kiểm soát cân nặng hoặc tăng nhẹ nếu cần bữa chính chắc bụng.'
        END
    WHEN ir.ingredient_order = 3 THEN
        ir."name_ingredients" || ' là phần rau, củ, nấm hoặc trái cây giúp ' || ir.food_name || ' có màu, mùi và vị tự nhiên hơn. Lượng gợi ý khoảng ' ||
        TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM ir."amount_per_serving"::TEXT)) || ' ' || ir."unit" ||
        '. Cần rửa kỹ, bỏ vỏ/hạt/xơ/cọng già nếu có, cắt nhỏ và cho vào đúng thời điểm để giữ độ mềm vừa phải. ' ||
        CASE WHEN ir."function_code" = 2
            THEN 'Với bé, phần này phải chín mềm, không còn miếng tròn hoặc dai; nếu là rau lá nên băm thật nhỏ để bé không bị mắc ở cổ.'
            ELSE 'Với mẹ, phần này giúp món đỡ ngán và cân bằng hơn; không nên nấu quá lâu làm rau nhũn và mất mùi tươi.'
        END
    ELSE
        COALESCE(ir."description", ir."name_ingredients" || ' là thành phần phụ giúp hoàn thiện mùi vị, độ mềm hoặc độ ẩm của món.')
END
FROM ingredient_rank ir
WHERE fli."id" = ir."id";
