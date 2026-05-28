CREATE TABLE IF NOT EXISTS handbook_posts (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    snippet TEXT,
    content TEXT,
    image_url TEXT,
    author_name VARCHAR(120),
    author_role VARCHAR(120),
    published_at TIMESTAMP,
    status BIGINT
);

CREATE TABLE IF NOT EXISTS handbook_comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES handbook_posts(id),
    parent_id BIGINT REFERENCES handbook_comments(id),
    user_id BIGINT,
    user_name VARCHAR(120),
    avatar_url TEXT,
    content TEXT NOT NULL,
    like_count BIGINT,
    admin_reply BOOLEAN,
    created_at TIMESTAMP,
    status BIGINT
);

CREATE INDEX IF NOT EXISTS idx_handbook_posts_status_category
    ON handbook_posts(status, category);

CREATE INDEX IF NOT EXISTS idx_handbook_comments_post_status
    ON handbook_comments(post_id, status);

INSERT INTO handbook_posts (
    id, category, title, snippet, content, image_url, author_name, author_role, published_at, status
) VALUES
(
    1,
    'Dinh dưỡng',
    'Thực đơn ăn dặm BLW tuần đầu tiên cho bé 6 tháng',
    'Bắt đầu ăn dặm tự chỉ huy không khó như mẹ nghĩ. Dưới đây là gợi ý thực đơn 7 ngày giúp bé làm quen với thức ăn thô an toàn.',
    'Tuần đầu tiên của BLW nên ưu tiên thức ăn mềm, cắt dạng thanh vừa tay bé cầm và luôn có người lớn quan sát. Mẹ có thể bắt đầu với bơ, khoai lang hấp, bí đỏ hấp, chuối chín, cà rốt hấp mềm và bông cải xanh hấp. Không ép bé ăn nhiều, mục tiêu chính là làm quen mùi vị, kết cấu và kỹ năng đưa thức ăn lên miệng. Tránh nêm muối, đường, mật ong và các món dễ hóc như hạt nguyên, nho nguyên quả hoặc miếng cứng.',
    'https://images.unsplash.com/photo-1514050684125-9614f24ebcc6?q=80&w=900&auto=format&fit=crop',
    'Admin Hương Trà',
    'Chuyên gia Dinh dưỡng',
    CURRENT_TIMESTAMP - INTERVAL '3 days',
    2
),
(
    2,
    'Sức khỏe',
    'Phân biệt sốt mọc răng và sốt do virus ở trẻ',
    'Nhiều mẹ nhầm lẫn giữa việc con khó chịu do sưng nướu mọc răng và sốt do nhiễm virus. Bài viết này giúp mẹ nhận biết dấu hiệu cần theo dõi.',
    'Mọc răng có thể khiến bé chảy dãi, thích cắn đồ vật, nướu sưng và hơi quấy. Tuy nhiên sốt cao, li bì, bỏ bú nhiều, thở nhanh, phát ban lan rộng hoặc tiêu chảy kéo dài thường không nên quy hết cho mọc răng. Khi nhiệt độ từ 38.5 độ C trở lên hoặc bé có biểu hiện bất thường, mẹ nên liên hệ bác sĩ để được hướng dẫn phù hợp.',
    'https://images.unsplash.com/photo-1584515933487-779824d29309?q=80&w=900&auto=format&fit=crop',
    'Bác sĩ Minh Đức',
    'Cố vấn Y tế',
    CURRENT_TIMESTAMP - INTERVAL '5 days',
    2
),
(
    3,
    'Giấc ngủ',
    'Wonder Week và cách giúp bé ngủ ổn hơn',
    'Wonder week không phải là kẻ thù. Một vài điều chỉnh nhỏ trong nếp sinh hoạt có thể giúp bé vượt qua giai đoạn này nhẹ nhàng hơn.',
    'Trong các giai đoạn phát triển mạnh, bé có thể bám mẹ hơn, dễ tỉnh giấc và khó tự ngủ lại. Mẹ nên giữ lịch sinh hoạt ổn định, giảm kích thích trước giờ ngủ, dùng ánh sáng dịu và tiếng ồn trắng nếu bé hợp. Ban ngày, cho bé vận động vừa sức và quan sát dấu hiệu buồn ngủ để tránh quá mệt.',
    'https://images.unsplash.com/photo-1519689680058-324335c77eba?q=80&w=900&auto=format&fit=crop',
    'Admin BabyOi',
    'Staff',
    CURRENT_TIMESTAMP - INTERVAL '7 days',
    2
)
ON CONFLICT (id) DO UPDATE SET
    category = EXCLUDED.category,
    title = EXCLUDED.title,
    snippet = EXCLUDED.snippet,
    content = EXCLUDED.content,
    image_url = EXCLUDED.image_url,
    author_name = EXCLUDED.author_name,
    author_role = EXCLUDED.author_role,
    status = EXCLUDED.status;

SELECT setval(pg_get_serial_sequence('handbook_posts', 'id'), COALESCE((SELECT MAX(id) FROM handbook_posts), 1), true);

INSERT INTO handbook_comments (
    id, post_id, parent_id, user_id, user_name, avatar_url, content, like_count, admin_reply, created_at, status
) VALUES
(
    1,
    1,
    NULL,
    NULL,
    'Mẹ Mít',
    'https://cdn-icons-png.flaticon.com/512/4140/4140047.png',
    'Cảm ơn admin, bài viết rất hữu ích. Bé nhà mình 6 tháng bắt đầu ăn dặm BLW mà mẹ còn lóng ngóng quá.',
    12,
    FALSE,
    CURRENT_TIMESTAMP - INTERVAL '2 hours',
    2
),
(
    2,
    1,
    NULL,
    NULL,
    'Mẹ Ken',
    'https://cdn-icons-png.flaticon.com/512/4140/4140048.png',
    'Cho mình hỏi lúc bắt đầu bé có hay bị ọe không ạ?',
    5,
    FALSE,
    CURRENT_TIMESTAMP - INTERVAL '4 hours',
    2
),
(
    3,
    1,
    2,
    NULL,
    'Admin Hương Trà',
    'https://cdn-icons-png.flaticon.com/512/4712/4712027.png',
    'Mẹ Ken ơi, phản xạ ọe ở giai đoạn đầu khá thường gặp. Mẹ cắt thức ăn đúng kích thước, hấp mềm và luôn ngồi cạnh quan sát nhé.',
    24,
    TRUE,
    CURRENT_TIMESTAMP - INTERVAL '3 hours',
    2
)
ON CONFLICT (id) DO UPDATE SET
    post_id = EXCLUDED.post_id,
    parent_id = EXCLUDED.parent_id,
    user_name = EXCLUDED.user_name,
    avatar_url = EXCLUDED.avatar_url,
    content = EXCLUDED.content,
    like_count = EXCLUDED.like_count,
    admin_reply = EXCLUDED.admin_reply,
    status = EXCLUDED.status;

SELECT setval(pg_get_serial_sequence('handbook_comments', 'id'), COALESCE((SELECT MAX(id) FROM handbook_comments), 1), true);
