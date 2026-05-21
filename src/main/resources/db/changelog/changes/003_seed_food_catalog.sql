    -- liquibase formatted sql

-- changeset codex:015
-- validCheckSum: ANY
WITH ingredient_seed(id, name) AS (
    SELECT ordinality::BIGINT, name
    FROM unnest(ARRAY[
        'Ức gà','Cá hồi','Cá thu','Cá trắng','Thịt bò nạc','Thịt heo nạc','Tôm','Đậu hũ','Đậu hũ non','Trứng gà',
        'Lòng đỏ trứng','Đậu lăng','Đậu lăng đỏ','Đậu gà','Ức vịt bỏ da','Nấm đùi gà','Thịt gà','Thịt bò','Gan gà',
        'Sữa chua không đường','Phô mai tươi','Sữa mẹ','Sữa mẹ vắt','Sữa công thức giai đoạn 1','Gạo lứt','Gạo tẻ',
        'Gạo lứt mềm','Yến mạch','Khoai lang','Khoai tây','Diêm mạch','Bún gạo lứt','Mì nguyên cám','Nui nhỏ',
        'Bánh mì mềm','Hạt kê','Bông cải xanh','Cải bó xôi','Cà rốt','Bí đỏ','Đậu que','Măng tây','Cải thìa',
        'Bắp non','Rong biển','Cà chua','Đậu Hà Lan','Bí xanh','Su su','Chuối','Bơ','Táo hấp','Lê hấp','Đu đủ',
        'Rau ngót','Rau dền','Cải kale','Dưa leo','Xà lách romaine','Củ dền','Bắp cải tím','Nấm hương','Nấm rơm',
        'Hành tây','Tỏi','Gừng','Nghệ','Sả','Chanh','Cam','Quýt','Ổi','Dâu tây','Việt quất','Xoài','Thanh long',
        'Kiwi','Lựu','Hạt chia','Hạt lanh','Hạt óc chó xay','Hạnh nhân xay','Mè rang xay','Dầu ô liu','Dầu mè',
        'Dầu hạt cải','Nước dùng gà nhạt','Nước dùng rau củ','Nước hầm xương nhạt','Hành lá','Ngò rí','Thì là',
        'Húng quế','Bạc hà','Rau mùi tây','Tiêu đen','Quế','Vani tự nhiên','Bột cacao nguyên chất','Đậu đỏ','Đậu đen',
        'Đậu xanh','Đậu nành non','Bí ngòi','Ớt chuông','Củ cải trắng','Củ sen','Khoai mỡ','Khoai sọ','Miến dong',
        'Bột gạo','Bột ngô','Bột đậu xanh','Ức gà xay','Cá hồi xay','Thịt bò xay','Thịt heo xay','Đậu phụ nghiền',
        'Sữa tươi không đường','Sữa hạt không đường','Rau má','Atiso','Nước lọc'
    ]::text[]) WITH ORDINALITY AS seed(name, ordinality)
)
INSERT INTO "food_ingredients" ("id", "name_ingredients", "created_at", "created_by", "updated_by", "updated_at", "status")
SELECT s.id, s.name, now(), 'system', 'system', now(), 2
FROM ingredient_seed s
WHERE NOT EXISTS (
    SELECT 1
    FROM "food_ingredients" fi
    WHERE fi."id" = s.id OR fi."name_ingredients" = s.name
);

SELECT setval(
    pg_get_serial_sequence('"food_ingredients"', 'id'),
    GREATEST((SELECT COALESCE(MAX("id"), 1) FROM "food_ingredients"), 1)
);

