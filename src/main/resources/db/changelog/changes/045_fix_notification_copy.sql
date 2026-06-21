-- liquibase formatted sql

-- changeset codex:045-fix-routine-notification-copy
UPDATE notification
SET
    title = replace(
        replace(title, 'Sap den lich sinh hoat cua be ', 'Sắp đến lịch sinh hoạt của bé '),
        'Sắp đến lịch sinh hoạt của bé bé', 'Sắp đến lịch sinh hoạt của bé'
    ),
    body = replace(
        replace(
            replace(
                replace(
                    replace(body, 'Be ', 'Bé '),
                    ' co lich: ', ' có lịch: '
                ),
                ' luc ', ' lúc '
            ),
            '. Me chuan bi truoc nhe.', '. Mẹ chuẩn bị trước nhé.'
        ),
        'Bé bé', 'Bé'
    )
WHERE type = 'ROUTINE_REMINDER';

-- changeset codex:045-fix-known-notification-copy
UPDATE notification
SET
    title = CASE
        WHEN title = 'Chao mung den voi BabyOi' THEN 'Chào mừng đến với BabyOi'
        ELSE replace(replace(title, 'của bé bé', 'của bé'), 'của mẹ mẹ', 'của mẹ')
    END,
    body = CASE
        WHEN body = 'Me co the theo doi lich tiem, dinh duong va cac thong bao quan trong tai day.'
            THEN 'Mẹ có thể theo dõi lịch tiêm, dinh dưỡng và các thông báo quan trọng tại đây.'
        ELSE replace(
            replace(
                replace(body, 'Bé bé', 'Bé'),
                'Mẹ mẹ', 'Mẹ'
            ),
            'kcal/ngay', 'kcal/ngày'
        )
    END
WHERE title = 'Chao mung den voi BabyOi'
   OR body = 'Me co the theo doi lich tiem, dinh duong va cac thong bao quan trong tai day.'
   OR title LIKE '%của bé bé%'
   OR title LIKE '%của mẹ mẹ%'
   OR body LIKE '%Bé bé%'
   OR body LIKE '%Mẹ mẹ%'
   OR body LIKE '%kcal/ngay%';
