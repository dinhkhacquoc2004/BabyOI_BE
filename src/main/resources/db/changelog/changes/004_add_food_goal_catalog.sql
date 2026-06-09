-- liquibase formatted sql

-- changeset codex:018
-- validCheckSum: ANY
CREATE TEMP TABLE seed_food_goal_recipe_018 (
    food_id BIGINT PRIMARY KEY,
    function_code BIGINT NOT NULL,
    name TEXT NOT NULL,
    advance_for TEXT NOT NULL,
    protein TEXT NOT NULL,
    carb TEXT NOT NULL,
    veg TEXT NOT NULL,
    amount_protein DOUBLE PRECISION NOT NULL,
    amount_carb DOUBLE PRECISION NOT NULL,
    amount_veg DOUBLE PRECISION NOT NULL,
    total_calories DOUBLE PRECISION NOT NULL,
    total_protein DOUBLE PRECISION NOT NULL,
    total_carbs DOUBLE PRECISION NOT NULL,
    total_fat DOUBLE PRECISION NOT NULL
) ON COMMIT DROP;

INSERT INTO seed_food_goal_recipe_018 (
    food_id, function_code, name, advance_for, protein, carb, veg,
    amount_protein, amount_carb, amount_veg,
    total_calories, total_protein, total_carbs, total_fat
) VALUES
    (201, 1, 'Cháo yến mạch cá hồi rau ngót', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Cá hồi', 'Yến mạch', 'Rau ngót', 120, 80, 90, 455, 31, 45, 17),
    (202, 1, 'Canh rau ngót thịt gà cơm gạo tẻ', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Thịt gà', 'Gạo tẻ', 'Rau ngót', 130, 100, 100, 430, 33, 49, 9),
    (203, 1, 'Cháo đậu xanh thịt heo nạc cà rốt', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Thịt heo nạc', 'Đậu xanh', 'Cà rốt', 120, 90, 80, 440, 31, 50, 11),
    (204, 1, 'Cơm gạo lứt cá thu cà chua thì là', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Cá thu', 'Gạo lứt', 'Cà chua', 110, 100, 90, 510, 32, 48, 20),
    (205, 1, 'Súp bí đỏ tôm phô mai tươi', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Tôm', 'Bí đỏ', 'Phô mai tươi', 120, 140, 35, 430, 30, 38, 16),
    (206, 1, 'Bún gạo lứt thịt bò rau dền', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Thịt bò nạc', 'Bún gạo lứt', 'Rau dền', 120, 110, 90, 465, 34, 52, 11),
    (207, 1, 'Cháo gà hạt kê nấm hương', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Thịt gà', 'Hạt kê', 'Nấm hương', 130, 90, 50, 445, 33, 48, 10),
    (208, 1, 'Khoai lang đậu gà mè rang xay', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Đậu gà', 'Khoai lang', 'Mè rang xay', 90, 150, 12, 460, 20, 62, 14),
    (209, 1, 'Cơm mềm đậu hũ sốt thịt heo cà chua', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Thịt heo xay', 'Gạo tẻ', 'Đậu hũ non', 110, 100, 100, 470, 32, 46, 17),
    (210, 1, 'Canh đậu đỏ thịt bò cà rốt', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Thịt bò nạc', 'Đậu đỏ', 'Cà rốt', 120, 90, 80, 485, 35, 52, 12),
    (211, 1, 'Nui nhỏ cá hồi bông cải xanh', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Cá hồi', 'Nui nhỏ', 'Bông cải xanh', 120, 100, 90, 495, 32, 50, 18),
    (212, 1, 'Cháo gan gà rau ngót gạo tẻ', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Gan gà', 'Gạo tẻ', 'Rau ngót', 80, 100, 90, 390, 26, 45, 10),
    (213, 1, 'Diêm mạch gà xé đậu Hà Lan', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Thịt gà', 'Diêm mạch', 'Đậu Hà Lan', 130, 100, 80, 475, 34, 50, 11),
    (214, 1, 'Canh khoai mỡ thịt gà rau ngót', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Thịt gà', 'Khoai mỡ', 'Rau ngót', 120, 130, 80, 455, 31, 54, 8),
    (215, 1, 'Cá trắng hấp gừng cơm gạo lứt', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Cá trắng', 'Gạo lứt', 'Gừng', 130, 100, 8, 410, 33, 43, 8),
    (216, 1, 'Đậu lăng đỏ hầm bí đỏ thịt heo nạc', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Thịt heo nạc', 'Đậu lăng đỏ', 'Bí đỏ', 110, 100, 100, 480, 34, 52, 12),
    (217, 1, 'Bánh mì mềm trứng gà bơ sữa chua', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Trứng gà', 'Bánh mì mềm', 'Bơ', 100, 80, 60, 510, 24, 46, 25),
    (218, 1, 'Súp tôm bắp non cà rốt', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Tôm', 'Bắp non', 'Cà rốt', 130, 120, 80, 390, 30, 42, 7),
    (219, 1, 'Cơm gạo lứt đậu hũ nấm hương cải bó xôi', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Đậu hũ', 'Gạo lứt', 'Cải bó xôi', 140, 100, 90, 445, 25, 50, 13),
    (220, 1, 'Cháo cá thu đậu xanh rau dền', 'FOR_MOTHER_INCREASE_MILK_SUPPLY', 'Cá thu', 'Đậu xanh', 'Rau dền', 100, 90, 90, 465, 30, 48, 17),
    (221, 1, 'Yến mạch chuối sữa chua hạt chia', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Sữa chua không đường', 'Yến mạch', 'Chuối', 120, 60, 80, 360, 16, 55, 9),
    (222, 1, 'Cháo hạt kê đậu hũ non bí đỏ', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Đậu hũ non', 'Hạt kê', 'Bí đỏ', 120, 90, 100, 390, 21, 53, 9),
    (223, 1, 'Cá hồi áp chảo khoai tây cải bó xôi', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Cá hồi', 'Khoai tây', 'Cải bó xôi', 110, 120, 90, 470, 30, 42, 19),
    (224, 1, 'Súp khoai lang đậu lăng đỏ cà rốt', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Đậu lăng đỏ', 'Khoai lang', 'Cà rốt', 90, 140, 80, 395, 21, 62, 5),
    (225, 1, 'Cơm mềm cá trắng hấp gừng su su', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Cá trắng', 'Gạo tẻ', 'Su su', 130, 100, 100, 385, 31, 45, 6),
    (226, 1, 'Bánh mì mềm trứng gà bơ dưa leo', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Trứng gà', 'Bánh mì mềm', 'Dưa leo', 100, 80, 70, 380, 22, 42, 14),
    (227, 1, 'Diêm mạch đậu gà bắp cải tím', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Đậu gà', 'Diêm mạch', 'Bắp cải tím', 100, 100, 90, 430, 22, 58, 10),
    (228, 1, 'Cháo gạo lứt mềm thịt gà nấm hương', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Thịt gà', 'Gạo lứt mềm', 'Nấm hương', 120, 100, 50, 420, 31, 48, 8),
    (229, 1, 'Sữa chua xoài yến mạch hạt lanh', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Sữa chua không đường', 'Yến mạch', 'Xoài', 120, 50, 80, 370, 15, 58, 9),
    (230, 1, 'Canh bí xanh tôm gừng', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Tôm', 'Gạo tẻ', 'Bí xanh', 120, 90, 120, 360, 29, 41, 5),
    (231, 1, 'Khoai tây nghiền cá hồi sữa chua', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Cá hồi', 'Khoai tây', 'Sữa chua không đường', 100, 130, 50, 455, 29, 40, 18),
    (232, 1, 'Cơm gạo lứt đậu hũ cải kale', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Đậu hũ', 'Gạo lứt', 'Cải kale', 130, 100, 90, 430, 25, 48, 13),
    (233, 1, 'Cháo yến mạch chuối quế', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Sữa chua không đường', 'Yến mạch', 'Chuối', 100, 80, 90, 365, 15, 60, 7),
    (234, 1, 'Súp bí đỏ thịt gà húng quế', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Thịt gà', 'Bí đỏ', 'Húng quế', 120, 150, 5, 410, 31, 39, 10),
    (235, 1, 'Miến dong thịt heo nạc cải thìa', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Thịt heo nạc', 'Miến dong', 'Cải thìa', 110, 90, 90, 390, 30, 48, 7),
    (236, 1, 'Cá thu kho cà chua ăn cùng su su', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Cá thu', 'Gạo tẻ', 'Su su', 100, 90, 100, 450, 30, 42, 17),
    (237, 1, 'Đậu lăng nấu cà rốt cải bó xôi', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Đậu lăng', 'Gạo tẻ', 'Cải bó xôi', 100, 90, 90, 400, 23, 55, 6),
    (238, 1, 'Khoai lang sữa chua hạnh nhân xay', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Sữa chua không đường', 'Khoai lang', 'Hạnh nhân xay', 120, 140, 10, 420, 16, 58, 13),
    (239, 1, 'Cơm mềm tôm măng tây', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Tôm', 'Gạo tẻ', 'Măng tây', 120, 100, 90, 395, 30, 45, 6),
    (240, 1, 'Cháo đậu đỏ yến mạch vani tự nhiên', 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', 'Đậu đỏ', 'Yến mạch', 'Vani tự nhiên', 90, 80, 2, 390, 20, 58, 7),
    (241, 2, 'Cháo mịn cá hồi bí đỏ', 'FOR_BABY_6_8_MONTHS_DEVELOPMENT', 'Cá hồi', 'Gạo tẻ', 'Bí đỏ', 25, 35, 30, 110, 7, 16, 4),
    (242, 2, 'Bột gạo thịt gà rau ngót', 'FOR_BABY_6_8_MONTHS_DEVELOPMENT', 'Thịt gà', 'Bột gạo', 'Rau ngót', 25, 35, 25, 100, 7, 15, 2),
    (243, 2, 'Cháo yến mạch lòng đỏ trứng cà rốt', 'FOR_BABY_6_8_MONTHS_DEVELOPMENT', 'Lòng đỏ trứng', 'Yến mạch', 'Cà rốt', 20, 35, 30, 115, 6, 15, 5),
    (244, 2, 'Khoai lang nghiền đậu hũ non', 'FOR_BABY_6_8_MONTHS_DEVELOPMENT', 'Đậu hũ non', 'Khoai lang', 'Bí xanh', 35, 45, 25, 118, 5, 19, 3),
    (245, 2, 'Cháo thịt bò bí đỏ', 'FOR_BABY_6_8_MONTHS_DEVELOPMENT', 'Thịt bò xay', 'Gạo tẻ', 'Bí đỏ', 25, 35, 30, 112, 8, 16, 3),
    (246, 2, 'Cháo tôm bí xanh', 'FOR_BABY_6_8_MONTHS_DEVELOPMENT', 'Tôm', 'Gạo tẻ', 'Bí xanh', 25, 35, 30, 105, 7, 15, 2),
    (247, 2, 'Bột đậu xanh gà cà rốt', 'FOR_BABY_6_8_MONTHS_DEVELOPMENT', 'Thịt gà', 'Bột đậu xanh', 'Cà rốt', 25, 35, 30, 116, 8, 17, 2),
    (248, 2, 'Cháo cá trắng cải bó xôi', 'FOR_BABY_6_8_MONTHS_DEVELOPMENT', 'Cá trắng', 'Gạo tẻ', 'Cải bó xôi', 25, 35, 25, 104, 7, 15, 2),
    (249, 2, 'Cháo gan gà cà rốt', 'FOR_BABY_6_8_MONTHS_DEVELOPMENT', 'Gan gà', 'Gạo tẻ', 'Cà rốt', 18, 35, 30, 108, 7, 15, 3),
    (250, 2, 'Nghiền bơ sữa chua yến mạch', 'FOR_BABY_6_8_MONTHS_DEVELOPMENT', 'Sữa chua không đường', 'Yến mạch', 'Bơ', 35, 25, 30, 125, 4, 13, 7),
    (251, 2, 'Cháo đặc cá hồi khoai lang', 'FOR_BABY_9_11_MONTHS_DEVELOPMENT', 'Cá hồi', 'Khoai lang', 'Bông cải xanh', 35, 50, 35, 155, 9, 21, 6),
    (252, 2, 'Nui nhỏ thịt bò cà chua', 'FOR_BABY_9_11_MONTHS_DEVELOPMENT', 'Thịt bò', 'Nui nhỏ', 'Cà chua', 35, 50, 35, 165, 10, 24, 4),
    (253, 2, 'Cơm nát đậu hũ non bí đỏ', 'FOR_BABY_9_11_MONTHS_DEVELOPMENT', 'Đậu hũ non', 'Gạo tẻ', 'Bí đỏ', 45, 55, 40, 150, 7, 24, 4),
    (254, 2, 'Cháo tôm đậu Hà Lan', 'FOR_BABY_9_11_MONTHS_DEVELOPMENT', 'Tôm', 'Gạo tẻ', 'Đậu Hà Lan', 35, 45, 35, 150, 9, 22, 2),
    (255, 2, 'Bánh mì mềm trứng bơ', 'FOR_BABY_9_11_MONTHS_DEVELOPMENT', 'Trứng gà', 'Bánh mì mềm', 'Bơ', 35, 35, 25, 170, 7, 20, 8),
    (256, 2, 'Cháo gà nấm rơm bí xanh', 'FOR_BABY_9_11_MONTHS_DEVELOPMENT', 'Thịt gà', 'Gạo tẻ', 'Nấm rơm', 35, 50, 25, 148, 9, 22, 3),
    (257, 2, 'Khoai tây nghiền cá trắng đậu Hà Lan', 'FOR_BABY_9_11_MONTHS_DEVELOPMENT', 'Cá trắng', 'Khoai tây', 'Đậu Hà Lan', 35, 55, 35, 155, 9, 23, 3),
    (258, 2, 'Cháo thịt heo nạc su su', 'FOR_BABY_9_11_MONTHS_DEVELOPMENT', 'Thịt heo nạc', 'Gạo tẻ', 'Su su', 35, 50, 35, 150, 9, 22, 3),
    (259, 2, 'Sữa chua đu đủ yến mạch', 'FOR_BABY_9_11_MONTHS_DEVELOPMENT', 'Sữa chua không đường', 'Yến mạch', 'Đu đủ', 45, 25, 40, 145, 6, 23, 3),
    (260, 2, 'Cháo cá thu cà chua', 'FOR_BABY_9_11_MONTHS_DEVELOPMENT', 'Cá thu', 'Gạo tẻ', 'Cà chua', 30, 50, 35, 160, 9, 21, 6),
    (261, 2, 'Cơm nát cá hồi bông cải', 'FOR_BABY_12_18_MONTHS_DEVELOPMENT', 'Cá hồi', 'Gạo tẻ', 'Bông cải xanh', 45, 70, 45, 225, 13, 30, 8),
    (262, 2, 'Cơm nát thịt bò bí đỏ', 'FOR_BABY_12_18_MONTHS_DEVELOPMENT', 'Thịt bò', 'Gạo tẻ', 'Bí đỏ', 45, 70, 45, 220, 14, 32, 5),
    (263, 2, 'Nui nhỏ gà cà rốt', 'FOR_BABY_12_18_MONTHS_DEVELOPMENT', 'Thịt gà', 'Nui nhỏ', 'Cà rốt', 45, 70, 45, 220, 13, 34, 4),
    (264, 2, 'Trứng hấp đậu hũ non rau ngót', 'FOR_BABY_12_18_MONTHS_DEVELOPMENT', 'Trứng gà', 'Đậu hũ non', 'Rau ngót', 45, 60, 35, 190, 11, 10, 11),
    (265, 2, 'Cơm nát tôm bắp non', 'FOR_BABY_12_18_MONTHS_DEVELOPMENT', 'Tôm', 'Gạo tẻ', 'Bắp non', 45, 70, 40, 210, 13, 32, 3),
    (266, 2, 'Khoai mỡ nghiền gà rau dền', 'FOR_BABY_12_18_MONTHS_DEVELOPMENT', 'Thịt gà', 'Khoai mỡ', 'Rau dền', 45, 80, 40, 225, 13, 34, 4),
    (267, 2, 'Cháo đậu đỏ thịt bò cà rốt', 'FOR_BABY_12_18_MONTHS_DEVELOPMENT', 'Thịt bò', 'Đậu đỏ', 'Cà rốt', 45, 70, 40, 230, 14, 34, 5),
    (268, 2, 'Miến dong cá trắng bí xanh', 'FOR_BABY_12_18_MONTHS_DEVELOPMENT', 'Cá trắng', 'Miến dong', 'Bí xanh', 45, 60, 45, 210, 13, 33, 3),
    (269, 2, 'Sữa chua xoài yến mạch', 'FOR_BABY_12_18_MONTHS_DEVELOPMENT', 'Sữa chua không đường', 'Yến mạch', 'Xoài', 60, 35, 50, 210, 7, 34, 5),
    (270, 2, 'Cơm nát thịt heo nạc su su', 'FOR_BABY_12_18_MONTHS_DEVELOPMENT', 'Thịt heo nạc', 'Gạo tẻ', 'Su su', 45, 70, 45, 220, 13, 32, 5),
    (271, 2, 'Cơm mềm gà nấm hương cải thìa', 'FOR_BABY_19_24_MONTHS_DEVELOPMENT', 'Thịt gà', 'Gạo tẻ', 'Nấm hương', 55, 85, 45, 270, 16, 40, 5),
    (272, 2, 'Cơm mềm cá hồi bí đỏ', 'FOR_BABY_19_24_MONTHS_DEVELOPMENT', 'Cá hồi', 'Gạo tẻ', 'Bí đỏ', 55, 85, 50, 285, 16, 38, 9),
    (273, 2, 'Mì nguyên cám mềm thịt heo cải bó xôi', 'FOR_BABY_19_24_MONTHS_DEVELOPMENT', 'Thịt heo nạc', 'Mì nguyên cám', 'Cải bó xôi', 55, 80, 45, 290, 16, 43, 6),
    (274, 2, 'Cơm mềm bò bông cải cà rốt', 'FOR_BABY_19_24_MONTHS_DEVELOPMENT', 'Thịt bò', 'Gạo tẻ', 'Bông cải xanh', 55, 85, 50, 285, 17, 40, 6),
    (275, 2, 'Nui nhỏ tôm sốt cà chua', 'FOR_BABY_19_24_MONTHS_DEVELOPMENT', 'Tôm', 'Nui nhỏ', 'Cà chua', 55, 80, 50, 265, 16, 41, 4),
    (276, 2, 'Đậu hũ non nấu tôm bông cải', 'FOR_BABY_19_24_MONTHS_DEVELOPMENT', 'Tôm', 'Đậu hũ non', 'Bông cải xanh', 45, 80, 50, 235, 15, 13, 10),
    (277, 2, 'Khoai lang nghiền trứng gà cà rốt', 'FOR_BABY_19_24_MONTHS_DEVELOPMENT', 'Trứng gà', 'Khoai lang', 'Cà rốt', 50, 85, 45, 275, 12, 39, 9),
    (278, 2, 'Bún gạo lứt mềm cá hồi cải thìa', 'FOR_BABY_19_24_MONTHS_DEVELOPMENT', 'Cá hồi', 'Bún gạo lứt', 'Cải thìa', 55, 80, 50, 295, 16, 41, 9),
    (279, 2, 'Sữa chua chuối hạt chia nghiền', 'FOR_BABY_19_24_MONTHS_DEVELOPMENT', 'Sữa chua không đường', 'Hạt chia', 'Chuối', 70, 10, 70, 240, 8, 30, 10),
    (280, 2, 'Cơm mềm đậu lăng thịt bò cà chua', 'FOR_BABY_19_24_MONTHS_DEVELOPMENT', 'Thịt bò', 'Đậu lăng', 'Cà chua', 55, 70, 50, 290, 18, 38, 6);

INSERT INTO "food_library" (
    "id", "function_code", "name", "advance_for",
    "created_at", "updated_at", "created_by", "updated_by", "status"
)
SELECT
    r.food_id, r.function_code, r.name, r.advance_for,
    now(), now(), 'system', 'system', 2
FROM seed_food_goal_recipe_018 r
WHERE NOT EXISTS (
    SELECT 1 FROM "food_library" fl WHERE fl."id" = r.food_id
);

SELECT setval(
    pg_get_serial_sequence('"food_library"', 'id'),
    GREATEST((SELECT COALESCE(MAX("id"), 1) FROM "food_library"), 1)
);

INSERT INTO "food_library_ingredients" (
    "food_lib_id", "food_ing_id", "amount_per_serving", "unit", "status", "description"
)
SELECT
    r.food_id,
    fi."id",
    ing.amount_grams,
    'g',
    2,
    ing.note
FROM seed_food_goal_recipe_018 r
CROSS JOIN LATERAL (
    VALUES
        (r.protein, r.amount_protein, 'Nguồn đạm chính của món.'),
        (r.carb, r.amount_carb, 'Nguồn tinh bột hoặc nền năng lượng của món.'),
        (r.veg, r.amount_veg, 'Rau, trái cây hoặc thành phần bổ sung vi chất/chất xơ.')
) AS ing(name, amount_grams, note)
JOIN "food_ingredients" fi ON fi."name_ingredients" = ing.name
WHERE NOT EXISTS (
    SELECT 1
    FROM "food_library_ingredients" fli
    WHERE fli."food_lib_id" = r.food_id
      AND fli."food_ing_id" = fi."id"
);

INSERT INTO "food_nutrition_summary" (
    "food_id",
    "total_calories", "total_calories_unit",
    "total_protein", "total_protein_unit",
    "total_carbs", "total_carbs_unit",
    "total_fat", "total_fat_unit",
    "total_fiber", "total_fiber_unit",
    "total_sugar", "total_sugar_unit",
    "total_sodium", "total_sodium_unit"
)
SELECT
    r.food_id,
    r.total_calories, 'kcal',
    r.total_protein, 'g',
    r.total_carbs, 'g',
    r.total_fat, 'g',
    ROUND(COALESCE(SUM((fli."amount_per_serving" / NULLIF(inn."base_amount", 0)) * inn."fiber"), 0)::numeric, 2)::double precision, 'g',
    ROUND(COALESCE(SUM((fli."amount_per_serving" / NULLIF(inn."base_amount", 0)) * inn."sugar"), 0)::numeric, 2)::double precision, 'g',
    ROUND(COALESCE(SUM((fli."amount_per_serving" / NULLIF(inn."base_amount", 0)) * inn."sodium"), 0)::numeric, 2)::double precision, 'mg'
FROM seed_food_goal_recipe_018 r
LEFT JOIN "food_library_ingredients" fli ON fli."food_lib_id" = r.food_id
LEFT JOIN "ingredient_nutrition" inn ON inn."food_ingredient_id" = fli."food_ing_id"
WHERE NOT EXISTS (
    SELECT 1 FROM "food_nutrition_summary" fns WHERE fns."food_id" = r.food_id
)
GROUP BY
    r.food_id, r.total_calories, r.total_protein, r.total_carbs, r.total_fat;

WITH seed_food_recommendation (food_id, good_points, bad_points, advice, cooking_way) AS (
    SELECT
        r.food_id,
        CASE
            WHEN r.function_code = 2 THEN
                r.name || ' phù hợp với nhóm ' || r.advance_for || ' vì có thể điều chỉnh độ mềm và lượng ăn theo kỹ năng nhai nuốt của bé. '
                || r.protein || ' tạo vị chính cho món, ' || r.carb || ' làm nền mềm để bé dễ no, và ' || r.veg
                || ' bổ sung màu sắc cùng chất xơ. Khi nấu mềm và chia lượng nhỏ, món dễ làm quen và thuận tiện theo dõi phản ứng của bé.'
            ELSE
                r.name || ' phù hợp với nhóm ' || r.advance_for || ' vì món có đủ phần chính, phần nền và phần rau/củ/trái cây. '
                || r.protein || ' tạo vị chính và giúp món có điểm nhấn, ' || r.carb || ' tạo độ no hoặc độ sánh, còn ' || r.veg
                || ' bổ sung màu sắc và chất xơ. Dùng khẩu phần vừa, nêm nhạt và ưu tiên cách nấu ít dầu.'
        END,
        CASE
            WHEN r.function_code = 2 THEN
                'Cần chế biến kỹ vì nhóm tuổi này còn nhạy với kết cấu và nguy cơ hóc. ' || r.protein
                || ' phải chín kỹ, bỏ xương, vỏ, gân hoặc phần dai nếu có; ' || r.carb
                || ' cần nấu mềm hoàn toàn; ' || r.veg
                || ' nên làm mềm và cắt/nghiền phù hợp. Không nêm mặn, không thêm mật ong cho bé dưới 12 tháng và theo dõi dị ứng khi có nguyên liệu mới.'
            ELSE
                'Cần tránh nêm mặn, thêm nhiều dầu hoặc dùng sốt béo vì dễ làm món nặng. ' || r.protein
                || ' nếu nấu quá lâu có thể khô hoặc dai; ' || r.carb
                || ' nếu dùng nhiều dễ làm khẩu phần mất cân đối; ' || r.veg
                || ' cần rửa sạch và nấu đúng độ mềm. Nếu mẹ hoặc bé từng dị ứng với nguyên liệu nào trong món thì không nên dùng món đó.'
        END,
        CASE
            WHEN r.advance_for = 'FOR_MOTHER_INCREASE_MILK_SUPPLY' THEN
                'Gợi ý dùng: Dùng như bữa chính hoặc bữa phụ lớn, ăn khi còn ấm, uống nước theo nhu cầu và duy trì bú/hút sữa đều; không kỳ vọng một món riêng lẻ làm tăng sữa ngay.'
            WHEN r.advance_for = 'FOR_MOTHER_SLEEP_STRESS_SUPPORT' THEN
                'Gợi ý dùng: Dùng vào bữa tối sớm hoặc bữa phụ chiều, giữ khẩu phần vừa, tránh cà phê, trà đặc và đồ quá ngọt cùng buổi để không làm khó ngủ hơn.'
            WHEN r.advance_for LIKE 'FOR_BABY_6_8%' THEN
                'Gợi ý dùng: Cho bé thử vào ban ngày, bắt đầu vài thìa nhỏ, xay mịn và chỉ tăng lượng khi bé dung nạp tốt.'
            WHEN r.advance_for LIKE 'FOR_BABY_9_11%' THEN
                'Gợi ý dùng: Tăng độ đặc từ từ, nghiền thô khi bé đã sẵn sàng; nếu bé ọe nhiều thì giảm độ thô và không ép ăn hết.'
            WHEN r.advance_for LIKE 'FOR_BABY_12_18%' THEN
                'Gợi ý dùng: Làm cơm nát hoặc món mềm cắt nhỏ, cho bé tập tự xúc một phần nhưng vẫn cần người lớn ngồi cạnh.'
            ELSE
                'Gợi ý dùng: Cho bé ăn cùng giờ bữa gia đình nhưng tách riêng phần ít gia vị, cắt nhỏ thức ăn tròn/cứng và theo dõi khi có nguyên liệu mới.'
        END,
        CASE
            WHEN r.function_code = 2 THEN
                'Bước 1: Rửa sạch từng nguyên liệu và để riêng để kiểm soát độ chín. Bước 2: Nấu ' || r.carb
                || ' trước đến khi mềm hẳn; nếu là cháo hoặc bột thì khuấy đều để không vón, nếu là nui, mì, khoai hoặc đậu thì nấu đến khi bóp nhẹ đã nát. Bước 3: Làm chín kỹ '
                || r.protein || ', bỏ xương, vỏ, gân hoặc phần dai nếu có. Bước 4: Làm mềm ' || r.veg
                || ', bỏ vỏ, hạt hoặc xơ cứng rồi băm/nghiền/cắt nhỏ theo tuổi. Bước 5: Phối hợp các phần với nước nấu hoặc nước dùng nhạt để đạt kết cấu phù hợp. Bước 6: Cho bé ăn khi món còn ấm và kiểm tra không còn cục cứng.'
            ELSE
                'Bước 1: Sơ chế ' || r.protein
                || ' thật sạch, bỏ xương, vỏ, mỡ thừa hoặc phần dai nếu có; có thể ướp rất nhạt với gừng hoặc hành. Bước 2: Chuẩn bị '
                || r.carb || ' riêng; nếu là gạo, đậu hoặc hạt thì nên ngâm và nấu mềm. Bước 3: Rửa '
                || r.veg || ', cắt vừa ăn và để riêng. Bước 4: Làm chín ' || r.protein
                || ' bằng hấp, luộc, hầm hoặc áp chảo ít dầu. Bước 5: Cho ' || r.carb || ' và ' || r.veg
                || ' v?o ph?i h?p, th?m n??c d?ng nh?t n?u m?n kh?. B??c 6: N?m nh?, n?u th?m v?i ph?t cho và h?a v?o nhau r?i d?ng khi c?n ?m.'
        END
    FROM seed_food_goal_recipe_018 r
)
INSERT INTO "food_recommendation" (
    "food_id", "good_points", "bad_points", "advice", "cooking_way", "status"
)
SELECT
    sfr.food_id,
    sfr.good_points,
    sfr.bad_points,
    sfr.advice,
    sfr.cooking_way,
    2
FROM seed_food_recommendation sfr
WHERE NOT EXISTS (
    SELECT 1 FROM "food_recommendation" fr WHERE fr."food_id" = sfr.food_id
);