WITH ingredient_nutrition_seed(name, base_amount, base_unit, calories, protein, carbs, fat, fiber, sugar, sodium) AS (
    VALUES
        ('Ức gà', 100, 'g', 165, 31, 0, 3.6, 0, 0, 74),
        ('Cá hồi', 100, 'g', 208, 20, 0, 13, 0, 0, 59),
        ('Cá thu', 100, 'g', 205, 19, 0, 14, 0, 0, 90),
        ('Cá trắng', 100, 'g', 96, 20, 0, 1.7, 0, 0, 70),
        ('Thịt bò nạc', 100, 'g', 170, 26, 0, 7, 0, 0, 65),
        ('Thịt heo nạc', 100, 'g', 143, 21, 0, 6, 0, 0, 62),
        ('Tôm', 100, 'g', 99, 24, 0.2, 0.3, 0, 0, 111),
        ('Đậu hũ', 100, 'g', 76, 8, 1.9, 4.8, 0.3, 0.6, 7),
        ('Đậu hũ non', 100, 'g', 55, 5, 2, 3, 0.3, 0.5, 5),
        ('Trứng gà', 100, 'g', 143, 13, 1.1, 9.5, 0, 0.4, 142),
        ('Lòng đỏ trứng', 100, 'g', 322, 16, 3.6, 27, 0, 0.6, 48),
        ('Đậu lăng', 100, 'g', 116, 9, 20, 0.4, 7.9, 1.8, 2),
        ('Đậu lăng đỏ', 100, 'g', 116, 9, 20, 0.4, 7.9, 1.8, 2),
        ('Đậu gà', 100, 'g', 164, 8.9, 27.4, 2.6, 7.6, 4.8, 7),
        ('Ức vịt bỏ da', 100, 'g', 140, 28, 0, 3, 0, 0, 75),
        ('Nấm đùi gà', 100, 'g', 35, 3, 6, 0.4, 2.3, 1.2, 5),
        ('Thịt gà', 100, 'g', 165, 27, 0, 6, 0, 0, 75),
        ('Thịt bò', 100, 'g', 217, 26, 0, 12, 0, 0, 72),
        ('Gan gà', 100, 'g', 167, 24, 0.7, 6.5, 0, 0, 71),
        ('Sữa chua không đường', 100, 'g', 61, 3.5, 4.7, 3.3, 0, 4.7, 46),
        ('Phô mai tươi', 100, 'g', 98, 11, 3.4, 4.3, 0, 2.7, 364),
        ('Sữa mẹ', 100, 'ml', 70, 1, 7, 4.2, 0, 7, 17),
        ('Sữa mẹ vắt', 100, 'ml', 70, 1, 7, 4.2, 0, 7, 17),
        ('Sữa công thức giai đoạn 1', 100, 'ml', 67, 1.4, 7.2, 3.5, 0, 7, 25),
        ('Gạo lứt', 100, 'g', 111, 2.6, 23, 0.9, 1.8, 0.4, 5),
        ('Gạo tẻ', 100, 'g', 130, 2.7, 28, 0.3, 0.4, 0.1, 1),
        ('Gạo lứt mềm', 100, 'g', 111, 2.6, 23, 0.9, 1.8, 0.4, 5),
        ('Yến mạch', 100, 'g', 389, 16.9, 66.3, 6.9, 10.6, 1, 2),
        ('Khoai lang', 100, 'g', 86, 1.6, 20.1, 0.1, 3, 4.2, 55),
        ('Khoai tây', 100, 'g', 77, 2, 17, 0.1, 2.2, 0.8, 6),
        ('Diêm mạch', 100, 'g', 120, 4.4, 21.3, 1.9, 2.8, 0.9, 7),
        ('Bún gạo lứt', 100, 'g', 110, 2.5, 24, 0.8, 1.5, 0.2, 5),
        ('Mì nguyên cám', 100, 'g', 124, 5.3, 26, 0.8, 3.9, 0.8, 4),
        ('Nui nhỏ', 100, 'g', 131, 5, 25, 1.1, 1.4, 0.6, 6),
        ('Bánh mì mềm', 100, 'g', 265, 9, 49, 3.2, 2.7, 5, 491),
        ('Hạt kê', 100, 'g', 119, 3.5, 23.7, 1, 1.3, 0.1, 2),
        ('Bông cải xanh', 100, 'g', 35, 2.4, 7.2, 0.4, 3.3, 1.4, 41),
        ('Cải bó xôi', 100, 'g', 23, 2.9, 3.6, 0.4, 2.2, 0.4, 79),
        ('Cà rốt', 100, 'g', 41, 0.9, 9.6, 0.2, 2.8, 4.7, 69),
        ('Bí đỏ', 100, 'g', 26, 1, 6.5, 0.1, 0.5, 2.8, 1),
        ('Đậu que', 100, 'g', 31, 1.8, 7, 0.1, 3.4, 3.3, 6),
        ('Măng tây', 100, 'g', 20, 2.2, 3.9, 0.1, 2.1, 1.9, 2),
        ('Cải thìa', 100, 'g', 13, 1.5, 2.2, 0.2, 1, 1.2, 65),
        ('Bắp non', 100, 'g', 81, 2.7, 19, 0.4, 2.7, 3.2, 15),
        ('Rong biển', 100, 'g', 45, 5.8, 9.6, 0.6, 0.5, 0.6, 872),
        ('Cà chua', 100, 'g', 18, 0.9, 3.9, 0.2, 1.2, 2.6, 5),
        ('Đậu Hà Lan', 100, 'g', 84, 5.4, 15.6, 0.4, 5.5, 5.9, 5),
        ('Bí xanh', 100, 'g', 13, 0.4, 3, 0.2, 1, 1.5, 2),
        ('Su su', 100, 'g', 19, 0.8, 4.5, 0.1, 1.7, 1.7, 2),
        ('Chuối', 100, 'g', 89, 1.1, 22.8, 0.3, 2.6, 12.2, 1),
        ('Bơ', 100, 'g', 160, 2, 8.5, 14.7, 6.7, 0.7, 7),
        ('Táo hấp', 100, 'g', 52, 0.3, 14, 0.2, 2.4, 10.4, 1),
        ('Lê hấp', 100, 'g', 57, 0.4, 15.2, 0.1, 3.1, 9.8, 1),
        ('Đu đủ', 100, 'g', 43, 0.5, 10.8, 0.3, 1.7, 7.8, 8),
        ('Rau ngót', 100, 'g', 35, 5.3, 3.4, 0.3, 2.5, 0.8, 25),
        ('Rau dền', 100, 'g', 23, 2.5, 4, 0.3, 2.1, 0.4, 20),
        ('Cải kale', 100, 'g', 49, 4.3, 8.8, 0.9, 3.6, 2.3, 38),
        ('Dưa leo', 100, 'g', 15, 0.7, 3.6, 0.1, 0.5, 1.7, 2),
        ('Xà lách romaine', 100, 'g', 17, 1.2, 3.3, 0.3, 2.1, 1.2, 8),
        ('Củ dền', 100, 'g', 43, 1.6, 9.6, 0.2, 2.8, 6.8, 78),
        ('Bắp cải tím', 100, 'g', 31, 1.4, 7.4, 0.2, 2.1, 3.8, 27),
        ('Nấm hương', 100, 'g', 34, 2.2, 6.8, 0.5, 2.5, 2.4, 9),
        ('Nấm rơm', 100, 'g', 31, 3.8, 4.6, 0.7, 2.5, 1.5, 21),
        ('Hành tây', 100, 'g', 40, 1.1, 9.3, 0.1, 1.7, 4.2, 4),
        ('Tỏi', 100, 'g', 149, 6.4, 33, 0.5, 2.1, 1, 17),
        ('Gừng', 100, 'g', 80, 1.8, 17.8, 0.8, 2, 1.7, 13),
        ('Nghệ', 100, 'g', 312, 9.7, 67, 3.3, 22.7, 3.2, 27),
        ('Sả', 100, 'g', 99, 1.8, 25, 0.5, 0, 0, 6),
        ('Chanh', 100, 'g', 29, 1.1, 9.3, 0.3, 2.8, 2.5, 2),
        ('Cam', 100, 'g', 47, 0.9, 11.8, 0.1, 2.4, 9.4, 0),
        ('Quýt', 100, 'g', 53, 0.8, 13.3, 0.3, 1.8, 10.6, 2),
        ('Ổi', 100, 'g', 68, 2.6, 14.3, 1, 5.4, 8.9, 2),
        ('Dâu tây', 100, 'g', 32, 0.7, 7.7, 0.3, 2, 4.9, 1),
        ('Việt quất', 100, 'g', 57, 0.7, 14.5, 0.3, 2.4, 10, 1),
        ('Xoài', 100, 'g', 60, 0.8, 15, 0.4, 1.6, 13.7, 1),
        ('Thanh long', 100, 'g', 57, 0.4, 13, 0.1, 3, 8, 1),
        ('Kiwi', 100, 'g', 61, 1.1, 14.7, 0.5, 3, 9, 3),
        ('Lựu', 100, 'g', 83, 1.7, 18.7, 1.2, 4, 13.7, 3),
        ('Hạt chia', 100, 'g', 486, 16.5, 42.1, 30.7, 34.4, 0, 16),
        ('Hạt lanh', 100, 'g', 534, 18.3, 28.9, 42.2, 27.3, 1.6, 30),
        ('Hạt óc chó xay', 100, 'g', 654, 15.2, 13.7, 65.2, 6.7, 2.6, 2),
        ('Hạnh nhân xay', 100, 'g', 579, 21.2, 21.6, 49.9, 12.5, 4.4, 1),
        ('Mè rang xay', 100, 'g', 573, 17.7, 23.4, 49.7, 11.8, 0.3, 11),
        ('Dầu ô liu', 100, 'g', 884, 0, 0, 100, 0, 0, 2),
        ('Dầu mè', 100, 'g', 884, 0, 0, 100, 0, 0, 0),
        ('Dầu hạt cải', 100, 'g', 884, 0, 0, 100, 0, 0, 0),
        ('Nước dùng gà nhạt', 100, 'ml', 12, 1.5, 0.8, 0.4, 0, 0.2, 120),
        ('Nước dùng rau củ', 100, 'ml', 8, 0.3, 1.5, 0.1, 0.3, 0.6, 80),
        ('Nước hầm xương nhạt', 100, 'ml', 20, 2, 0.5, 1, 0, 0, 120),
        ('Hành lá', 100, 'g', 32, 1.8, 7.3, 0.2, 2.6, 2.3, 16),
        ('Ngò rí', 100, 'g', 23, 2.1, 3.7, 0.5, 2.8, 0.9, 46),
        ('Thì là', 100, 'g', 43, 3.5, 7, 1.1, 2.1, 0, 61),
        ('Húng quế', 100, 'g', 23, 3.2, 2.7, 0.6, 1.6, 0.3, 4),
        ('Bạc hà', 100, 'g', 44, 3.3, 8.4, 0.7, 6.8, 0, 31),
        ('Rau mùi tây', 100, 'g', 36, 3, 6.3, 0.8, 3.3, 0.9, 56),
        ('Tiêu đen', 100, 'g', 251, 10.4, 64, 3.3, 25.3, 0.6, 20),
        ('Quế', 100, 'g', 247, 4, 81, 1.2, 53.1, 2.2, 10),
        ('Vani tự nhiên', 100, 'g', 288, 0.1, 12.7, 0.1, 0, 12.7, 9),
        ('Bột cacao nguyên chất', 100, 'g', 228, 19.6, 57.9, 13.7, 33.2, 1.8, 21),
        ('Đậu đỏ', 100, 'g', 127, 7.5, 22.8, 0.5, 7.3, 0.3, 2),
        ('Đậu đen', 100, 'g', 132, 8.9, 23.7, 0.5, 8.7, 0.3, 1),
        ('Đậu xanh', 100, 'g', 105, 7, 19, 0.4, 7.6, 2, 2),
        ('Đậu nành non', 100, 'g', 121, 11.9, 8.9, 5.2, 5.2, 2.2, 6),
        ('Bí ngòi', 100, 'g', 17, 1.2, 3.1, 0.3, 1, 2.5, 8),
        ('Ớt chuông', 100, 'g', 31, 1, 6, 0.3, 2.1, 4.2, 4),
        ('Củ cải trắng', 100, 'g', 18, 0.6, 4.1, 0.1, 1.6, 2.5, 21),
        ('Củ sen', 100, 'g', 74, 2.6, 17.2, 0.1, 4.9, 0.5, 40),
        ('Khoai mỡ', 100, 'g', 118, 1.5, 27.9, 0.2, 4.1, 0.5, 9),
        ('Khoai sọ', 100, 'g', 112, 1.5, 26.5, 0.2, 4.1, 0.4, 11),
        ('Miến dong', 100, 'g', 351, 0.2, 86, 0.1, 0.8, 0, 10),
        ('Bột gạo', 100, 'g', 366, 6, 80, 1.4, 2.4, 0.1, 0),
        ('Bột ngô', 100, 'g', 381, 7, 91, 0.7, 0.9, 0.6, 9),
        ('Bột đậu xanh', 100, 'g', 347, 24, 63, 1.2, 16, 6.6, 15),
        ('Ức gà xay', 100, 'g', 165, 31, 0, 3.6, 0, 0, 74),
        ('Cá hồi xay', 100, 'g', 208, 20, 0, 13, 0, 0, 59),
        ('Thịt bò xay', 100, 'g', 217, 26, 0, 12, 0, 0, 72),
        ('Thịt heo xay', 100, 'g', 143, 21, 0, 6, 0, 0, 62),
        ('Đậu phụ nghiền', 100, 'g', 76, 8, 1.9, 4.8, 0.3, 0.6, 7),
        ('Sữa tươi không đường', 100, 'ml', 61, 3.2, 4.8, 3.3, 0, 5, 43),
        ('Sữa hạt không đường', 100, 'ml', 35, 1, 3, 2.5, 0.5, 0.2, 35),
        ('Rau má', 100, 'g', 37, 2, 6.7, 0.2, 2.1, 0.5, 20),
        ('Atiso', 100, 'g', 47, 3.3, 10.5, 0.2, 5.4, 1, 94),
        ('Nước lọc', 100, 'ml', 0, 0, 0, 0, 0, 0, 0)
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
    fi."id",
    ns.base_amount,
    ns.base_unit,
    ns.calories,
    'kcal',
    ns.protein,
    'g',
    ns.carbs,
    'g',
    ns.fat,
    'g',
    ns.fiber,
    'g',
    ns.sugar,
    'g',
    ns.sodium,
    'mg',
    2
FROM ingredient_nutrition_seed ns
JOIN "food_ingredients" fi ON fi."name_ingredients" = ns.name
WHERE NOT EXISTS (
    SELECT 1
    FROM "ingredient_nutrition" inn
    WHERE inn."food_ingredient_id" = fi."id"
);
-- changeset codex:016
-- validCheckSum: ANY
DROP TABLE IF EXISTS seed_food_recipe;

CREATE TEMP TABLE seed_food_recipe (
    food_id BIGINT,
    function_code BIGINT,
    name TEXT,
    protein TEXT,
    carb TEXT,
    veg TEXT,
    total_calories DOUBLE PRECISION,
    total_protein DOUBLE PRECISION,
    total_carbs DOUBLE PRECISION,
    total_fat DOUBLE PRECISION
) ON COMMIT DROP;

INSERT INTO seed_food_recipe (
    food_id, function_code, name, protein, carb, veg, total_calories, total_protein, total_carbs, total_fat
) VALUES
    (1, 1, 'Salad ức gà gạo lứt bơ', 'Ức gà', 'Gạo lứt', 'Bơ', 430, 34, 42, 14),
    (2, 1, 'Cá hồi áp chảo diêm mạch bông cải', 'Cá hồi', 'Diêm mạch', 'Bông cải xanh', 465, 31, 38, 19),
    (3, 1, 'Bowl tôm khoai lang cải bó xôi', 'Tôm', 'Khoai lang', 'Cải bó xôi', 390, 29, 44, 10),
    (4, 1, 'Thịt bò nạc xào bún gạo lứt đậu que', 'Thịt bò nạc', 'Bún gạo lứt', 'Đậu que', 455, 33, 49, 13),
    (5, 1, 'Đậu hũ sốt cà chua ăn cùng gạo lứt', 'Đậu hũ', 'Gạo lứt', 'Cà chua', 405, 24, 47, 13),
    (6, 1, 'Trứng gà cuộn nấm đùi gà và măng tây', 'Trứng gà', 'Nấm đùi gà', 'Măng tây', 360, 23, 24, 18),
    (7, 1, 'Cá thu hấp gừng ăn cùng khoai tây và cải thìa', 'Cá thu', 'Khoai tây', 'Cải thìa', 440, 30, 39, 18),
    (8, 1, 'Ức gà xé trộn yến mạch và dưa leo', 'Ức gà', 'Yến mạch', 'Dưa leo', 410, 35, 37, 12),
    (9, 1, 'Tôm hấp sả ăn cùng bí đỏ và rau ngót', 'Tôm', 'Bí đỏ', 'Rau ngót', 340, 28, 35, 8),
    (10, 1, 'Đậu lăng nấu cà rốt và gạo lứt', 'Đậu lăng', 'Gạo lứt', 'Cà rốt', 420, 25, 58, 8),
    (11, 1, 'Cá trắng hấp chanh ăn cùng su su và diêm mạch', 'Cá trắng', 'Diêm mạch', 'Su su', 385, 30, 40, 9),
    (12, 1, 'Thịt heo nạc rim nhạt ăn cùng bông cải xanh', 'Thịt heo nạc', 'Gạo lứt', 'Bông cải xanh', 425, 32, 40, 14),
    (13, 1, 'Đậu gà nghiền bơ và xà lách romaine', 'Đậu gà', 'Bơ', 'Xà lách romaine', 430, 20, 42, 20),
    (14, 1, 'Ức vịt bỏ da áp chảo ăn cùng khoai lang', 'Ức vịt bỏ da', 'Khoai lang', 'Cải kale', 455, 34, 43, 15),
    (15, 1, 'Nấm đùi gà xào cải kale và mì nguyên cám', 'Nấm đùi gà', 'Mì nguyên cám', 'Cải kale', 390, 18, 55, 10),
    (16, 1, 'Salad cá hồi bơ dưa leo', 'Cá hồi', 'Bơ', 'Dưa leo', 445, 29, 18, 28),
    (17, 1, 'Bowl thịt bò nạc khoai lang và cà chua', 'Thịt bò nạc', 'Khoai lang', 'Cà chua', 430, 33, 45, 12),
    (18, 1, 'Súp đậu lăng đỏ bí đỏ', 'Đậu lăng đỏ', 'Bí đỏ', 'Cà rốt', 365, 22, 56, 6),
    (19, 1, 'Ức gà hấp nghệ ăn cùng đậu Hà Lan', 'Ức gà', 'Gạo lứt', 'Đậu Hà Lan', 420, 35, 44, 10),
    (20, 1, 'Tôm xào bắp non và gạo lứt', 'Tôm', 'Gạo lứt', 'Bắp non', 395, 29, 46, 9),
    (21, 1, 'Yến mạch mặn trứng gà cải bó xôi', 'Trứng gà', 'Yến mạch', 'Cải bó xôi', 410, 24, 42, 16),
    (22, 1, 'Cơm gạo lứt cá thu sốt cà chua', 'Cá thu', 'Gạo lứt', 'Cà chua', 470, 31, 45, 18),
    (23, 1, 'Diêm mạch đậu hũ nấm hương', 'Đậu hũ', 'Diêm mạch', 'Nấm hương', 420, 25, 48, 13),
    (24, 1, 'Nui nguyên cám gà xé bông cải', 'Thịt gà', 'Nui nhỏ', 'Bông cải xanh', 440, 32, 50, 11),
    (25, 1, 'Bún gạo lứt tôm rau ngót', 'Tôm', 'Bún gạo lứt', 'Rau ngót', 400, 29, 48, 8),
    (26, 1, 'Cháo yến mạch thịt bò cà rốt', 'Thịt bò nạc', 'Yến mạch', 'Cà rốt', 430, 31, 43, 13),
    (27, 1, 'Đậu gà hầm bí đỏ và cải thìa', 'Đậu gà', 'Bí đỏ', 'Cải thìa', 390, 20, 54, 9),
    (28, 1, 'Cá trắng nướng giấy bạc với măng tây', 'Cá trắng', 'Khoai tây', 'Măng tây', 380, 31, 36, 10),
    (29, 1, 'Trứng gà hấp đậu hũ non và rong biển', 'Trứng gà', 'Đậu hũ non', 'Rong biển', 350, 24, 18, 20),
    (30, 1, 'Gà áp chảo sốt cam và đậu que', 'Thịt gà', 'Gạo lứt', 'Đậu que', 430, 33, 43, 12),
    (31, 1, 'Mì nguyên cám thịt heo nạc nấm rơm', 'Thịt heo nạc', 'Mì nguyên cám', 'Nấm rơm', 455, 32, 54, 13),
    (32, 1, 'Salad đậu lăng cà chua dưa leo', 'Đậu lăng', 'Diêm mạch', 'Dưa leo', 395, 23, 52, 9),
    (33, 1, 'Khoai tây nghiền cá hồi cải bó xôi', 'Cá hồi', 'Khoai tây', 'Cải bó xôi', 455, 30, 38, 19),
    (34, 1, 'Cơm gạo lứt bò xào ớt chuông', 'Thịt bò nạc', 'Gạo lứt', 'Ớt chuông', 445, 33, 44, 14),
    (35, 1, 'Cháo hạt kê tôm bí xanh', 'Tôm', 'Hạt kê', 'Bí xanh', 385, 28, 49, 7),
    (36, 1, 'Đậu nành non xào nấm đùi gà', 'Đậu nành non', 'Gạo lứt', 'Nấm đùi gà', 410, 24, 45, 13),
    (37, 1, 'Bánh mì mềm trứng và bơ', 'Trứng gà', 'Bánh mì mềm', 'Bơ', 455, 22, 42, 22),
    (38, 1, 'Súp gà bắp non cà rốt', 'Thịt gà', 'Bắp non', 'Cà rốt', 380, 30, 38, 10),
    (39, 1, 'Miến dong thịt bò cải thìa', 'Thịt bò nạc', 'Miến dong', 'Cải thìa', 420, 31, 50, 9),
    (40, 1, 'Khoai lang đậu hũ sốt nấm', 'Đậu hũ', 'Khoai lang', 'Nấm hương', 405, 24, 48, 12),
    (41, 1, 'Cháo gà rau ngót gạo tẻ', 'Thịt gà', 'Gạo tẻ', 'Rau ngót', 430, 31, 50, 10),
    (42, 1, 'Cá hồi kho nghệ ăn cùng gạo lứt', 'Cá hồi', 'Gạo lứt', 'Nghệ', 480, 32, 44, 20),
    (43, 1, 'Thịt bò hầm khoai tây cà rốt', 'Thịt bò nạc', 'Khoai tây', 'Cà rốt', 465, 34, 42, 15),
    (44, 1, 'Canh tôm bí xanh ăn cùng cơm mềm', 'Tôm', 'Gạo tẻ', 'Bí xanh', 400, 29, 48, 8),
    (45, 1, 'Trứng gà hấp thịt heo nạc', 'Trứng gà', 'Gạo tẻ', 'Thịt heo nạc', 460, 32, 40, 18),
    (46, 1, 'Gà hầm hạt kê và nấm hương', 'Thịt gà', 'Hạt kê', 'Nấm hương', 440, 32, 48, 11),
    (47, 1, 'Đậu đỏ hầm thịt bò nạc', 'Thịt bò nạc', 'Đậu đỏ', 'Cà rốt', 470, 35, 50, 11),
    (48, 1, 'Cá thu nấu cà chua thì là', 'Cá thu', 'Gạo tẻ', 'Cà chua', 465, 31, 43, 18),
    (49, 1, 'Cháo đậu xanh thịt gà', 'Thịt gà', 'Đậu xanh', 'Rau ngót', 425, 31, 48, 10),
    (50, 1, 'Đậu hũ non sốt thịt heo xay', 'Thịt heo xay', 'Đậu hũ non', 'Cà chua', 420, 30, 22, 20),
    (51, 1, 'Súp bí đỏ cá hồi', 'Cá hồi', 'Bí đỏ', 'Cà rốt', 430, 29, 36, 18),
    (52, 1, 'Thịt heo nạc kho gừng với su su', 'Thịt heo nạc', 'Gạo tẻ', 'Su su', 430, 32, 45, 13),
    (53, 1, 'Cơm gạo lứt tôm hấp bông cải', 'Tôm', 'Gạo lứt', 'Bông cải xanh', 410, 30, 45, 9),
    (54, 1, 'Canh đậu lăng đỏ cà rốt', 'Đậu lăng đỏ', 'Gạo tẻ', 'Cà rốt', 420, 24, 58, 7),
    (55, 1, 'Cháo gan gà cải bó xôi', 'Gan gà', 'Gạo tẻ', 'Cải bó xôi', 410, 28, 46, 11),
    (56, 1, 'Cá trắng hấp gừng hành lá', 'Cá trắng', 'Gạo tẻ', 'Hành lá', 395, 31, 43, 9),
    (57, 1, 'Thịt bò xào cải kale', 'Thịt bò nạc', 'Gạo lứt', 'Cải kale', 445, 34, 43, 14),
    (58, 1, 'Gà nấu khoai mỡ rau ngót', 'Thịt gà', 'Khoai mỡ', 'Rau ngót', 450, 32, 52, 10),
    (59, 1, 'Nui nhỏ thịt heo nạc cà chua', 'Thịt heo nạc', 'Nui nhỏ', 'Cà chua', 455, 31, 54, 12),
    (60, 1, 'Diêm mạch trứng gà phô mai tươi', 'Trứng gà', 'Diêm mạch', 'Phô mai tươi', 465, 27, 45, 19),
    (61, 1, 'Bowl cá hồi khoai lang bơ', 'Cá hồi', 'Khoai lang', 'Bơ', 520, 32, 47, 24),
    (62, 1, 'Cơm gạo lứt gà nướng sả', 'Thịt gà', 'Gạo lứt', 'Sả', 490, 35, 50, 14),
    (63, 1, 'Yến mạch chuối hạt chia sữa chua', 'Sữa chua không đường', 'Yến mạch', 'Chuối', 430, 20, 58, 13),
    (64, 1, 'Diêm mạch bò nạc đậu Hà Lan', 'Thịt bò nạc', 'Diêm mạch', 'Đậu Hà Lan', 485, 35, 50, 13),
    (65, 1, 'Khoai tây tôm phô mai tươi', 'Tôm', 'Khoai tây', 'Phô mai tươi', 445, 31, 42, 15),
    (66, 1, 'Mì nguyên cám cá thu rau cải', 'Cá thu', 'Mì nguyên cám', 'Cải thìa', 520, 33, 56, 18),
    (67, 1, 'Đậu gà hầm gà và cà rốt', 'Thịt gà', 'Đậu gà', 'Cà rốt', 500, 36, 52, 14),
    (68, 1, 'Bún gạo lứt thịt heo nạc rau thơm', 'Thịt heo nạc', 'Bún gạo lứt', 'Ngò rí', 470, 32, 55, 12),
    (69, 1, 'Trứng gà bơ bánh mì mềm', 'Trứng gà', 'Bánh mì mềm', 'Bơ', 505, 24, 47, 25),
    (70, 1, 'Cá trắng kho cà chua ăn cùng khoai sọ', 'Cá trắng', 'Khoai sọ', 'Cà chua', 450, 31, 49, 10),
    (71, 1, 'Đậu đen hầm thịt bò nạc', 'Thịt bò nạc', 'Đậu đen', 'Cà rốt', 510, 36, 54, 13),
    (72, 1, 'Gạo lứt cá hồi rong biển', 'Cá hồi', 'Gạo lứt', 'Rong biển', 500, 32, 46, 21),
    (73, 1, 'Cháo yến mạch tôm bí đỏ', 'Tôm', 'Yến mạch', 'Bí đỏ', 425, 29, 50, 9),
    (74, 1, 'Nui nhỏ gà nấm đùi gà', 'Thịt gà', 'Nui nhỏ', 'Nấm đùi gà', 470, 33, 55, 11),
    (75, 1, 'Khoai lang đậu hũ mè rang xay', 'Đậu hũ', 'Khoai lang', 'Mè rang xay', 455, 24, 50, 17),
    (76, 1, 'Cơm gạo tẻ thịt bò xào bắp non', 'Thịt bò nạc', 'Gạo tẻ', 'Bắp non', 485, 34, 52, 13),
    (77, 1, 'Hạt kê cá trắng cải bó xôi', 'Cá trắng', 'Hạt kê', 'Cải bó xôi', 435, 31, 51, 9),
    (78, 1, 'Đậu lăng đỏ thịt heo nạc bí xanh', 'Thịt heo nạc', 'Đậu lăng đỏ', 'Bí xanh', 475, 34, 48, 13),
    (79, 1, 'Bánh mì mềm cá hồi sữa chua', 'Cá hồi', 'Bánh mì mềm', 'Sữa chua không đường', 515, 33, 48, 21),
    (80, 1, 'Gà xào nấm rơm và khoai tây', 'Thịt gà', 'Khoai tây', 'Nấm rơm', 455, 33, 46, 12),
    (81, 1, 'Cháo yến mạch bí đỏ đậu hũ non', 'Đậu hũ non', 'Yến mạch', 'Bí đỏ', 370, 22, 45, 11),
    (82, 1, 'Súp gà cà rốt khoai tây', 'Thịt gà', 'Khoai tây', 'Cà rốt', 405, 30, 42, 10),
    (83, 1, 'Cháo cá trắng rau ngót', 'Cá trắng', 'Gạo tẻ', 'Rau ngót', 385, 30, 44, 8),
    (84, 1, 'Canh bí xanh tôm gừng', 'Tôm', 'Gạo tẻ', 'Bí xanh', 365, 28, 42, 7),
    (85, 1, 'Cháo đậu xanh thịt heo nạc', 'Thịt heo nạc', 'Đậu xanh', 'Cà rốt', 410, 31, 46, 11),
    (86, 1, 'Súp khoai lang đậu lăng đỏ', 'Đậu lăng đỏ', 'Khoai lang', 'Bí đỏ', 390, 22, 58, 6),
    (87, 1, 'Cơm mềm cá hồi cải thìa', 'Cá hồi', 'Gạo tẻ', 'Cải thìa', 430, 30, 44, 17),
    (88, 1, 'Đậu hũ non hấp trứng', 'Trứng gà', 'Đậu hũ non', 'Hành lá', 350, 24, 18, 18),
    (89, 1, 'Cháo thịt bò bí đỏ', 'Thịt bò nạc', 'Gạo tẻ', 'Bí đỏ', 410, 31, 47, 10),
    (90, 1, 'Súp nấm hương gà xé', 'Thịt gà', 'Khoai tây', 'Nấm hương', 395, 30, 39, 10),
    (91, 1, 'Cháo hạt kê cá thu thì là', 'Cá thu', 'Hạt kê', 'Thì là', 430, 30, 46, 16),
    (92, 1, 'Canh rau dền thịt băm', 'Thịt heo xay', 'Gạo tẻ', 'Rau dền', 395, 29, 43, 11),
    (93, 1, 'Súp bắp non tôm', 'Tôm', 'Bắp non', 'Cà rốt', 360, 28, 36, 8),
    (94, 1, 'Cháo gạo lứt mềm đậu đỏ', 'Đậu đỏ', 'Gạo lứt mềm', 'Cà rốt', 405, 22, 58, 7),
    (95, 1, 'Cá trắng hấp su su', 'Cá trắng', 'Gạo tẻ', 'Su su', 380, 30, 43, 8),
    (96, 1, 'Nui nhỏ sốt cà chua thịt bò', 'Thịt bò nạc', 'Nui nhỏ', 'Cà chua', 430, 32, 51, 10),
    (97, 1, 'Khoai sọ hầm gà rau ngót', 'Thịt gà', 'Khoai sọ', 'Rau ngót', 420, 31, 48, 10),
    (98, 1, 'Cháo gan gà cà rốt', 'Gan gà', 'Gạo tẻ', 'Cà rốt', 395, 28, 45, 10),
    (99, 1, 'Canh củ sen thịt heo nạc', 'Thịt heo nạc', 'Củ sen', 'Cà rốt', 410, 31, 43, 11),
    (100, 1, 'Đậu lăng hầm nấm đùi gà', 'Đậu lăng', 'Gạo tẻ', 'Nấm đùi gà', 395, 23, 54, 7),
    (101, 2, 'Cháo mịn gạo tẻ thịt gà bí đỏ', 'Thịt gà', 'Gạo tẻ', 'Bí đỏ', 105, 7, 16, 2),
    (102, 2, 'Cháo mịn gạo tẻ cá trắng cà rốt', 'Cá trắng', 'Gạo tẻ', 'Cà rốt', 100, 7, 15, 2),
    (103, 2, 'Cháo mịn yến mạch lòng đỏ trứng bí xanh', 'Lòng đỏ trứng', 'Yến mạch', 'Bí xanh', 115, 6, 14, 5),
    (104, 2, 'Bột gạo đậu hũ non cải bó xôi', 'Đậu hũ non', 'Bột gạo', 'Cải bó xôi', 105, 5, 16, 3),
    (105, 2, 'Cháo khoai lang cá hồi bông cải', 'Cá hồi', 'Khoai lang', 'Bông cải xanh', 120, 7, 15, 5),
    (106, 2, 'Cháo khoai tây thịt bò cà rốt', 'Thịt bò', 'Khoai tây', 'Cà rốt', 118, 8, 16, 3),
    (107, 2, 'Cháo gạo tẻ tôm bí đỏ', 'Tôm', 'Gạo tẻ', 'Bí đỏ', 105, 7, 16, 2),
    (108, 2, 'Cháo đậu lăng đỏ bí xanh', 'Đậu lăng đỏ', 'Gạo tẻ', 'Bí xanh', 110, 6, 19, 1),
    (109, 2, 'Bột ngô thịt heo nạc su su', 'Thịt heo nạc', 'Bột ngô', 'Su su', 115, 7, 17, 3),
    (110, 2, 'Cháo yến mạch gà rau ngót', 'Thịt gà', 'Yến mạch', 'Rau ngót', 112, 7, 16, 3),
    (111, 2, 'Cháo gạo lứt mềm cá hồi bí đỏ', 'Cá hồi', 'Gạo lứt mềm', 'Bí đỏ', 125, 7, 17, 5),
    (112, 2, 'Bột đậu xanh thịt gà cà rốt', 'Thịt gà', 'Bột đậu xanh', 'Cà rốt', 118, 8, 18, 2),
    (113, 2, 'Cháo khoai lang đậu hũ non đậu Hà Lan', 'Đậu hũ non', 'Khoai lang', 'Đậu Hà Lan', 120, 6, 18, 4),
    (114, 2, 'Cháo gạo tẻ gan gà cải bó xôi', 'Gan gà', 'Gạo tẻ', 'Cải bó xôi', 110, 7, 15, 3),
    (115, 2, 'Cháo hạt kê cá trắng bí xanh', 'Cá trắng', 'Hạt kê', 'Bí xanh', 108, 7, 17, 2),
    (116, 2, 'Nghiền bơ sữa chua không đường yến mạch', 'Sữa chua không đường', 'Yến mạch', 'Bơ', 130, 5, 14, 7),
    (117, 2, 'Táo hấp nghiền yến mạch phô mai tươi', 'Phô mai tươi', 'Yến mạch', 'Táo hấp', 125, 5, 18, 4),
    (118, 2, 'Lê hấp nghiền khoai lang sữa chua', 'Sữa chua không đường', 'Khoai lang', 'Lê hấp', 120, 4, 20, 3),
    (119, 2, 'Cháo bí đỏ thịt bò xay', 'Thịt bò xay', 'Gạo tẻ', 'Bí đỏ', 115, 8, 16, 3),
    (120, 2, 'Cháo cà rốt thịt gà xay', 'Ức gà xay', 'Gạo tẻ', 'Cà rốt', 110, 8, 15, 2),
    (121, 2, 'Cháo cá hồi xay rau dền', 'Cá hồi xay', 'Gạo tẻ', 'Rau dền', 120, 7, 15, 5),
    (122, 2, 'Cháo thịt heo xay bí xanh', 'Thịt heo xay', 'Gạo tẻ', 'Bí xanh', 112, 7, 16, 3),
    (123, 2, 'Cháo đậu phụ nghiền bông cải', 'Đậu phụ nghiền', 'Gạo tẻ', 'Bông cải xanh', 105, 5, 16, 3),
    (124, 2, 'Cháo gạo tẻ lòng đỏ trứng cà rốt', 'Lòng đỏ trứng', 'Gạo tẻ', 'Cà rốt', 115, 6, 16, 5),
    (125, 2, 'Cháo khoai mỡ thịt gà rau ngót', 'Thịt gà', 'Khoai mỡ', 'Rau ngót', 125, 7, 19, 3),
    (126, 2, 'Cháo đặc thịt gà bí đỏ cà rốt', 'Thịt gà', 'Gạo tẻ', 'Bí đỏ', 145, 9, 22, 3),
    (127, 2, 'Cháo đặc cá hồi khoai lang cải bó xôi', 'Cá hồi', 'Khoai lang', 'Cải bó xôi', 155, 9, 20, 6),
    (128, 2, 'Nui nhỏ mềm thịt bò cà chua', 'Thịt bò', 'Nui nhỏ', 'Cà chua', 160, 10, 24, 4),
    (129, 2, 'Cháo đặc tôm bí xanh', 'Tôm', 'Gạo tẻ', 'Bí xanh', 140, 9, 21, 2),
    (130, 2, 'Cơm nát rất mềm đậu hũ non cà rốt', 'Đậu hũ non', 'Gạo tẻ', 'Cà rốt', 145, 6, 23, 4),
    (131, 2, 'Yến mạch chuối sữa chua không đường', 'Sữa chua không đường', 'Yến mạch', 'Chuối', 150, 6, 25, 4),
    (132, 2, 'Khoai tây nghiền cá trắng đậu Hà Lan', 'Cá trắng', 'Khoai tây', 'Đậu Hà Lan', 150, 9, 22, 3),
    (133, 2, 'Cháo đậu lăng đỏ thịt heo nạc bí đỏ', 'Thịt heo nạc', 'Đậu lăng đỏ', 'Bí đỏ', 160, 10, 22, 4),
    (134, 2, 'Bánh mì mềm phô mai tươi bơ', 'Phô mai tươi', 'Bánh mì mềm', 'Bơ', 170, 7, 22, 7),
    (135, 2, 'Cháo hạt kê gan gà rau ngót', 'Gan gà', 'Hạt kê', 'Rau ngót', 150, 8, 22, 4),
    (136, 2, 'Nui nhỏ gà xé bông cải', 'Thịt gà', 'Nui nhỏ', 'Bông cải xanh', 158, 10, 24, 3),
    (137, 2, 'Cháo gạo lứt mềm cá thu cà chua', 'Cá thu', 'Gạo lứt mềm', 'Cà chua', 165, 9, 22, 6),
    (138, 2, 'Miến dong mềm thịt bò cải thìa', 'Thịt bò', 'Miến dong', 'Cải thìa', 150, 9, 25, 2),
    (139, 2, 'Cháo trứng gà đậu hũ non bí xanh', 'Trứng gà', 'Đậu hũ non', 'Bí xanh', 145, 8, 10, 8),
    (140, 2, 'Khoai lang nghiền đậu gà cà rốt', 'Đậu gà', 'Khoai lang', 'Cà rốt', 160, 7, 28, 3),
    (141, 2, 'Cháo thịt heo xay su su', 'Thịt heo xay', 'Gạo tẻ', 'Su su', 145, 9, 22, 3),
    (142, 2, 'Cháo cá trắng thì là bí đỏ', 'Cá trắng', 'Gạo tẻ', 'Bí đỏ', 142, 9, 21, 2),
    (143, 2, 'Cháo tôm bắp non cà rốt', 'Tôm', 'Bắp non', 'Cà rốt', 145, 9, 21, 2),
    (144, 2, 'Cháo gà nấm rơm bí xanh', 'Thịt gà', 'Gạo tẻ', 'Nấm rơm', 145, 9, 22, 3),
    (145, 2, 'Cháo bò khoai tây bông cải', 'Thịt bò', 'Khoai tây', 'Bông cải xanh', 158, 10, 22, 4),
    (146, 2, 'Sữa chua đu đủ yến mạch', 'Sữa chua không đường', 'Yến mạch', 'Đu đủ', 145, 6, 24, 3),
    (147, 2, 'Táo hấp nghiền hạt chia rất mịn', 'Sữa chua không đường', 'Hạt chia', 'Táo hấp', 135, 5, 18, 5),
    (148, 2, 'Cháo đậu xanh thịt gà cải bó xôi', 'Thịt gà', 'Đậu xanh', 'Cải bó xôi', 155, 10, 22, 3),
    (149, 2, 'Cháo cá hồi rong biển bí đỏ', 'Cá hồi', 'Gạo tẻ', 'Rong biển', 155, 9, 20, 6),
    (150, 2, 'Cháo thịt bò xay rau dền', 'Thịt bò xay', 'Gạo tẻ', 'Rau dền', 150, 10, 21, 4),
    (151, 2, 'Cơm nát gà xé bí đỏ', 'Thịt gà', 'Gạo tẻ', 'Bí đỏ', 210, 13, 32, 4),
    (152, 2, 'Cơm nát cá hồi bông cải', 'Cá hồi', 'Gạo tẻ', 'Bông cải xanh', 225, 13, 30, 8),
    (153, 2, 'Cơm nát thịt bò cà rốt', 'Thịt bò', 'Gạo tẻ', 'Cà rốt', 220, 14, 32, 5),
    (154, 2, 'Cơm nát tôm bí xanh', 'Tôm', 'Gạo tẻ', 'Bí xanh', 205, 13, 31, 3),
    (155, 2, 'Nui nhỏ sốt cà chua thịt heo', 'Thịt heo nạc', 'Nui nhỏ', 'Cà chua', 230, 13, 34, 5),
    (156, 2, 'Trứng hấp đậu hũ non rau ngót', 'Trứng gà', 'Đậu hũ non', 'Rau ngót', 190, 11, 10, 11),
    (157, 2, 'Miến dong thịt gà cải thìa', 'Thịt gà', 'Miến dong', 'Cải thìa', 210, 12, 34, 3),
    (158, 2, 'Khoai lang viên mềm cá trắng', 'Cá trắng', 'Khoai lang', 'Cà rốt', 205, 12, 31, 3),
    (159, 2, 'Cháo đặc đậu gà bí đỏ', 'Đậu gà', 'Gạo tẻ', 'Bí đỏ', 215, 9, 36, 4),
    (160, 2, 'Bánh mì mềm trứng bơ', 'Trứng gà', 'Bánh mì mềm', 'Bơ', 245, 10, 28, 11),
    (161, 2, 'Cơm nát cá thu cà chua', 'Cá thu', 'Gạo tẻ', 'Cà chua', 230, 13, 31, 8),
    (162, 2, 'Cơm nát thịt heo nạc su su', 'Thịt heo nạc', 'Gạo tẻ', 'Su su', 220, 13, 32, 5),
    (163, 2, 'Nui nhỏ phô mai tươi bông cải', 'Phô mai tươi', 'Nui nhỏ', 'Bông cải xanh', 225, 9, 33, 7),
    (164, 2, 'Súp khoai tây thịt bò đậu Hà Lan', 'Thịt bò', 'Khoai tây', 'Đậu Hà Lan', 225, 14, 32, 5),
    (165, 2, 'Cháo yến mạch tôm cà rốt', 'Tôm', 'Yến mạch', 'Cà rốt', 210, 13, 31, 3),
    (166, 2, 'Đậu hũ non sốt thịt gà cà chua', 'Thịt gà', 'Đậu hũ non', 'Cà chua', 200, 13, 11, 10),
    (167, 2, 'Cơm nát gan gà cải bó xôi', 'Gan gà', 'Gạo tẻ', 'Cải bó xôi', 215, 12, 32, 5),
    (168, 2, 'Súp bí đỏ cá hồi phô mai tươi', 'Cá hồi', 'Bí đỏ', 'Phô mai tươi', 225, 12, 24, 9),
    (169, 2, 'Cháo đậu đỏ thịt bò', 'Thịt bò', 'Đậu đỏ', 'Cà rốt', 230, 14, 34, 5),
    (170, 2, 'Khoai mỡ nghiền gà rau ngót', 'Thịt gà', 'Khoai mỡ', 'Rau ngót', 220, 13, 34, 4),
    (171, 2, 'Cơm nát đậu lăng đỏ cà rốt', 'Đậu lăng đỏ', 'Gạo tẻ', 'Cà rốt', 215, 10, 36, 3),
    (172, 2, 'Nui nhỏ cá trắng bí xanh', 'Cá trắng', 'Nui nhỏ', 'Bí xanh', 215, 13, 34, 3),
    (173, 2, 'Sữa chua xoài yến mạch', 'Sữa chua không đường', 'Yến mạch', 'Xoài', 210, 7, 34, 5),
    (174, 2, 'Cơm nát tôm bắp non', 'Tôm', 'Gạo tẻ', 'Bắp non', 210, 13, 32, 3),
    (175, 2, 'Cháo gạo lứt mềm thịt heo bí đỏ', 'Thịt heo nạc', 'Gạo lứt mềm', 'Bí đỏ', 220, 13, 33, 5),
    (176, 2, 'Cơm mềm gà nấm hương cải thìa', 'Thịt gà', 'Gạo tẻ', 'Nấm hương', 270, 16, 40, 5),
    (177, 2, 'Cơm mềm cá hồi bí đỏ', 'Cá hồi', 'Gạo tẻ', 'Bí đỏ', 285, 16, 38, 9),
    (178, 2, 'Cơm mềm bò xào bông cải', 'Thịt bò', 'Gạo tẻ', 'Bông cải xanh', 280, 17, 39, 6),
    (179, 2, 'Nui nhỏ tôm sốt cà chua', 'Tôm', 'Nui nhỏ', 'Cà chua', 265, 16, 41, 4),
    (180, 2, 'Mì nguyên cám mềm thịt heo cải bó xôi', 'Thịt heo nạc', 'Mì nguyên cám', 'Cải bó xôi', 290, 16, 43, 6),
    (181, 2, 'Trứng cuộn mềm đậu hũ non cà rốt', 'Trứng gà', 'Đậu hũ non', 'Cà rốt', 230, 13, 13, 13),
    (182, 2, 'Cá trắng hấp gừng cơm mềm', 'Cá trắng', 'Gạo tẻ', 'Gừng', 255, 16, 38, 4),
    (183, 2, 'Cháo hạt kê gà bí xanh', 'Thịt gà', 'Hạt kê', 'Bí xanh', 270, 16, 41, 5),
    (184, 2, 'Bánh mì mềm cá hồi phô mai tươi', 'Cá hồi', 'Bánh mì mềm', 'Phô mai tươi', 305, 16, 35, 12),
    (185, 2, 'Cơm mềm đậu gà bí đỏ', 'Đậu gà', 'Gạo tẻ', 'Bí đỏ', 275, 11, 46, 5),
    (186, 2, 'Miến dong bò cà chua', 'Thịt bò', 'Miến dong', 'Cà chua', 265, 16, 42, 4),
    (187, 2, 'Khoai tây hầm thịt gà đậu Hà Lan', 'Thịt gà', 'Khoai tây', 'Đậu Hà Lan', 275, 16, 41, 5),
    (188, 2, 'Cơm mềm cá thu rau ngót', 'Cá thu', 'Gạo tẻ', 'Rau ngót', 290, 16, 39, 9),
    (189, 2, 'Nui nhỏ thịt bò bí đỏ', 'Thịt bò', 'Nui nhỏ', 'Bí đỏ', 285, 17, 43, 6),
    (190, 2, 'Đậu hũ non nấu tôm bông cải', 'Tôm', 'Đậu hũ non', 'Bông cải xanh', 235, 15, 13, 10),
    (191, 2, 'Cơm mềm heo nạc su su', 'Thịt heo nạc', 'Gạo tẻ', 'Su su', 275, 16, 40, 6),
    (192, 2, 'Cháo đậu xanh cá trắng cà rốt', 'Cá trắng', 'Đậu xanh', 'Cà rốt', 270, 16, 40, 4),
    (193, 2, 'Cơm mềm gan gà cải bó xôi', 'Gan gà', 'Gạo tẻ', 'Cải bó xôi', 270, 15, 39, 6),
    (194, 2, 'Khoai lang nghiền trứng gà', 'Trứng gà', 'Khoai lang', 'Cà rốt', 275, 12, 39, 9),
    (195, 2, 'Súp nấm rơm thịt gà', 'Thịt gà', 'Khoai tây', 'Nấm rơm', 260, 16, 37, 5),
    (196, 2, 'Bún gạo lứt mềm cá hồi cải thìa', 'Cá hồi', 'Bún gạo lứt', 'Cải thìa', 295, 16, 41, 9),
    (197, 2, 'Cơm mềm tôm bí xanh', 'Tôm', 'Gạo tẻ', 'Bí xanh', 260, 16, 39, 4),
    (198, 2, 'Nui nhỏ gà cà rốt', 'Thịt gà', 'Nui nhỏ', 'Cà rốt', 280, 16, 43, 5),
    (199, 2, 'Sữa chua chuối hạt chia nghiền', 'Sữa chua không đường', 'Hạt chia', 'Chuối', 240, 8, 30, 10),
    (200, 2, 'Cơm mềm đậu lăng thịt bò cà chua', 'Thịt bò', 'Đậu lăng', 'Cà chua', 290, 18, 38, 6);

INSERT INTO "food_library" ("id", "function_code", "name", "advance_for", "created_at", "updated_at", "created_by", "updated_by", "status")
SELECT
    r.food_id,
    r.function_code,
    r.name,
    CASE
        WHEN r.food_id BETWEEN 1 AND 20 THEN 'FOR_MOTHER_DIET'
        WHEN r.food_id BETWEEN 21 AND 40 THEN 'FOR_MOTHER_CHANGE_DIET'
        WHEN r.food_id BETWEEN 41 AND 60 THEN 'FOR_MOTHER_POSTPARTUM_BREASTFEEDING'
        WHEN r.food_id BETWEEN 61 AND 80 THEN 'FOR_MOTHER_HEALTHY_ENERGY'
        WHEN r.food_id BETWEEN 81 AND 100 THEN 'FOR_MOTHER_DIGESTION_RECOVERY'
        WHEN r.food_id BETWEEN 101 AND 125 THEN 'FOR_BABY_6_8_MONTHS'
        WHEN r.food_id BETWEEN 126 AND 150 THEN 'FOR_BABY_9_11_MONTHS'
        WHEN r.food_id BETWEEN 151 AND 175 THEN 'FOR_BABY_12_18_MONTHS'
        ELSE 'FOR_BABY_19_24_MONTHS'
    END,
    now(), now(), 'system', 'system', 2
FROM seed_food_recipe r
WHERE NOT EXISTS (
    SELECT 1
    FROM "food_library" fl
    WHERE fl."id" = r.food_id OR fl."name" = r.name
);

SELECT setval(
    pg_get_serial_sequence('"food_library"', 'id'),
    GREATEST((SELECT COALESCE(MAX("id"), 1) FROM "food_library"), 1)
);

INSERT INTO "food_library_ingredients" ("food_lib_id", "food_ing_id", "amount_per_serving", "unit", "status", "description")
SELECT r.food_id, fi."id", ing.amount, 'g', 2, ing.description
FROM seed_food_recipe r
JOIN "food_library" fl ON fl."id" = r.food_id
CROSS JOIN LATERAL (
    VALUES
        (r.protein, CASE WHEN r.function_code = 2 THEN 35::double precision ELSE 120::double precision END, 'Nguồn đạm chính của món.'),
        (r.carb, CASE WHEN r.function_code = 2 THEN 45::double precision ELSE 90::double precision END, 'Nguồn tinh bột hoặc nền năng lượng của món.'),
        (r.veg, CASE WHEN r.function_code = 2 THEN 30::double precision ELSE 80::double precision END, 'Rau, trái cây hoặc thành phần bổ sung vi chất/chất xơ.')
) AS ing(name, amount, description)
JOIN "food_ingredients" fi ON fi."name_ingredients" = ing.name
WHERE NOT EXISTS (
    SELECT 1
    FROM "food_library_ingredients" fli
    WHERE fli."food_lib_id" = r.food_id AND fli."food_ing_id" = fi."id"
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
FROM seed_food_recipe r
JOIN "food_library" fl ON fl."id" = r.food_id
LEFT JOIN "food_library_ingredients" fli ON fli."food_lib_id" = r.food_id
LEFT JOIN "ingredient_nutrition" inn ON inn."food_ingredient_id" = fli."food_ing_id"
WHERE NOT EXISTS (
    SELECT 1 FROM "food_nutrition_summary" fn WHERE fn."food_id" = r.food_id
)
GROUP BY r.food_id, r.total_calories, r.total_protein, r.total_carbs, r.total_fat;

INSERT INTO "food_recommendation" ("food_id", "good_points", "bad_points", "advice", "cooking_way", "status")
SELECT
    r.food_id,
    r.name || ' là món được xây từ ba phần rõ ràng gồm ' || r.protein || ', ' || r.carb || ' và ' || r.veg || ', nên người ăn dễ hiểu món này có gì và vì sao nó phù hợp với mục tiêu đang chọn. ' || r.protein || ' tạo vị chính cho món, giúp món không bị nhạt hoặc chỉ toàn tinh bột. ' || r.carb || ' làm phần nền để món có độ no, độ sánh hoặc độ chắc bữa. ' || r.veg || ' giúp món mềm hơn, có màu sắc hơn và đỡ ngán khi ăn. Nếu nấu đúng cách, món này có thể giữ vị tự nhiên của nguyên liệu, không cần dùng nhiều dầu, đường hoặc sốt béo. Với mã mục tiêu ' || fl."advance_for" || ', điểm mạnh lớn nhất của món là có thể điều chỉnh độ mềm, lượng ăn và cách nêm theo từng người.' AS good_points,
    CASE
        WHEN r.function_code = 2 THEN
            'Dù món này có thể dùng cho bé, vẫn cần chú ý kỹ vì hệ tiêu hóa và kỹ năng nhai nuốt của bé còn đang phát triển. ' || r.protein || ' nếu còn xương, vỏ, gân, da dai hoặc bị nấu khô có thể làm bé khó nuốt. ' || r.carb || ' nếu chưa nấu mềm sẽ dễ làm món bị lợn cợn, bé ọe hoặc nuốt kém. ' || r.veg || ' cần bỏ vỏ, hạt, xơ cứng và không để thành miếng tròn hoặc cứng. Không dùng món này nếu bé từng dị ứng với ' || r.protein || ', ' || r.carb || ' hoặc ' || r.veg || '. Với bé dưới 12 tháng, không thêm mật ong, hạn chế muối đường và luôn cho ăn khi có người lớn quan sát.'
        ELSE
            'Món này vẫn có thể trở nên kém phù hợp nếu chế biến quá nhiều dầu, nêm quá mặn hoặc tăng quá nhiều phần ' || r.carb || '. ' || r.protein || ' nếu nấu quá lâu có thể khô, dai hoặc mất vị ngọt tự nhiên. ' || r.veg || ' nếu nấu quá kỹ có thể mềm nhũn, kém màu và làm món mất cảm giác tươi. Người dị ứng với ' || r.protein || ', ' || r.carb || ' hoặc ' || r.veg || ' nên tránh. Nếu mẹ đang kiểm soát cân nặng, đường huyết, mỡ máu, đang sau sinh có bệnh nền hoặc đang kiêng theo chỉ định thì nên giảm dầu, giảm sốt béo và hỏi nhân viên y tế khi cần.'
    END AS bad_points,
    CASE
        WHEN fl."advance_for" = 'FOR_MOTHER_DIET' THEN 'Gợi ý dùng: phù hợp bữa trưa hoặc tối, ưu tiên khẩu phần vừa, nêm nhạt và không dùng kèm sốt ngọt nhiều dầu.'
        WHEN fl."advance_for" = 'FOR_MOTHER_CHANGE_DIET' THEN 'Gợi ý dùng: dùng như món đổi vị trong tuần, thay đổi gia vị nhẹ như gừng, hành, chanh hoặc rau thơm để bữa ăn đỡ lặp lại.'
        WHEN fl."advance_for" = 'FOR_MOTHER_POSTPARTUM_BREASTFEEDING' THEN 'Gợi ý dùng: ăn khi còn ấm, chia khẩu phần nhỏ nếu mẹ mệt hoặc đầy bụng, uống nước theo nhu cầu và không ép ăn quá nhiều một món.'
        WHEN fl."advance_for" = 'FOR_MOTHER_HEALTHY_ENERGY' THEN 'Gợi ý dùng: hợp ngày mẹ cần bữa chắc bụng; nếu đang giảm cân thì giảm bớt phần ' || r.carb || ' và tăng phần rau mềm.'
        WHEN fl."advance_for" = 'FOR_MOTHER_DIGESTION_RECOVERY' THEN 'Gợi ý dùng: nấu mềm, ăn chậm, dùng khẩu phần nhỏ và hạn chế chiên xào để dễ tiêu hơn.'
        WHEN fl."advance_for" LIKE 'FOR_BABY_6_8%' THEN 'Gợi ý dùng: cho bé thử vào ban ngày, bắt đầu vài thìa nhỏ, xay mịn và chỉ tăng lượng khi bé dung nạp tốt.'
        WHEN fl."advance_for" LIKE 'FOR_BABY_9_11%' THEN 'Gợi ý dùng: nghiền thô từ từ theo khả năng của bé; nếu bé ọe nhiều thì quay lại kết cấu mềm hơn.'
        WHEN fl."advance_for" LIKE 'FOR_BABY_12_18%' THEN 'Gợi ý dùng: làm dạng cơm nát hoặc món mềm cắt nhỏ, có thể cho bé tập tự xúc nhưng cần người lớn ngồi cạnh.'
        ELSE 'Gợi ý dùng: cho bé ăn cùng giờ bữa gia đình nhưng tách riêng phần ít gia vị, cắt nhỏ thức ăn tròn/cứng và theo dõi khi có nguyên liệu mới.'
    END AS advice,
    CASE
        WHEN r.function_code = 2 THEN
            'Bước 1: Rửa sạch từng nguyên liệu và để riêng để kiểm soát độ chín. Bước 2: Nấu ' || r.carb || ' trước đến khi thật mềm; nếu là cháo hoặc bột thì khuấy đều để không vón, nếu là nui, mì, khoai hoặc đậu thì nấu đến khi bóp nhẹ đã nát. Bước 3: Làm chín kỹ ' || r.protein || ' bằng hấp, luộc hoặc hầm; cá và tôm phải bỏ xương, vỏ, chỉ lưng, thịt phải bỏ gân và phần dai, trứng phải chín hoàn toàn. Bước 4: Hấp hoặc luộc mềm ' || r.veg || ', bỏ vỏ, hạt và xơ cứng rồi băm hoặc nghiền nhỏ. Bước 5: Trộn ' || r.protein || ', ' || r.carb || ' và ' || r.veg || ' với nước nấu hoặc nước dùng nhạt để đạt độ mềm phù hợp. Bước 6: Với bé 6-8 tháng thì xay mịn và hơi loãng; bé 9-11 tháng có thể nghiền thô; bé trên 12 tháng có thể làm cơm nát hoặc cắt hạt lựu rất mềm. Bước 7: Trước khi cho bé ăn, để món còn ấm, khuấy đều và kiểm tra lại không còn cục cứng hoặc miếng dai.'
        ELSE
            'Bước 1: Sơ chế ' || r.protein || ' thật sạch, bỏ xương, vỏ, mỡ thừa hoặc phần dai nếu có; có thể ướp rất nhạt với gừng, hành hoặc tiêu. Bước 2: Chuẩn bị ' || r.carb || ' riêng; nếu là gạo, đậu hoặc hạt thì nên ngâm và nấu mềm, nếu là bún, nui hoặc mì thì luộc mềm rồi để ráo. Bước 3: Rửa ' || r.veg || ', cắt vừa ăn và để riêng để cho vào đúng lúc. Bước 4: Làm chín ' || r.protein || ' bằng hấp, luộc, hầm hoặc áp chảo ít dầu ở lửa vừa; không nên chiên ngập dầu vì món sẽ nặng bụng. Bước 5: Khi ' || r.protein || ' gần chín, cho ' || r.carb || ' và ' || r.veg || ' vào phối hợp, thêm nước dùng nhạt nếu món bị khô. Bước 6: Nêm nhẹ, nấu thêm vài phút cho vị hòa vào nhau. Bước 7: Dọn món khi còn ấm; nếu món có sữa chua, phô mai hoặc trái cây mềm thì cho phần đó vào cuối cùng, không đun lâu để giữ vị tươi.'
    END AS cooking_way,
    2 AS status
FROM seed_food_recipe r
JOIN "food_library" fl ON fl."id" = r.food_id
WHERE NOT EXISTS (
    SELECT 1 FROM "food_recommendation" fr WHERE fr."food_id" = r.food_id
);
