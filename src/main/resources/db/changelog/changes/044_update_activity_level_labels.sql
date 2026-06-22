-- liquibase formatted sql

-- changeset codex:044-update-activity-level-labels
UPDATE "type_value" tv
SET
    "value_name" = seed."value_name",
    "description" = seed."description",
    "updated_at" = CURRENT_TIMESTAMP,
    "updated_by" = 'SYSTEM'
FROM (
    VALUES
        ('SEDENTARY', 'Không tập luyện', 'Chủ yếu ngồi, chỉ thực hiện các hoạt động sinh hoạt hằng ngày.'),
        ('LIGHT', 'Tập nhẹ 1-3 buổi/tuần', 'Đi bộ, yoga hoặc vận động nhẹ từ 1 đến 3 buổi mỗi tuần.'),
        ('MODERATE', 'Tập vừa 3-5 buổi/tuần', 'Tập luyện cường độ vừa từ 3 đến 5 buổi mỗi tuần.'),
        ('ACTIVE', 'Tập nặng 6-7 buổi/tuần', 'Tập luyện cường độ cao từ 6 đến 7 buổi mỗi tuần.'),
        ('VERY_ACTIVE', 'Rất nặng mỗi ngày', 'Tập nặng mỗi ngày, có thể 2 buổi mỗi ngày, hoặc lao động thể lực.')
) AS seed ("value_code", "value_name", "description"),
     "type_code" tc
WHERE tc."id" = tv."type_code_id"
  AND tc."code" = 'ACTIVITY_LEVEL'
  AND tv."value_code" = seed."value_code";
