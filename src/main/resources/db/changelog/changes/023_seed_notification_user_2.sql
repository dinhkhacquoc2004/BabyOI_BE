-- liquibase formatted sql

-- changeset codex:073
-- validCheckSum: ANY
INSERT INTO "notification" (
    "user_id",
    "type",
    "title",
    "body",
    "data_json",
    "priority",
    "source_type",
    "source_id",
    "status",
    "created_at"
)
SELECT
    2::BIGINT,
    seed."type",
    seed."title",
    seed."body",
    seed."data_json",
    seed."priority",
    seed."source_type",
    seed."source_id",
    seed."status",
    CURRENT_TIMESTAMP
FROM (
    VALUES
        (
            'SYSTEM',
            'Chao mung den voi BabyOi',
            'Me co the theo doi lich tiem, dinh duong va cac thong bao quan trong tai day.',
            '{"screen":"Home"}',
            2::BIGINT,
            'SYSTEM',
            NULL::BIGINT,
            -1::BIGINT
        ),
        (
            'VACCINE_REMINDER',
            'Lịch tiêm hôm nay',
            'Bé trong hồ sơ ID 2 có lịch tiêm hôm nay: mũi vaccine cần kiểm tra trong sổ tiêm.',
            '{"screen":"VaccineRecordDetail","profileId":2}',
            3::BIGINT,
            'VACCINE_RECORD',
            NULL::BIGINT,
            -1::BIGINT
        )
) AS seed (
    "type",
    "title",
    "body",
    "data_json",
    "priority",
    "source_type",
    "source_id",
    "status"
)
WHERE EXISTS (
    SELECT 1 FROM "users" WHERE "id" = 2
);
