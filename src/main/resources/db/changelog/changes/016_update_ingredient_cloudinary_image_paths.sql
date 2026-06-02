-- liquibase formatted sql

-- changeset codex:045
WITH ingredient_image_seed(id, image_url) AS (
    VALUES
        (1, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766856/Ingredient/Ingredient_1.jpg'),
        (2, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766857/Ingredient/Ingredient_2.jpg'),
        (3, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766858/Ingredient/Ingredient_3.jpg'),
        (4, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766859/Ingredient/Ingredient_4.jpg'),
        (5, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766860/Ingredient/Ingredient_5.jpg'),
        (6, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766861/Ingredient/Ingredient_6.jpg'),
        (7, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766861/Ingredient/Ingredient_7.webp'),
        (8, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766862/Ingredient/Ingredient_8.jpg'),
        (9, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766863/Ingredient/Ingredient_9.jpg'),
        (10, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766864/Ingredient/Ingredient_10.jpg'),
        (11, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766865/Ingredient/ingredient_11.jpg'),
        (12, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766866/Ingredient/Ingredient_12.jpg'),
        (13, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766867/Ingredient/Ingredient_13.webp'),
        (14, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766868/Ingredient/Ingredient_14.jpg'),
        (15, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766885/Ingredient/Ingredient_15.png'),
        (16, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766887/Ingredient/Ingredient_16.png'),
        (17, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766892/Ingredient/Ingredient_17.webp'),
        (18, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766898/Ingredient/Ingredient_18.jpg'),
        (19, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766899/Ingredient/Ingredient_19.webp'),
        (20, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766923/Ingredient/Ingredient_20.png'),
        (21, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766936/Ingredient/Ingredient_21.png'),
        (22, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766954/Ingredient/Ingredient_22.png'),
        (23, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766963/Ingredient/Ingredient_23.png'),
        (24, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766970/Ingredient/Ingredient_24.png'),
        (25, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766971/Ingredient/Ingredient_25.jpg'),
        (26, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766972/Ingredient/Ingredient_26.jpg'),
        (27, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766976/Ingredient/Ingredient_27.jpg'),
        (28, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766978/Ingredient/Ingredient_28.jpg'),
        (29, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766979/Ingredient/Ingredient_29.jpg'),
        (30, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766980/Ingredient/Ingredient_30.jpg'),
        (31, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766982/Ingredient/Ingredient_31.webp'),
        (32, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766983/Ingredient/Ingredient_32.webp'),
        (33, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766985/Ingredient/Ingredient_33.jpg'),
        (34, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766987/Ingredient/Ingredient_34.jpg'),
        (35, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766988/Ingredient/Ingredient_35.jpg'),
        (36, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766990/Ingredient/Ingredient_36.jpg'),
        (37, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766991/Ingredient/Ingredient_37.jpg'),
        (38, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766994/Ingredient/Ingredient_38.webp'),
        (39, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779766996/Ingredient/Ingredient_39.jpg'),
        (40, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767015/Ingredient/Ingredient_40.webp'),
        (41, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767017/Ingredient/Ingredient_41.jpg'),
        (42, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767023/Ingredient/Ingredient_42.jpg'),
        (43, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767025/Ingredient/Ingredient_43.jpg'),
        (44, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767027/Ingredient/Ingredient_44.jpg'),
        (46, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767029/Ingredient/Ingredient_46.jpg'),
        (47, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767031/Ingredient/Ingredient_47.jpg'),
        (48, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767032/Ingredient/Ingredient_48.jpg'),
        (49, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767033/Ingredient/Ingredient_49.webp'),
        (50, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767035/Ingredient/Ingredient_50.jpg'),
        (51, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767038/Ingredient/Ingredient_51.jpg'),
        (52, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767039/Ingredient/Ingredient_52.jpg'),
        (53, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767041/Ingredient/Ingredient_53.jpg'),
        (54, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767042/Ingredient/Ingredient_54.jpg'),
        (55, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767047/Ingredient/Ingredient_55.jpg'),
        (56, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767048/Ingredient/Ingredient_56.webp'),
        (57, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767049/Ingredient/Ingredient_57.jpg'),
        (58, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767050/Ingredient/Ingredient_58.jpg'),
        (59, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767052/Ingredient/Ingredient_59.jpg'),
        (60, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767053/Ingredient/Ingredient_60.jpg'),
        (61, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767056/Ingredient/Ingredient_61.jpg'),
        (62, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767064/Ingredient/Ingredient_62.jpg'),
        (63, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767068/Ingredient/Ingredient_63.jpg'),
        (64, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767069/Ingredient/Ingredient_64.webp'),
        (65, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767071/Ingredient/Ingredient_65.jpg'),
        (66, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767075/Ingredient/Ingredient_66.webp'),
        (67, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767086/Ingredient/Ingredient_67.jpg'),
        (68, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767087/Ingredient/Ingredient_68.jpg'),
        (69, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767089/Ingredient/Ingredient_69.jpg'),
        (70, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767101/Ingredient/Ingredient_70.jpg'),
        (71, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767104/Ingredient/Ingredient_71.webp'),
        (72, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767106/Ingredient/Ingredient_72.jpg'),
        (73, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767108/Ingredient/Ingredient_73.jpg'),
        (74, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767112/Ingredient/Ingredient_74.jpg'),
        (75, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767113/Ingredient/Ingredient_75.jpg'),
        (76, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767116/Ingredient/Ingredient_76.webp'),
        (77, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767119/Ingredient/Ingredient_77.webp'),
        (78, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767124/Ingredient/Ingredient_78.jpg'),
        (79, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767132/Ingredient/Ingredient_79.jpg'),
        (80, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767139/Ingredient/Ingredient_80.webp'),
        (81, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767140/Ingredient/Ingredient_81.jpg'),
        (82, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767142/Ingredient/Ingredient_82.webp'),
        (83, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767143/Ingredient/Ingredient_83.jpg'),
        (84, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767144/Ingredient/Ingredient_84.jpg'),
        (85, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767145/Ingredient/Ingredient_85.png'),
        (86, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767146/Ingredient/Ingredient_86.png'),
        (87, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767147/Ingredient/Ingredient_87.jpg'),
        (88, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767147/Ingredient/Ingredient_88.jpg'),
        (89, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767149/Ingredient/Ingredient_89.webp'),
        (90, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767150/Ingredient/Ingredient_90.jpg'),
        (91, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767151/Ingredient/Ingredient_91.jpg'),
        (92, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767152/Ingredient/Ingredient_92.jpg'),
        (93, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767153/Ingredient/Ingredient_93.jpg'),
        (94, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767157/Ingredient/Ingredient_94.jpg'),
        (95, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767158/Ingredient/Ingredient_95.jpg'),
        (96, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767159/Ingredient/Ingredient_96.jpg'),
        (97, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767160/Ingredient/Ingredient_97.jpg'),
        (98, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767161/Ingredient/Ingredient_98.jpg'),
        (99, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767163/Ingredient/Ingredient_99.jpg'),
        (100, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767164/Ingredient/Ingredient_100.jpg'),
        (101, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767165/Ingredient/Ingredient_101.jpg'),
        (102, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767167/Ingredient/Ingredient_102.jpg'),
        (103, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767170/Ingredient/Ingredient_103.jpg'),
        (104, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767172/Ingredient/Ingredient_104.webp'),
        (105, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767174/Ingredient/Ingredient_105.jpg'),
        (106, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767177/Ingredient/Ingredient_106.jpg'),
        (107, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767180/Ingredient/Ingredient_107.jpg'),
        (108, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767182/Ingredient/Ingredient_108.webp'),
        (109, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767183/Ingredient/Ingredient_109.jpg'),
        (110, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767186/Ingredient/Ingredient_110.jpg'),
        (111, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767192/Ingredient/Ingredient_111.jpg'),
        (112, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767196/Ingredient/Ingredient_112.jpg'),
        (113, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767198/Ingredient/Ingredient_113.jpg'),
        (114, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767199/Ingredient/Ingredient_114.jpg'),
        (115, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767200/Ingredient/Ingredient_115.webp'),
        (116, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767202/Ingredient/Ingredient_116.webp'),
        (117, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767203/Ingredient/Ingredient_117.webp'),
        (118, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767205/Ingredient/Ingredient_118.jpg'),
        (119, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767206/Ingredient/Ingredient_119.png'),
        (120, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767208/Ingredient/Ingredient_120.jpg'),
        (121, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767209/Ingredient/Ingredient_121.jpg'),
        (122, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767210/Ingredient/Ingredient_122.jpg'),
        (123, 'https://res.cloudinary.com/dkerzsvan/image/upload/v1779767211/Ingredient/Ingredient_123.jpg')
)
UPDATE "food_ingredients" fi
SET "image_url" = s.image_url
FROM ingredient_image_seed s
WHERE fi."id" = s.id;

-- changeset codex:047
UPDATE "food_ingredients"
SET "image_url" = NULL,
    "updated_at" = now(),
    "updated_by" = 'system'
WHERE "image_url" LIKE '/Ingredient/%';

-- changeset codex:048
UPDATE "food_library"
SET "image_url" = NULL,
    "updated_at" = now(),
    "updated_by" = 'system'
WHERE "image_url" LIKE '/Food/%'
   OR "image_url" LIKE 'https://images.unsplash.com/%';

-- changeset codex:049
WITH extra_ingredient_image_seed(name, image_url) AS (
    VALUES
        ('Bá»™t nÄƒng', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_bot-nang.jpg'),
        ('Dáº§u Äƒn dáº·m', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_dau-an-dam.jpg'),
        ('Dáº§u mÃ¨', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_dau-me.jpg'),
        ('Dáº§u Ã´ liu', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_dau-o-liu.jpg'),
        ('Gá»«ng', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_gung.jpg'),
        ('HÃ nh lÃ¡', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_hanh-la.jpg'),
        ('HÃ nh tÃ­m', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_hanh-tim.jpg'),
        ('MÃ¨ rang', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_me-rang.jpg'),
        ('Muá»‘i i-á»‘t', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_muoi-iot.jpg'),
        ('NÆ°á»›c cá»‘t chanh', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_nuoc-cot-chanh.jpg'),
        ('NÆ°á»›c dÃ¹ng nháº¡t', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_nuoc-dung-nhat.jpg'),
        ('NÆ°á»›c lá»c', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_nuoc-loc.jpg'),
        ('NÆ°á»›c luá»™c rau cá»§', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_nuoc-luoc-rau-cu.jpg'),
        ('Rau mÃ¹i', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_rau-mui.jpg'),
        ('Sá»¯a tÆ°Æ¡i khÃ´ng Ä‘Æ°á»ng', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_sua-tuoi-khong-duong.jpg'),
        ('TiÃªu xay', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_tieu-xay.jpg'),
        ('Tá»i', 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_toi.jpg')
)
UPDATE "food_ingredients" fi
SET "image_url" = s.image_url,
    "updated_at" = now(),
    "updated_by" = 'system'
FROM extra_ingredient_image_seed s
WHERE fi."name_ingredients" = s.name
  AND (
      fi."image_url" IS NULL
      OR BTRIM(fi."image_url") = ''
      OR fi."image_url" LIKE '/Ingredient/%'
      OR fi."image_url" LIKE 'https://res.cloudinary.com/dkerzsvan/image/upload/%/Ingredient/Ingredient_%'
  );

-- changeset codex:050
WITH ingredient_image_by_id_seed(id, image_url) AS (
    VALUES
        (45, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_rong-bien.jpg'),
        (65, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_toi.jpg'),
        (66, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_gung.jpg'),
        (84, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_dau-o-liu.jpg'),
        (85, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_dau-me.jpg'),
        (90, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_hanh-la.jpg'),
        (119, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_sua-tuoi-khong-duong.jpg'),
        (123, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_nuoc-loc.jpg'),
        (124, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_hanh-tim.jpg'),
        (125, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_nuoc-dung-nhat.jpg'),
        (126, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_dau-an-dam.jpg'),
        (127, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_rau-mui.jpg'),
        (128, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_nuoc-cot-chanh.jpg'),
        (129, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_nuoc-luoc-rau-cu.jpg'),
        (130, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_me-rang.jpg'),
        (131, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_bot-nang.jpg'),
        (132, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_muoi-iot.jpg'),
        (133, 'https://res.cloudinary.com/dkerzsvan/image/upload/f_jpg,q_auto/Ingredient/ingredient_tieu-xay.jpg')
)
UPDATE "food_ingredients" fi
SET "image_url" = s.image_url,
    "updated_at" = now(),
    "updated_by" = 'system'
FROM ingredient_image_by_id_seed s
WHERE fi."id" = s.id
  AND (
      fi."image_url" IS NULL
      OR BTRIM(fi."image_url") = ''
      OR fi."image_url" LIKE '/Ingredient/%'
      OR fi."image_url" LIKE '%/f_auto,q_auto/%'
  );
