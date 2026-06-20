-- liquibase formatted sql

-- changeset codex:040-fix-mother-health-record-notifications
UPDATE notification n
SET
    body = 'Mẹ ' || p.name || ' vừa được cập nhật '
        || COALESCE(
            NULLIF(concat_ws(', ',
                CASE WHEN hr.weight IS NOT NULL THEN 'cân nặng ' || trim(to_char(hr.weight, 'FM999999990.0')) || 'kg' END,
                CASE WHEN hr.height IS NOT NULL THEN 'chiều cao ' || trim(to_char(hr.height, 'FM999999990.0')) || 'cm' END,
                CASE WHEN hr.bmi IS NOT NULL THEN 'BMI ' || trim(to_char(hr.bmi, 'FM999999990.0')) END,
                CASE WHEN hr.tdee IS NOT NULL THEN 'TDEE ' || trim(to_char(hr.tdee, 'FM999999990')) || ' kcal/ngay' END
            ), ''),
            'hồ sơ sức khỏe'
        )
        || ' ngày ' || hr.record_date || '.'
FROM health_record hr
JOIN profile p ON p.id = hr.profile_id
WHERE n.source_type = 'HEALTH_RECORD'
  AND n.source_id = hr.id
  AND UPPER(COALESCE(p.profile_type, '')) = 'MOTHER';

-- changeset codex:040-fix-mother-illness-notifications
UPDATE notification n
SET
    body = 'Mẹ ' || p.name || ' có ghi nhận ' || ie.illness_type
        || ' từ ' || ie.start_at
        || ', mức độ: '
        || CASE COALESCE(ie.status, 2)
            WHEN 1 THEN 'Nhẹ'
            WHEN 3 THEN 'Cần chú ý'
            ELSE 'Bình thường'
        END
        || '.'
FROM illness_event ie
JOIN profile p ON p.id = ie.profile_id
WHERE n.source_type = 'ILLNESS_EVENT'
  AND n.source_id = ie.id
  AND UPPER(COALESCE(p.profile_type, '')) = 'MOTHER';

-- changeset codex:040-fix-mother-vaccine-notifications
WITH resolved AS (
    SELECT
        n.id AS notification_id,
        vr.id AS record_id,
        vr.injection_date,
        vr.dose_order,
        p.name AS profile_name,
        d.name AS disease_name,
        v.name AS vaccine_name
    FROM notification n
    JOIN vaccine_record vr ON vr.id = n.source_id
    JOIN profile p ON p.id = vr.profile_id
    LEFT JOIN child_vaccine_diseases d ON d.id = vr.disease_id
    LEFT JOIN vaccines v ON v.id = vr.vaccine_id
    WHERE n.type = 'VACCINE_REMINDER'
      AND UPPER(COALESCE(p.profile_type, '')) = 'MOTHER'
)
UPDATE notification n
SET
    title = CASE
        WHEN r.injection_date = CURRENT_DATE THEN 'Lịch tiêm hôm nay của mẹ ' || r.profile_name
        WHEN r.injection_date IS NOT NULL THEN 'Lịch tiêm của mẹ ' || r.profile_name
        ELSE 'Nhắc lịch tiêm của mẹ ' || r.profile_name
    END,
    body = CASE
        WHEN r.injection_date = CURRENT_DATE THEN 'Hôm nay mẹ ' || r.profile_name || ' tiêm '
        ELSE 'Mẹ ' || r.profile_name || ' sẽ tiêm '
    END
        || COALESCE(
            CASE
                WHEN r.disease_name IS NOT NULL AND r.vaccine_name IS NOT NULL AND lower(r.disease_name) <> lower(r.vaccine_name)
                THEN r.disease_name || ' (' || r.vaccine_name || ')'
            END,
            r.disease_name,
            r.vaccine_name,
            'vaccine theo lịch'
        )
        || ', ' || CASE WHEN r.dose_order IS NULL THEN 'mũi theo lịch' ELSE 'mũi ' || r.dose_order END
        || CASE
            WHEN r.injection_date = CURRENT_DATE THEN '. Mẹ nhớ kiểm tra giấy tờ và giờ hẹn nhé.'
            WHEN r.injection_date IS NOT NULL THEN ' vào ' || to_char(r.injection_date, 'DD/MM/YYYY') || '. Mẹ có thể sắp xếp thời gian từ bây giờ.'
            ELSE '.'
        END
FROM resolved r
WHERE n.id = r.notification_id;
