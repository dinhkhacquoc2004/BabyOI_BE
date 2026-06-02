CREATE TABLE IF NOT EXISTS ai_lesson_progress (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    progress_status BIGINT NOT NULL,
    started_at DATE,
    completed_at DATE,
    current_day INTEGER,
    practice_count INTEGER,
    last_practiced_at DATE,
    note TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    status BIGINT,
    CONSTRAINT fk_ai_lesson_progress_profile FOREIGN KEY (profile_id) REFERENCES profile (id),
    CONSTRAINT fk_ai_lesson_progress_lesson FOREIGN KEY (lesson_id) REFERENCES ai_teaching_lesson (id)
);

ALTER TABLE ai_lesson_progress ADD COLUMN IF NOT EXISTS profile_id BIGINT;
ALTER TABLE ai_lesson_progress ADD COLUMN IF NOT EXISTS lesson_id BIGINT;
ALTER TABLE ai_lesson_progress ADD COLUMN IF NOT EXISTS progress_status BIGINT;
ALTER TABLE ai_lesson_progress ADD COLUMN IF NOT EXISTS started_at DATE;
ALTER TABLE ai_lesson_progress ADD COLUMN IF NOT EXISTS completed_at DATE;
ALTER TABLE ai_lesson_progress ADD COLUMN IF NOT EXISTS current_day INTEGER;
ALTER TABLE ai_lesson_progress ADD COLUMN IF NOT EXISTS practice_count INTEGER;
ALTER TABLE ai_lesson_progress ADD COLUMN IF NOT EXISTS last_practiced_at DATE;
ALTER TABLE ai_lesson_progress ADD COLUMN IF NOT EXISTS note TEXT;
ALTER TABLE ai_lesson_progress ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE ai_lesson_progress ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE ai_lesson_progress ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE ai_lesson_progress ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE ai_lesson_progress ADD COLUMN IF NOT EXISTS status BIGINT;

CREATE INDEX IF NOT EXISTS idx_ai_lesson_progress_profile_status
    ON ai_lesson_progress (profile_id, status);

CREATE INDEX IF NOT EXISTS idx_ai_lesson_progress_profile_lesson
    ON ai_lesson_progress (profile_id, lesson_id);

INSERT INTO ai_lesson_progress (
    profile_id,
    lesson_id,
    progress_status,
    started_at,
    completed_at,
    current_day,
    practice_count,
    last_practiced_at,
    note,
    created_at,
    updated_at,
    created_by,
    updated_by,
    status
)
WITH selected_profiles AS (
    SELECT
        p.id,
        ROW_NUMBER() OVER (
            ORDER BY
                CASE WHEN UPPER(COALESCE(p.profile_type, '')) = 'CHILD' THEN 0 ELSE 1 END,
                p.id
        ) AS profile_no
    FROM profile p
    WHERE p.status = 2
    ORDER BY
        CASE WHEN UPPER(COALESCE(p.profile_type, '')) = 'CHILD' THEN 0 ELSE 1 END,
        p.id
    LIMIT 2
),
seed_progress AS (
    SELECT *
    FROM (
        VALUES
            (1, 1, 3, 14, 14, 'Bé hợp tác tốt, đã quen nằm sấp mỗi ngày.'),
            (1, 2, 3, 10, 12, 'Phản hồi âm thanh và ánh mắt tốt.'),
            (1, 3, 2, 4, 5, 'Đang tập trung nhìn tranh tốt hơn, nên duy trì thời lượng ngắn.'),
            (1, 4, 1, 1, 0, 'Bài tiếp theo để theo dõi tập lẫy và cầm nắm.'),
            (2, 1, 3, 12, 10, 'Đã hoàn thành mốc nằm sấp cơ bản.'),
            (2, 5, 2, 6, 8, 'Bé thích ú òa, phản ứng tìm đồ chơi rõ hơn.'),
            (2, 6, 2, 3, 4, 'Đang làm quen bốc nhón với thức ăn mềm.'),
            (2, 14, 1, 1, 0, 'Chuẩn bị đánh giá sẵn sàng ăn dặm.')
    ) AS values_table(profile_no, lesson_sort_order, progress_status, current_day, practice_count, note)
)
SELECT
    sp.id,
    lesson.id,
    seed_progress.progress_status,
    CURRENT_DATE - seed_progress.current_day,
    CASE WHEN seed_progress.progress_status = 3 THEN CURRENT_DATE - 1 ELSE NULL END,
    seed_progress.current_day,
    seed_progress.practice_count,
    CASE WHEN seed_progress.practice_count > 0 THEN CURRENT_DATE ELSE NULL END,
    seed_progress.note,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'SYSTEM',
    'SYSTEM',
    2
FROM seed_progress
JOIN selected_profiles sp ON sp.profile_no = seed_progress.profile_no
JOIN ai_teaching_lesson lesson ON lesson.sort_order = seed_progress.lesson_sort_order
WHERE NOT EXISTS (
    SELECT 1
    FROM ai_lesson_progress existing
    WHERE existing.profile_id = sp.id
      AND existing.lesson_id = lesson.id
      AND existing.status = 2
);
