-- liquibase formatted sql

-- changeset codex:022
UPDATE "food_library"
SET "image_url" = '/Food/Food_' || "id" || '.jpg'
WHERE "image_url" IS NULL
   OR "image_url" = ''
   OR "image_url" LIKE 'https://images.unsplash.com/%';

-- changeset codex:024
WITH ingredient_rank AS (
    SELECT
        fli."id",
        fl."function_code",
        fi."name_ingredients",
        fli."amount_per_serving",
        COALESCE(NULLIF(fli."unit", ''), 'g') AS "unit",
        ROW_NUMBER() OVER (PARTITION BY fli."food_lib_id" ORDER BY fli."id") AS ingredient_order
    FROM "food_library_ingredients" fli
    JOIN "food_library" fl ON fl."id" = fli."food_lib_id"
    JOIN "food_ingredients" fi ON fi."id" = fli."food_ing_id"
    WHERE fli."status" <> -4 OR fli."status" IS NULL
)
UPDATE "food_library_ingredients" fli
SET "description" = CASE
    WHEN ir.ingredient_order = 1 THEN
        ir."name_ingredients" || ' là phần đạm chính của món, cần được sơ chế sạch, nấu chín kỹ nhưng không quá lâu để giữ độ mềm. Lượng dùng khoảng ' ||
        TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM ir."amount_per_serving"::TEXT)) || ' ' || ir."unit" ||
        CASE WHEN ir."function_code" = 2
            THEN ' nên được xé nhỏ, băm nhuyễn hoặc nghiền theo độ tuổi của bé; luôn kiểm tra xương, vỏ, da dai, gân hoặc miếng cứng trước khi cho ăn.'
            ELSE ' giúp món có vị chính rõ ràng; nên hạn chế chiên ngập dầu và ưu tiên hấp, luộc, hầm hoặc áp chảo ít dầu để món nhẹ hơn.'
        END
    WHEN ir.ingredient_order = 2 THEN
        ir."name_ingredients" || ' là phần nền năng lượng của món, tạo độ no, độ sánh hoặc độ mềm khi ăn. Lượng dùng khoảng ' ||
        TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM ir."amount_per_serving"::TEXT)) || ' ' || ir."unit" ||
        CASE WHEN ir."function_code" = 2
            THEN ' cần được nấu thật mềm; với bé nhỏ nên xay, nghiền hoặc cắt rất nhỏ để tránh vón cục và giúp bé nuốt dễ hơn.'
            ELSE ' có thể tăng giảm theo mức đói và mục tiêu ăn uống; nếu muốn bữa nhẹ hơn thì giảm phần này và tăng rau mềm.'
        END
    ELSE
        ir."name_ingredients" || ' là phần rau, củ, nấm hoặc trái cây giúp món có màu, mùi và vị tự nhiên hơn. Lượng dùng khoảng ' ||
        TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM ir."amount_per_serving"::TEXT)) || ' ' || ir."unit" ||
        CASE WHEN ir."function_code" = 2
            THEN ' cần rửa kỹ, bỏ vỏ, hạt, xơ cứng nếu có và nấu mềm trước khi trộn vào món; tránh để miếng tròn, dai hoặc cứng.'
            ELSE ' nên cho vào đúng thời điểm để giữ độ mềm vừa phải, tránh nấu quá lâu làm mất màu và làm món bị nhũn.'
        END
END
FROM ingredient_rank ir
WHERE fli."id" = ir."id";
