-- liquibase formatted sql

-- changeset codex:091
-- validCheckSum: ANY
UPDATE "vaccine_type"
SET
    "name" = seed."name",
    "description" = seed."description",
    "required_age" = seed."required_age",
    "updated_at" = CURRENT_DATE,
    "updated_by" = 1
FROM (
    VALUES
        (1::BIGINT, 'BCG', 'Vaccine phòng bệnh Lao', NULL),
        (2::BIGINT, 'Viêm gan B', 'Vaccine phòng bệnh Viêm gan B', NULL),
        (3::BIGINT, '6 trong 1 (Infanrix hexa)', 'Phòng Bạch hầu, Ho gà, Uốn ván, Bại liệt, Viêm gan B, Hib', NULL),
        (4::BIGINT, 'Cúm', 'Vaccine phòng bệnh Cúm mùa', NULL),
        (5::BIGINT, 'Sởi - Quai bị - Rubella', 'Vaccine MMR II', NULL),
        (6::BIGINT, 'Sởi', 'Vắc xin phòng bệnh sởi', '9 tháng'),
        (7::BIGINT, 'Quai bị', 'Vắc xin phòng bệnh quai bị', '12 tháng'),
        (8::BIGINT, 'Rubella', 'Vắc xin phòng bệnh Rubella', '12 tháng'),
        (9::BIGINT, 'Thủy đậu', 'Vắc xin phòng bệnh thủy đậu', '12 tháng'),
        (10::BIGINT, 'Viêm não Nhật Bản', 'Phòng bệnh viêm não Nhật Bản', '12 tháng'),
        (11::BIGINT, 'Cúm', 'Vắc xin phòng cúm mùa', '6 tháng'),
        (12::BIGINT, 'Viêm gan A', 'Vắc xin phòng viêm gan A', '12 tháng'),
        (13::BIGINT, 'Thương hàn', 'Vắc xin phòng bệnh thương hàn', '24 tháng'),
        (14::BIGINT, 'Tả', 'Vắc xin phòng bệnh tả (uống)', '24 tháng'),
        (15::BIGINT, 'Dại', 'Vắc xin phòng bệnh dại', 'Mọi lứa tuổi')
) AS seed("id", "name", "description", "required_age")
WHERE "vaccine_type"."id" = seed."id";

-- changeset codex:092
-- validCheckSum: ANY
UPDATE "vaccination_center"
SET
    "name" = seed."name",
    "address" = seed."address",
    "updated_at" = CURRENT_TIMESTAMP
FROM (
    VALUES
        (1::BIGINT, 'VNVC Trường Chinh', '180 Trường Chinh, Đống Đa, Hà Nội'),
        (2::BIGINT, 'Trung tâm Y tế Dự phòng Hà Nội', '70 Nguyễn Chí Thanh, Đống Đa, Hà Nội'),
        (3::BIGINT, 'Bệnh viện Nhi Trung ương', '18/879 La Thành, Đống Đa, Hà Nội')
) AS seed("id", "name", "address")
WHERE "vaccination_center"."id" = seed."id";

-- changeset codex:093
-- validCheckSum: ANY
UPDATE "vaccine_pricing"
SET
    "note" = seed."note",
    "updated_at" = CURRENT_TIMESTAMP
FROM (
    VALUES
        (1::BIGINT, 'Giá ưu đãi gói tiêm'),
        (2::BIGINT, 'Giá niêm yết'),
        (3::BIGINT, 'Cúm mùa mới nhất'),
        (4::BIGINT, NULL)
) AS seed("id", "note")
WHERE "vaccine_pricing"."id" = seed."id";

-- changeset codex:094
-- validCheckSum: ANY
UPDATE "vaccine_record"
SET
    "location" = seed."location",
    "note" = seed."note",
    "updated_at" = CURRENT_DATE,
    "updated_by" = 1
FROM (
    VALUES
        (1::BIGINT, 'Bệnh viện Phụ sản TW', 'Tiêm ngay sau sinh (Miễn phí)'),
        (2::BIGINT, 'Bệnh viện Phụ sản TW', 'Mũi 1 sơ sinh'),
        (3::BIGINT, 'VNVC Trường Chinh', 'Mũi 2 trễ 5 ngày'),
        (4::BIGINT, 'VNVC Trường Chinh', 'Mũi 1 - 6 trong 1'),
        (5::BIGINT, 'Trung tâm y tế dự phòng', 'Mũi 1'),
        (6::BIGINT, 'VNVC Hà Nội', 'Tiêm dịch vụ'),
        (7::BIGINT, 'VNVC Hà Nội', 'Kết hợp MMR'),
        (8::BIGINT, 'Bệnh viện Nhi Trung ương', 'Mũi 1'),
        (9::BIGINT, 'Trạm y tế xã', 'Tiêm chủng mở rộng'),
        (10::BIGINT, 'Phòng khám đa khoa', 'Cúm mùa hằng năm'),
        (11::BIGINT, 'VNVC', 'Mũi nhắc lại'),
        (12::BIGINT, 'Trung tâm y tế quận', 'Định kỳ'),
        (13::BIGINT, 'Trạm y tế', 'Uống liều 1'),
        (14::BIGINT, 'Viện Vệ sinh Dịch tễ', 'Tiêm phòng sau phơi nhiễm'),
        (16::BIGINT, 'Trung tâm y tế Q1', 'Mũi 1 - Đã tiêm'),
        (17::BIGINT, 'VNVC', 'Hủy do bé sốt'),
        (18::BIGINT, 'Bệnh viện Nhi', 'Mũi nhắc lại'),
        (19::BIGINT, 'Trạm y tế phường', 'Lịch tiêm chủng mở rộng'),
        (20::BIGINT, 'Phòng khám đa khoa', 'Gia đình bận, không đi được'),
        (21::BIGINT, 'VNVC Hà Nội', 'Tiêm cúm mùa'),
        (22::BIGINT, 'Trung tâm tiêm chủng', 'Hủy lịch hẹn'),
        (23::BIGINT, 'Bệnh viện Từ Dũ', 'Đã hoàn thành'),
        (24::BIGINT, 'Trạm y tế', 'Uống liều 1'),
        (25::BIGINT, 'Viện Pasteur', 'Hết thuốc tại điểm tiêm')
) AS seed("id", "location", "note")
WHERE "vaccine_record"."id" = seed."id";
